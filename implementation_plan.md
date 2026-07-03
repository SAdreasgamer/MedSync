# Patient Management System — Complete Enhancement Plan

---

# Phase 1: Agentic AI — The Nervous System of the App

> [!IMPORTANT]
> The AI agent is NOT an AI doctor. It does NOT recommend treatments, assess risk, or give medical advice. It is a **conversational management interface** — a chatbot that can perform every operation in the system through natural language. Think of it as a Slack bot for your hospital's admin staff.

## What the Agent Can Do

| Category | Natural Language Input | What the Agent Does |
|---|---|---|
| **Query Patients** | "Show me all patients" | Calls `GET /patients` on patient-service |
| | "Find patient John Doe" | Calls `GET /patients/search?q=John Doe` |
| | "Get details for patient abc-123" | Calls `GET /patients/{id}` |
| **Create Patient** | "Register a new patient named Jane Smith, email jane@test.com, DOB 1990-05-15, address 123 Main St" | Calls `POST /patients` on patient-service |
| **Update Patient** | "Update patient abc-123's address to 456 Oak Ave" | Calls `PUT /patients/{id}` |
| **Delete Patient** | "Remove patient abc-123 from the system" | Calls `DELETE /patients/{id}` |
| **Billing** | "Set up a billing account for patient abc-123" | Calls `CreateBillingAccount` gRPC on billing-service |
| | "Check billing status for patient abc-123" | Calls `GetBillingAccount` gRPC on billing-service |
| **Analytics** | "How many patients registered this month?" | Calls analytics-service REST API |
| | "Show me patient registration trends" | Queries analytics aggregations |
| **Multi-step Workflows** | "Register patient John, set up his billing, and confirm everything is done" | Chains: create patient → create billing → verify both → report back |

## Agent Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                        USER                                   │
│  "Register a new patient named Rahul, email rahul@test.com,  │
│   DOB 1995-08-20, address Mumbai, and set up his billing"    │
└──────────────────────────┬────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                  Python AI Agent (LangGraph)                  │
│                                                              │
│  Step 1 — LLM reasons:                                       │
│    "I need to create the patient first, then set up billing" │
│                                                              │
│  Step 2 — Tool call:                                         │
│    create_patient(name="Rahul", email="rahul@test.com",      │
│                   dob="1995-08-20", address="Mumbai")        │
│    → REST POST to patient-service:4000/patients              │
│    → Result: {id: "xyz-789", name: "Rahul", ...}             │
│                                                              │
│  Step 3 — LLM reasons:                                       │
│    "Patient created with ID xyz-789. Now set up billing."    │
│                                                              │
│  Step 4 — Tool call:                                         │
│    create_billing_account(patient_id="xyz-789",              │
│                           name="Rahul",                      │
│                           email="rahul@test.com")            │
│    → gRPC call to billing-service:9001                       │
│    → Result: {account_id: "12345", status: "ACTIVE"}         │
│                                                              │
│  Step 5 — LLM responds:                                      │
│    "Done! Patient Rahul has been registered (ID: xyz-789)    │
│     and billing account #12345 is now ACTIVE."               │
└──────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Web Framework | **FastAPI** | Async, auto OpenAPI docs, WebSocket, Pydantic |
| Agent Framework | **LangGraph** | Stateful ReAct loop, tool calling, streaming |
| LLM | **Google Gemini free tier** | $0, 15 RPM, via `langchain-google-genai` |
| HTTP Client | **httpx** (async) | Calls Java REST services |
| gRPC Client | **grpcio** | Calls Java billing-service gRPC |
| Kafka | **confluent-kafka** | Listens to patient events |

## Service Structure

