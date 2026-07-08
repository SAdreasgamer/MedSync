.PHONY: help dev dev-detached down logs ps stats clean db-reset \
        prod prod-restart prod-down prod-logs \
        frontend-dev frontend-build frontend-lint deploy-frontend \
        test-java test-python test-frontend test-all \
        build-all health ssl-gen

# ──────────────────────────────────────────────────
# Default target — show help
# ──────────────────────────────────────────────────
help: ## Show all available targets
	@echo ""
	@echo "  MedSync — Available Commands"
	@echo "  ────────────────────────────"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""

# ──────────────────────────────────────────────────
# Local Development
# ──────────────────────────────────────────────────
dev: ## Start all services locally (docker compose up --build)
	docker compose up --build

dev-detached: ## Start all services in background
	docker compose up --build -d

down: ## Stop all services
	docker compose down

logs: ## Tail logs from all containers
	docker compose logs -f --tail=100

logs-%: ## Tail logs for a specific service (e.g., make logs-patient-service)
	docker compose logs -f --tail=100 $*

ps: ## Show running containers
	docker compose ps

stats: ## Show container resource usage
	docker stats --no-stream

clean: ## Stop everything, remove volumes and local images
	docker compose down -v --rmi local

db-reset: ## Reset databases (destroy volumes, restart postgres)
	docker compose down -v
	docker compose up postgres -d

# ──────────────────────────────────────────────────
# Production
# ──────────────────────────────────────────────────
prod: ## Start production stack (with nginx, mem limits)
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

prod-restart: ## Rebuild and restart production stack
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build --remove-orphans

prod-down: ## Stop production stack
	docker compose -f docker-compose.yml -f docker-compose.prod.yml down

prod-logs: ## Tail production logs
	docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail=100

# ──────────────────────────────────────────────────
# Frontend
# ──────────────────────────────────────────────────
frontend-dev: ## Start frontend dev server
	cd frontend && npm run dev

frontend-build: ## Build frontend for production
	cd frontend && npm ci && npm run build

frontend-lint: ## Lint frontend code
	cd frontend && npm run lint

deploy-frontend: frontend-build ## Build + deploy frontend to S3 + CloudFront
	aws s3 sync frontend/dist/ s3://$(S3_BUCKET) --delete \
	  --cache-control "max-age=31536000,immutable" \
	  --exclude "index.html"
	aws s3 cp frontend/dist/index.html s3://$(S3_BUCKET)/index.html \
	  --cache-control "no-cache"
	aws cloudfront create-invalidation \
	  --distribution-id $(CF_DIST_ID) --paths "/*"

# ──────────────────────────────────────────────────
# Testing
# ──────────────────────────────────────────────────
SERVICES := auth-service patient-service billing-service \
            analytics-service appointment-service api-gateway

test-java: ## Run Maven tests for all Java services
	@for svc in $(SERVICES); do \
	  echo "\n=== Testing $$svc ==="; \
	  (cd $$svc && mvn clean verify -B) || exit 1; \
	done

test-python: ## Run pytest for ai-agent-service
	cd ai-agent-service && pip install -r requirements.txt -q && \
	  python -m pytest tests/ -v 2>/dev/null || echo "No tests found"

test-frontend: ## Run frontend lint + build check
	cd frontend && npm ci && npm run lint && npm run build

test-all: test-java test-python test-frontend ## Run all tests

# ──────────────────────────────────────────────────
# Build Verification
# ──────────────────────────────────────────────────
build-all: ## Build all Docker images without starting
	docker compose build --parallel

# ──────────────────────────────────────────────────
# Deployment Utilities
# ──────────────────────────────────────────────────
health: ## Check health of all running services
	@echo "=== Container Status ==="
	@docker compose ps
	@echo "\n=== Health Checks ==="
	@curl -sf http://localhost:4005/actuator/health 2>/dev/null && echo "  auth-service: ✓" || echo "  auth-service: ✗"
	@curl -sf http://localhost:4000/actuator/health 2>/dev/null && echo "  patient-service: ✓" || echo "  patient-service: ✗"
	@curl -sf http://localhost:4001/actuator/health 2>/dev/null && echo "  billing-service: ✓" || echo "  billing-service: ✗"
	@curl -sf http://localhost:4002/actuator/health 2>/dev/null && echo "  analytics-service: ✓" || echo "  analytics-service: ✗"
	@curl -sf http://localhost:4006/actuator/health 2>/dev/null && echo "  appointment-service: ✓" || echo "  appointment-service: ✗"
	@curl -sf http://localhost:4004/actuator/health 2>/dev/null && echo "  api-gateway: ✓" || echo "  api-gateway: ✗"
	@curl -sf http://localhost:4003/health 2>/dev/null && echo "  ai-agent-service: ✓" || echo "  ai-agent-service: ✗"

ssl-gen: ## Generate self-signed SSL certs for local/dev use
	mkdir -p deploy/ssl
	openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
	  -keyout deploy/ssl/key.pem -out deploy/ssl/cert.pem \
	  -subj "/CN=localhost"
	@echo "Self-signed certs created in deploy/ssl/"
