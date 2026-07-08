#!/bin/bash
# ============================================================
# MedSync — EC2 Bootstrap Script
# ============================================================
# One-command setup for a fresh Ubuntu 22.04/24.04 EC2 instance.
# Installs Docker, creates swap, configures firewall, and sets
# up systemd to auto-start the app on boot.
#
# Usage:
#   scp setup-ec2.sh ubuntu@<EC2_IP>:~
#   ssh ubuntu@<EC2_IP> 'chmod +x setup-ec2.sh && sudo ./setup-ec2.sh'
#
# This script is idempotent — safe to run multiple times.
# ============================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log()  { echo -e "${GREEN}[SETUP]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

APP_DIR="/opt/medsync"
SWAP_SIZE="1G"

# ============================================================
# Pre-flight checks
# ============================================================
if [ "$EUID" -ne 0 ]; then
    err "Please run as root: sudo ./setup-ec2.sh"
fi

log "Starting MedSync EC2 setup..."

# ============================================================
# 1. System Updates
# ============================================================
log "Updating system packages..."
apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get upgrade -y -qq

# ============================================================
# 2. Install Docker
# ============================================================
if command -v docker &> /dev/null; then
    log "Docker already installed: $(docker --version)"
else
    log "Installing Docker..."
    apt-get install -y -qq ca-certificates curl gnupg

    # Add Docker's official GPG key
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg

    # Add Docker repository
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null

    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Add ubuntu user to docker group
    usermod -aG docker ubuntu
    log "Docker installed: $(docker --version)"
fi

# ============================================================
# 3. Install Additional Tools
# ============================================================
log "Installing additional tools..."
apt-get install -y -qq \
    git \
    curl \
    htop \
    make \
    jq \
    unzip

# Install certbot for Let's Encrypt SSL
if ! command -v certbot &> /dev/null; then
    log "Installing Certbot..."
    apt-get install -y -qq certbot
fi

# Install AWS CLI (for potential manual operations)
if ! command -v aws &> /dev/null; then
    log "Installing AWS CLI..."
    curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
    unzip -qq /tmp/awscliv2.zip -d /tmp/
    /tmp/aws/install --update
    rm -rf /tmp/aws /tmp/awscliv2.zip
fi

# ============================================================
# 4. Create Swap (critical for t3.small with 2GB RAM)
# ============================================================
if swapon --show | grep -q '/swapfile'; then
    log "Swap already configured: $(swapon --show)"
else
    log "Creating ${SWAP_SIZE} swap file..."
    fallocate -l ${SWAP_SIZE} /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile

    # Make swap persistent across reboots
    if ! grep -q '/swapfile' /etc/fstab; then
        echo '/swapfile none swap sw 0 0' >> /etc/fstab
    fi

    # Tune swappiness (lower = prefer RAM over swap)
    sysctl vm.swappiness=10
    if ! grep -q 'vm.swappiness' /etc/sysctl.conf; then
        echo 'vm.swappiness=10' >> /etc/sysctl.conf
    fi

    log "Swap configured: $(free -h | grep Swap)"
fi

# ============================================================
# 5. Create App Directory
# ============================================================
if [ -d "$APP_DIR" ]; then
    log "App directory already exists: ${APP_DIR}"
else
    log "Creating app directory: ${APP_DIR}"
    mkdir -p ${APP_DIR}
    chown ubuntu:ubuntu ${APP_DIR}
fi

# Create SSL directory
mkdir -p ${APP_DIR}/deploy/ssl

# ============================================================
# 6. Generate Self-Signed SSL (fallback)
# ============================================================
if [ ! -f "${APP_DIR}/deploy/ssl/cert.pem" ]; then
    log "Generating self-signed SSL certificate..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout ${APP_DIR}/deploy/ssl/key.pem \
        -out ${APP_DIR}/deploy/ssl/cert.pem \
        -subj "/CN=localhost" 2>/dev/null
    chown ubuntu:ubuntu ${APP_DIR}/deploy/ssl/*.pem
    log "Self-signed SSL cert created (replace with Let's Encrypt for production)"
else
    log "SSL certificates already exist"
fi

# ============================================================
# 7. Configure Docker Logging
# ============================================================
log "Configuring Docker log rotation..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json << 'EOF'
{
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "10m",
        "max-file": "3"
    }
}
EOF
systemctl restart docker

# ============================================================
# 8. Configure Firewall (UFW)
# ============================================================
log "Configuring firewall..."
if ! ufw status | grep -q "Status: active"; then
    ufw default deny incoming
    ufw default allow outgoing
    ufw allow 22/tcp comment 'SSH'
    ufw allow 80/tcp comment 'HTTP'
    ufw allow 443/tcp comment 'HTTPS'
    ufw --force enable
    log "Firewall enabled (SSH, HTTP, HTTPS allowed)"
else
    log "Firewall already active"
fi

# ============================================================
# 9. Configure Systemd Service (auto-start on boot)
# ============================================================
log "Creating systemd service for MedSync..."
cat > /etc/systemd/system/medsync.service << EOF
[Unit]
Description=MedSync Patient Management System
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=${APP_DIR}
ExecStart=/usr/bin/docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
ExecStop=/usr/bin/docker compose -f docker-compose.yml -f docker-compose.prod.yml down
User=ubuntu
Group=ubuntu

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable medsync.service
log "Systemd service created and enabled (auto-starts on boot)"

# ============================================================
# 10. Summary
# ============================================================
echo ""
echo "============================================================"
echo "  MedSync EC2 Setup Complete!"
echo "============================================================"
echo ""
echo "  Docker:     $(docker --version)"
echo "  Compose:    $(docker compose version)"
echo "  Swap:       $(free -h | grep Swap | awk '{print $2}')"
echo "  Firewall:   $(ufw status | head -1)"
echo "  App Dir:    ${APP_DIR}"
echo "  SSL:        ${APP_DIR}/deploy/ssl/"
echo "  Systemd:    medsync.service (enabled)"
echo ""
echo "  Next steps:"
echo "    1. cd ${APP_DIR}"
echo "    2. git clone https://github.com/SAdreasgamer/MedSync.git ."
echo "       (or git pull if already cloned)"
echo "    3. Create .env file with your secrets"
echo "    4. make prod"
echo "    5. make health"
echo ""
echo "  For Let's Encrypt SSL (after setting up DNS):"
echo "    sudo certbot certonly --standalone -d your-domain.com"
echo "    sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem ${APP_DIR}/deploy/ssl/cert.pem"
echo "    sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem ${APP_DIR}/deploy/ssl/key.pem"
echo "    make prod-restart"
echo ""
echo "============================================================"