```
ai-agent-service/
├── Dockerfile
├── requirements.txt
├── app/
│   ├── main.py                       # FastAPI app + startup
│   ├── config.py                     # Env config (Pydantic Settings)
│   │
│   ├── agent/
│   │   ├── graph.py                  # LangGraph ReAct agent definition
│   │   ├── state.py                  # Agent state schema
│   │   └── prompts.py               # System prompt (what the agent CAN and CANNOT do)
│   │
│   ├── tools/
│   │   ├── patient_tools.py          # get, search, create, update, delete patients
│   │   ├── billing_tools.py          # create billing account, get billing status
│   │   └── analytics_tools.py        # query registration counts, trends
│   │
│   ├── api/
│   │   ├── routes.py                 # POST /agent/chat, GET /agent/history
│   │   └── websocket.py             # WebSocket /agent/stream for real-time
│   │
│   ├── models/
│   │   └── schemas.py               # Pydantic request/response models
│   │
│   ├── kafka/
│   │   └── consumer.py              # Listen to patient events (optional notifications)
│   │
│   └── grpc_client/
│       ├── billing_client.py         # Python gRPC stub for billing-service
│       └── generated/               # protoc-generated Python stubs
│           ├── billing_pb2.py
│           └── billing_pb2_grpc.py
│
├── proto/
│   └── billing.proto                 # Copied from billing-service for Python codegen
│
└── tests/
    ├── test_tools.py                 # Unit tests for each tool
    └── test_agent.py                 # Agent integration tests with mocked LLM
```

## System Prompt (What the Agent Is)

```text
You are MedSync Assistant, a patient management system operator.
You help hospital staff manage patients, billing, and view analytics.

You CAN:
- Search, create, update, and delete patient records
- Set up and check billing accounts
- Query analytics (patient counts, registration trends)
- Chain multiple operations together

You CANNOT:
- Give medical advice or treatment recommendations
- Diagnose conditions or assess patient risk
- Access or discuss medical records content
- Make clinical decisions of any kind

Always confirm destructive actions (delete) before executing them.
Always report back what was done with specific IDs and details.
```

## Changes Needed in Existing Java Services

#### [MODIFY] [PatientController.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/patient-service/src/main/java/com/pm/patientservice/controller/PatientController.java)

Add search endpoint (so the agent can find patients by name/email):
```java
@GetMapping("/search")
public ResponseEntity<List<PatientResponseDTO>> searchPatients(@RequestParam String q) { ... }
```

Add get-by-ID endpoint (currently missing):
```java
@GetMapping("/{id}")
public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id) { ... }
```

#### [MODIFY] [BillingGrpcService.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/billing-service/src/main/java/com/pm/billingservice/grpc/BillingGrpcService.java)

- Add actual PostgreSQL persistence (currently hardcoded)
- Add `GetBillingAccount` gRPC method so agent can query billing status
- Update `.proto` file with the new method

#### [MODIFY] [KafkaConsumer.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/analytics-service/src/main/java/com/pm/analyticsservice/kafka/KafkaConsumer.java)

- Persist events to PostgreSQL (currently just logs them)
- Add REST endpoints:
  - `GET /analytics/patient-count` — total count
  - `GET /analytics/registrations?period=month` — registration trends
  - `GET /analytics/recent-events` — last N events

#### [MODIFY] API Gateway [application.yml](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/api-gateway/src/main/resources/application.yml)

Add route for ai-agent-service:
```yaml
- id: ai-agent-route
  uri: http://ai-agent-service:4003
  predicates:
    - Path=/agent/**
  filters:
    - StripPrefix=1
    - JwtValidation
```

Add route for analytics-service:
```yaml
- id: analytics-route
  uri: http://analytics-service:4002
  predicates:
    - Path=/api/analytics/**
  filters:
    - StripPrefix=1
    - JwtValidation
```

---

# Phase 2: React Frontend (Light Professional Theme)

> [!IMPORTANT]
> **Light theme. No purple. No vibe-coded gradients.** Think of real healthcare SaaS — clean white backgrounds, clear typography, functional layout. Inspired by Notion, Linear (light mode), Stripe Dashboard.

## Color Palette

