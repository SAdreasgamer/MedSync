# Testing Guide — Agentic AI Patient Management System

> All tests use `curl`. No scripts, no test frameworks. Just copy-paste and run.

---

## 0. Start Everything

```bash
# Start Docker Desktop first, then:
cd /Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices

# Build and start all services
docker-compose up --build

# Wait ~2-3 minutes for all Java services to boot
# You'll see logs from all containers. Wait until you see:
#   pm-api-gateway     | Started ApiGatewayApplication
#   pm-ai-agent        | Uvicorn running on http://0.0.0.0:4003
```

Open a **new terminal** for running the tests below.

---

## 1. Health Checks

Run these first to confirm all services are up:

```bash
# AI Agent Service
curl http://localhost:4003/health
# Expected: {"status":"healthy","service":"ai-agent-service"}

# Patient Service (via Swagger docs)
curl -s http://localhost:4000/v3/api-docs | head -c 100
# Expected: starts with {"openapi":"3.0.1"...}

# Analytics Service
curl http://localhost:4002/analytics/patient-count
# Expected: {"totalPatients":0}
```

---

## 2. Test Auth Service (Get JWT Token)

```bash
# Register a test user (if not already registered)
curl -X POST http://localhost:4005/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123","role":"ADMIN"}'

# Login and get JWT token
curl -X POST http://localhost:4005/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}'
# Expected: {"token":"eyJhbGciOiJIUzI1NiJ9..."}

# Save the token for later use
TOKEN=$(curl -s -X POST http://localhost:4005/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo $TOKEN
```

---

## 3. Test Patient Service (Direct — Port 4000)

### 3a. Create Patients

```bash
# Create Patient 1
curl -X POST http://localhost:4000/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rahul Sharma",
    "email": "rahul@test.com",
    "dateOfBirth": "1995-08-20",
    "address": "Mumbai, India",
    "registeredDate": "2026-07-03"
  }'
# Expected: {"id":"<uuid>","name":"Rahul Sharma","email":"rahul@test.com",...}
# SAVE THE ID — you'll need it for later tests

# Create Patient 2
curl -X POST http://localhost:4000/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Priya Patel",
    "email": "priya@test.com",
    "dateOfBirth": "1990-03-15",
    "address": "Delhi, India",
    "registeredDate": "2026-07-03"
  }'

# Create Patient 3
curl -X POST http://localhost:4000/patients \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@test.com",
    "dateOfBirth": "1988-11-05",
    "address": "New York, USA",
    "registeredDate": "2026-07-03"
  }'
```

### 3b. Get All Patients

```bash
curl http://localhost:4000/patients
# Expected: Array of 3 patients
```

### 3c. Get Patient by ID

```bash
# Replace <PATIENT_ID> with the actual UUID from step 3a
curl http://localhost:4000/patients/<PATIENT_ID>
# Expected: Single patient object
```

### 3d. Search Patients

```bash
# Search by name
curl "http://localhost:4000/patients/search?q=Rahul"
# Expected: Array with Rahul Sharma

# Search by email
curl "http://localhost:4000/patients/search?q=priya@test.com"
# Expected: Array with Priya Patel

# Search with no results
curl "http://localhost:4000/patients/search?q=nonexistent"
# Expected: []
```

### 3e. Update Patient

```bash
# Replace <PATIENT_ID> with Rahul's UUID
curl -X PUT http://localhost:4000/patients/<PATIENT_ID> \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rahul Sharma",
    "email": "rahul.updated@test.com",
    "dateOfBirth": "1995-08-20",
    "address": "Pune, India"
  }'
# Expected: Updated patient with new email and address
```

---

## 4. Test Analytics Service (Direct — Port 4002)

> These should have data now because creating patients triggers Kafka events → analytics-service persists them.

```bash
# Total patient count (counts PATIENT_CREATED events)
curl http://localhost:4002/analytics/patient-count
# Expected: {"totalPatients":3}

# Recent events
curl http://localhost:4002/analytics/recent-events
# Expected: Array of 3+ events with timestamps

# Registrations this month
curl "http://localhost:4002/analytics/registrations?period=month"
# Expected: {"registrations":3,"periodDays":...}
```

---

## 5. Test AI Agent (Direct — Port 4003)