```
BACKGROUNDS                         ACCENT (Teal)
┌─────┐ ┌─────┐ ┌─────┐           ┌─────┐ ┌─────┐
│#FFFF│ │#F8FA│ │#F1F5│           │#0F76│ │#1099│
│FF   │ │FC   │ │F9   │           │6E   │ │8A   │
│     │ │     │ │     │           │     │ │     │
│page │ │card │ │hover│           │pri- │ │btn- │
│bg   │ │bg   │ │bg   │           │mary │ │hover│
└─────┘ └─────┘ └─────┘           └─────┘ └─────┘

TEXT                                BORDERS
┌─────┐ ┌─────┐ ┌─────┐           ┌─────┐
│#111│ │#475│ │#94A3│           │#E2E8│
│827 │ │569 │ │B8   │           │F0   │
│     │ │     │ │     │           │     │
│head-│ │body │ │muted│           │bor- │
│ings │ │text │ │text │           │ders │
└─────┘ └─────┘ └─────┘           └─────┘

STATUS
┌─────┐ ┌─────┐ ┌─────┐
│#0596│ │#D97│ │#DC26│
│6E   │ │706 │ │26   │
│     │ │     │ │     │
│ACTIVE│ │PEND│ │OVER │
│/good │ │ING │ │DUE  │
└─────┘ └─────┘ └─────┘
```

- **Page background**: Pure white `#FFFFFF`
- **Cards**: Very light gray `#F8FAFC` with `1px solid #E2E8F0` border
- **Primary accent**: Teal `#0F766E` — professional, not flashy
- **Text**: Near-black `#111827` headings, dark gray `#475569` body
- **Typography**: `Inter` via Google Fonts
- **No shadows** on cards — clean flat borders instead
- **No gradients** — solid colors only
- **No rounded-full pills** — subtle `border-radius: 8px`

## Pages

### Login Page
```
┌──────────────────────────────────────────────────┐
│                                                  │
│              ┌──────────────────┐                │
│              │                  │                │
│              │    🏥 MedSync    │                │
│              │                  │                │
│              │  Email           │                │
│              │  ┌────────────┐  │                │
│              │  │            │  │                │
│              │  └────────────┘  │                │
│              │                  │                │
│              │  Password        │                │
│              │  ┌────────────┐  │                │
│              │  │            │  │                │
│              │  └────────────┘  │                │
│              │                  │                │
│              │  [  Sign In   ]  │                │
│              │                  │                │
│              └──────────────────┘                │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Dashboard Page
```
┌────────────────────────────────────────────────────────────────┐
│ ┌──────────┐ MedSync             🔍 Search       [Dr. Nand ▾] │
│ │          │───────────────────────────────────────────────────│
│ │ Overview │                                                   │
│ │ Patients │  ┌───────────┐ ┌───────────┐ ┌───────────┐       │
│ │ Billing  │  │ 247       │ │ 18        │ │ 12        │       │
│ │ Analytic │  │ Total     │ │ This      │ │ Active    │       │
│ │ AI Asst  │  │ Patients  │ │ Month     │ │ Billing   │       │
│ │          │  └───────────┘ └───────────┘ └───────────┘       │
│ │          │                                                   │
│ │          │  Registration Trend              Recent Patients  │
│ │          │  ┌─────────────────────┐  ┌────────────────────┐ │
│ │          │  │ ▁▂▃▅▆▇█▇▆         │  │ Rahul S.  Today   │ │
│ │          │  │ Jan → Jun 2026     │  │ Jane D.   Ystrdy  │ │
│ │          │  │                     │  │ Mike T.   Jun 23  │ │
│ │          │  └─────────────────────┘  └────────────────────┘ │
│ └──────────┘                                                   │
└────────────────────────────────────────────────────────────────┘
```

### Patients Page
```
┌────────────────────────────────────────────────────────────────┐
│ Sidebar │  Patients                        [+ Add Patient]    │
│         │─────────────────────────────────────────────────────│
│         │  🔍 Search patients...                              │
│         │                                                     │
│         │  ┌────────────────────────────────────────────────┐ │
│         │  │ Name        │ Email          │ DOB      │ ⋯   │ │
│         │  │─────────────│────────────────│──────────│─────│ │
│         │  │ Rahul S.    │ rahul@test.com │ 1995-08  │ ⋮   │ │
│         │  │ Jane Doe    │ jane@test.com  │ 1990-05  │ ⋮   │ │
│         │  │ Mike Taylor │ mike@test.com  │ 1988-11  │ ⋮   │ │
│         │  └────────────────────────────────────────────────┘ │
│         │                                                     │
│         │  Showing 1-10 of 247              [< 1 2 3 ... >]  │
└────────────────────────────────────────────────────────────────┘
```

### AI Assistant Page (The Showpiece)
```
┌────────────────────────────────────────────────────────────────┐
│ Sidebar │  AI Assistant                                       │
│         │─────────────────────────────────────────────────────│
│         │                                                     │
│         │  ┌─ You ────────────────────────────────────────┐   │
│         │  │ Register a new patient Rahul, email           │   │
│         │  │ rahul@test.com, DOB 1995-08-20, address       │   │
│         │  │ Mumbai. Then set up his billing.               │   │
│         │  └───────────────────────────────────────────────┘   │
│         │                                                     │
│         │  ┌─ Assistant ──────────────────────────────────┐   │
│         │  │                                               │   │
│         │  │ ┌ 🔧 create_patient ────────────────────────┐ │   │
│         │  │ │ ✓ Created patient Rahul (ID: xyz-789)     │ │   │
│         │  │ └───────────────────────────────────────────┘ │   │
│         │  │                                               │   │
│         │  │ ┌ 🔧 create_billing_account ────────────────┐ │   │
│         │  │ │ ✓ Billing account #12345 — Status: ACTIVE │ │   │
│         │  │ └───────────────────────────────────────────┘ │   │
│         │  │                                               │   │
│         │  │ Done! Here's the summary:                     │   │
│         │  │ • Patient **Rahul** registered (ID: xyz-789)  │   │
│         │  │ • Billing account **#12345** is ACTIVE         │   │
│         │  └───────────────────────────────────────────────┘   │
│         │                                                     │
│         │  ┌────────────────────────────────────┐ ┌────────┐ │
│         │  │ Type a message...                  │ │  Send  │ │
│         │  └────────────────────────────────────┘ └────────┘ │
└────────────────────────────────────────────────────────────────┘
```

### Patient Detail Page
```
┌────────────────────────────────────────────────────────────────┐
│ ← Patients                                    [Edit] [Delete] │
│                                                                │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ Rahul Sharma                                              │ │
│  │ rahul@test.com  •  DOB: 1995-08-20  •  Mumbai            │ │
│  │ Registered: 2026-06-25                                    │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                │
│  Billing                                                       │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ Account ID: #12345     Status: ● ACTIVE                   │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                │
│  [🤖 Ask AI about this patient]                               │
└────────────────────────────────────────────────────────────────┘
```

## Frontend Structure

```
frontend/
├── package.json
├── vite.config.js
├── index.html
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── index.css                     # Design tokens + global reset
│   │
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Sidebar.jsx + .css    # Left nav
│   │   │   ├── Header.jsx + .css     # Top bar
│   │   │   └── Layout.jsx + .css     # Page wrapper
│   │   │
│   │   ├── ui/
│   │   │   ├── Button.jsx + .css
│   │   │   ├── Card.jsx + .css
│   │   │   ├── Table.jsx + .css
│   │   │   ├── Input.jsx + .css
│   │   │   ├── Modal.jsx + .css
│   │   │   ├── Badge.jsx + .css
│   │   │   ├── Skeleton.jsx + .css
│   │   │   └── Toast.jsx + .css
│   │   │
│   │   ├── patients/
│   │   │   ├── PatientTable.jsx
│   │   │   ├── PatientForm.jsx       # Create/edit form in modal
│   │   │   └── PatientDetail.jsx
│   │   │
│   │   ├── agent/
│   │   │   ├── AgentChat.jsx         # Chat container
│   │   │   ├── AgentMessage.jsx      # Single message bubble
│   │   │   └── ToolCallCard.jsx      # Shows tool name + result
│   │   │
│   │   ├── billing/
│   │   │   └── BillingTable.jsx
│   │   │
│   │   └── analytics/
│   │       ├── StatCard.jsx
│   │       └── TrendChart.jsx
│   │
│   ├── pages/
│   │   ├── LoginPage.jsx
│   │   ├── DashboardPage.jsx
│   │   ├── PatientsPage.jsx
│   │   ├── PatientDetailPage.jsx
│   │   ├── AgentPage.jsx
│   │   ├── BillingPage.jsx
│   │   └── AnalyticsPage.jsx
│   │
│   ├── services/
│   │   ├── api.js                    # Axios + JWT interceptor
│   │   ├── authService.js
│   │   ├── patientService.js
│   │   ├── agentService.js           # WebSocket + REST
│   │   ├── billingService.js
│   │   └── analyticsService.js
│   │
│   ├── context/
│   │   └── AuthContext.jsx           # JWT auth state
│   │
│   └── hooks/
│       ├── useAuth.js
│       └── useWebSocket.js           # Agent streaming hook
│
└── deploy/
    └── deploy-s3.sh                  # Build → S3 sync → CF invalidate