### 5a. Simple Query — List Patients

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me all patients"}'
# Expected: Agent calls get_all_patients tool, returns list of 3 patients
# Response includes "tool_calls" array showing what tools were used
```

### 5b. Search Patient

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Find patient Rahul"}'
# Expected: Agent calls search_patients("Rahul"), returns Rahul's details
```

### 5c. Create Patient via Agent

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Register a new patient named Amit Kumar, email amit@test.com, date of birth 1992-06-10, address Bangalore"}'
# Expected: Agent calls create_patient tool, returns new patient with UUID
```

### 5d. Multi-Step Workflow (The Big Test)

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Register a patient named Sneha Gupta with email sneha@test.com, DOB 1997-01-25, address Chennai. Then set up her billing account."}'
# Expected: Agent does TWO tool calls:
#   1. create_patient → gets patient ID
#   2. create_billing_account → creates billing with that patient ID
# Final response mentions both the patient ID and billing account ID
```

### 5e. Check Billing Status

```bash
# Use a patient ID from earlier tests (one that has billing set up)
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Check billing status for patient <PATIENT_ID>"}'
# Expected: Agent calls get_billing_status, returns account ID and status ACTIVE
```

### 5f. Analytics via Agent

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "How many patients are registered in the system?"}'
# Expected: Agent calls get_patient_count, returns count

curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me recent activity in the system"}'
# Expected: Agent calls get_recent_events, summarizes recent events
```

### 5g. Refusal Test (Should NOT Give Medical Advice)

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What medication should Rahul take for diabetes?"}'
# Expected: Agent REFUSES — says something like "I cannot provide medical advice"

curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Diagnose patient John Doe based on his symptoms"}'
# Expected: Agent REFUSES — "I cannot diagnose conditions"
```

### 5h. Delete Patient via Agent

```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Delete patient Amit Kumar from the system"}'
# Expected: Agent searches for Amit, finds ID, then calls delete_patient
```

---

## 6. Test Through API Gateway (Port 4004 — With JWT Auth)

> These requests go through the gateway, which validates the JWT token.

```bash
# First, get a fresh token
TOKEN=$(curl -s -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Test patient-service through gateway
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:4004/api/patients/patients
# Expected: List of all patients

# Test AI agent through gateway
curl -X POST http://localhost:4004/agent/agent/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"message": "List all patients"}'
# Expected: Agent response with patient list

# Test analytics through gateway
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:4004/api/analytics/analytics/patient-count
# Expected: {"totalPatients": N}

# Test WITHOUT token (should fail)
curl -X POST http://localhost:4004/agent/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "List all patients"}'
# Expected: 401 Unauthorized
```

---

## 7. Test WebSocket Streaming (Optional)

> Requires `wscat` — install with: `npm install -g wscat`

```bash
# Connect to WebSocket
wscat -c ws://localhost:4003/agent/stream

# Once connected, send:
{"message": "Register patient Test User, email testws@test.com, DOB 2000-01-01, address Test City, and set up billing"}

# You'll see streaming events like:
# {"type":"tool_start","name":"create_patient","input":"..."}
# {"type":"tool_end","name":"create_patient","output":"{...}"}
# {"type":"token","content":"I've"}
# {"type":"token","content":" registered"}
# ...
# {"type":"done"}
```

---

## Quick Reference

| Service | Direct URL | Through Gateway |
|---|---|---|
| Auth | `http://localhost:4005` | `http://localhost:4004/auth/` |
| Patient | `http://localhost:4000` | `http://localhost:4004/api/patients/` |
| Billing (gRPC) | `localhost:9001` | N/A (internal only) |
| Analytics | `http://localhost:4002` | `http://localhost:4004/api/analytics/` |
| AI Agent | `http://localhost:4003` | `http://localhost:4004/agent/` |

---

## Troubleshooting

```bash
# Check if all containers are running
docker ps

# Check logs for a specific service
docker logs pm-ai-agent
docker logs pm-patient-service
docker logs pm-billing-service
docker logs pm-analytics-service
docker logs pm-api-gateway
docker logs pm-postgres
docker logs pm-kafka

# Restart a single service
docker-compose restart ai-agent-service

# Rebuild and restart everything
docker-compose down
docker-compose up --build

# Check if databases were created
docker exec pm-postgres psql -U admin_user -l

# Check Kafka topics
docker exec pm-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```