```

## Frontend Tech Stack

| Layer | Choice |
|---|---|
| Framework | React 18 + Vite |
| Routing | React Router v6 |
| HTTP | Axios with JWT interceptor |
| WebSocket | Native browser WebSocket API |
| Charts | Recharts |
| Styling | Vanilla CSS with CSS custom properties |
| Icons | Lucide React |
| Font | Inter (Google Fonts) |

---

# Phase 3: Deployment

## Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│   S3 + CloudFront ($0)              EC2 t3.small ($0-8/mo)    │
│  ┌──────────────────┐           ┌──────────────────────────┐  │
│  │  React SPA       │           │  docker-compose          │  │
│  │  (static files)  │──HTTPS──▶ │                          │  │
│  │                  │           │  ┌────────────────────┐   │  │
│  │  CloudFront CDN  │           │  │ nginx (:80/:443)   │   │  │
│  │  ACM SSL (free)  │           │  └────────┬───────────┘   │  │
│  └──────────────────┘           │           │               │  │
│                                 │           ▼               │  │
│                                 │  ┌────────────────────┐   │  │
│                                 │  │ api-gateway (:4004)│   │  │
│                                 │  └────────┬───────────┘   │  │
│                                 │           │               │  │
│                                 │     ┌─────┼─────┬────┐    │  │
│                                 │     ▼     ▼     ▼    ▼    │  │
│                                 │  ┌─────┐┌────┐┌───┐┌───┐ │  │
│                                 │  │pati-││bill││ana││ai-│ │  │
│                                 │  │ent  ││ing ││ly-││age│ │  │
│                                 │  │:4000││:4001││tics││nt │ │  │
│                                 │  └─────┘└────┘│:4002││:4003│ │
│                                 │               └───┘└───┘ │  │
│                                 │  ┌─────┐                  │  │
│                                 │  │auth  │                  │  │
│                                 │  │:4005 │                  │  │
│                                 │  └─────┘                  │  │
│                                 │                           │  │
│                                 │  ┌──────────┐ ┌────────┐  │  │
│                                 │  │PostgreSQL│ │ Kafka  │  │  │
│                                 │  │+ pgvector│ │ KRaft  │  │  │
│                                 │  │ :5432    │ │ :9092  │  │  │
│                                 │  └──────────┘ └────────┘  │  │
│                                 └──────────────────────────┘  │
│                                                                │
│   GitHub Actions CI/CD (FREE)                                  │
│   - Frontend: npm build → S3 sync                              │
│   - Backend: SSH → git pull → docker-compose up --build        │
└────────────────────────────────────────────────────────────────┘
```

## New Deployment Files

| File | Purpose |
|---|---|
| [NEW] `docker-compose.yml` | All backend services + infra in one file |
| [NEW] `docker-compose.dev.yml` | Local dev overrides (debug ports, no nginx) |
| [NEW] `deploy/init-db.sql` | Creates all databases in single PostgreSQL instance |
| [NEW] `deploy/nginx.conf` | Reverse proxy + CORS + SSL + rate limiting |
| [NEW] `deploy/setup-ec2.sh` | One-command EC2 bootstrap (install Docker, swap, systemd) |
| [NEW] `.github/workflows/deploy-backend.yml` | SSH → pull → rebuild → restart |
| [NEW] `.github/workflows/deploy-frontend.yml` | Build → S3 sync → CloudFront invalidation |

## docker-compose.yml Services

```yaml
services:
  # --- Infrastructure ---
  postgres:          # Single instance, init-db.sql creates 5 databases
  kafka:             # Bitnami Kafka in KRaft mode (no ZooKeeper)
  nginx:             # Reverse proxy, SSL, CORS

  # --- Java Services ---
  auth-service:      # depends: postgres
  patient-service:   # depends: postgres, kafka, billing-service
  billing-service:   # depends: postgres
  analytics-service: # depends: postgres, kafka

  # --- Python Service ---
  ai-agent-service:  # depends: kafka

  # --- Gateway ---
  api-gateway:       # depends: all services
```

## Memory Budget (t3.small = 2GB RAM)

| Container | Limit | JVM / Config |
|---|---|---|
| PostgreSQL | 256MB | `shared_buffers=64MB` |
| Kafka (KRaft) | 256MB | `KAFKA_HEAP_OPTS=-Xmx200m` |
| auth-service | 160MB | `-Xmx100m -XX:+UseSerialGC` |
| patient-service | 180MB | `-Xmx120m -XX:+UseSerialGC` |
| billing-service | 160MB | `-Xmx100m -XX:+UseSerialGC` |
| analytics-service | 160MB | `-Xmx100m -XX:+UseSerialGC` |
| api-gateway | 160MB | `-Xmx100m -XX:+UseSerialGC` |
| ai-agent-service (Python) | 180MB | Lightweight — no JVM |
| nginx | 32MB | Native |
| **TOTAL** | **~1.55GB** | **Fits in 2GB + 1GB swap** |

## Cost Summary

| Component | Year 1 | After Year 1 |
|---|---|---|
| EC2 t3.small | **$0** (free tier t3.micro) or ~$8 spot | ~$8/mo spot |
| EBS 20GB | **$0** (30GB free) | ~$1.60/mo |
| S3 | **~$0.01** | ~$0.01 |
| CloudFront | **$0** (1TB free) | $0 |
| SSL | **$0** (ACM + Let's Encrypt) | $0 |
| Gemini API | **$0** (free tier) | $0 |
| GitHub Actions | **$0** (public repo) | $0 |
| **TOTAL** | **~$0/mo** | **~$10/mo** |

---

# Implementation Order

| Step | What | Phase |
|---|---|---|
| 1 | Create `docker-compose.yml` — get all existing Java services + Kafka + Postgres running together | Phase 3 |
| 2 | Add search + get-by-id endpoints to patient-service | Phase 1 |
| 3 | Add persistence + GetBillingAccount to billing-service | Phase 1 |
| 4 | Add persistence + analytics REST endpoints to analytics-service | Phase 1 |
| 5 | Add ai-agent-service route to API gateway | Phase 1 |
| 6 | Build Python `ai-agent-service` — tools, agent graph, FastAPI endpoints, WebSocket | Phase 1 |
| 7 | Test agent end-to-end via docker-compose | Phase 1 |
| 8 | Build React frontend — design system, layout, all pages | Phase 2 |
| 9 | Connect frontend to all APIs + agent WebSocket | Phase 2 |
| 10 | Create `deploy/setup-ec2.sh` + `nginx.conf` | Phase 3 |
| 11 | Create `deploy-s3.sh` + GitHub Actions workflows | Phase 3 |
| 12 | Deploy backend to EC2, frontend to S3 | Phase 3 |
| 13 | Polish — README, ARCHITECTURE.md, final testing | All |

---

# Verification Plan

### Phase 1 (Agent)
- `docker-compose up` → all containers healthy
- `curl POST /agent/chat` with "show me all patients" → returns patient list
- Multi-step: "create patient X and set up billing" → both operations succeed
- WebSocket streaming works from `/agent/stream`
- Agent refuses medical advice prompts

### Phase 2 (Frontend)
- `npm run build` succeeds with zero errors
- Login flow works with JWT
- All pages render with correct data
- Agent chat shows tool-call cards during streaming
- Responsive layout works on tablet/mobile

### Phase 3 (Deployment)
- `setup-ec2.sh` runs clean on fresh EC2
- `docker ps` shows all 10 containers healthy
- S3 URL serves React app
- React app talks to EC2 backend via HTTPS
- GitHub Actions deploys on push to main
