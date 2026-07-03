<div align="center">

# 🏥 MedSync — Distributed Patient Management Platform

**A production-grade polyglot microservices system with AI-powered agentic orchestration**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.12-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://python.org)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-3.8-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![LangGraph](https://img.shields.io/badge/LangGraph-ReAct_Agent-1C3C3C?style=for-the-badge&logo=langchain&logoColor=white)](https://langchain-ai.github.io/langgraph/)

---

*MedSync is a full-stack healthcare management platform built using a distributed microservices architecture. It features real-time AI-powered natural language operations, event-driven analytics pipelines, gRPC inter-service communication, and a modern React dashboard — all orchestrated with Docker Compose.*

[Features](#-features) · [Architecture](#-architecture) · [Tech Stack](#-tech-stack) · [Quick Start](#-quick-start) · [API Documentation](#-api-documentation) · [Project Structure](#-project-structure) · [Screenshots](#-screenshots)

</div>

---

## ✨ Features

### 🩺 Clinical Operations
- **Patient Lifecycle Management** — Register, admit, discharge, and track patients through their entire hospital journey
- **Appointment Scheduling** — Schedule, reschedule, and manage doctor appointments with date/time filtering
- **Billing & Invoicing** — Create invoices, process payments, and track outstanding balances per patient

### 🤖 AI-Powered Assistant (Agentic AI)
- **Natural Language Commands** — Talk to the system in plain English: *"Admit patient Ananya Patel to ICU room 101"*
- **LangGraph ReAct Agent** — Multi-step reasoning engine that dynamically decides which microservice tools to call
- **20+ Tool Functions** — The agent can autonomously create patients, schedule appointments, process billing, and more
- **Real-time Streaming** — WebSocket-based token streaming for a live chat experience with graceful REST fallback

### 📊 Analytics & Monitoring
- **Event-Driven Pipeline** — Patient events are published to Apache Kafka and consumed by the analytics service asynchronously
- **Dashboard Metrics** — Live counts of total patients, admissions, outstanding billing, and today's appointments
- **Recently Accessed Tracking** — Automatically tracks and displays your 5 most recently viewed patient records

### 🔐 Security & Gateway
- **JWT Authentication** — Stateless token-based auth with Spring Security
- **API Gateway** — Centralized routing, CORS handling, and JWT validation via Spring Cloud Gateway
- **Protected Routes** — Both backend endpoints and frontend routes are guarded by authentication

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React + Vite)                       │
│                         http://localhost:5173                         │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │  HTTP / WebSocket
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Spring Cloud Gateway)                 │
│                         http://localhost:4004                         │
│              JWT Validation · Route Forwarding · CORS                │
└───────┬──────────┬──────────┬──────────┬──────────┬─────────────────┘
        │          │          │          │          │
        ▼          ▼          ▼          ▼          ▼
  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
  │   Auth   │ │ Patient  │ │ Billing  │ │Appoint-  │ │  AI Agent    │
  │ Service  │ │ Service  │ │ Service  │ │  ment    │ │  Service     │
  │ (Java)   │ │ (Java)   │ │ (Java)   │ │ Service  │ │  (Python)    │
  │ :4005    │ │ :4000    │ │ :4001    │ │ (Java)   │ │  :4003       │
  └──────────┘ └────┬─────┘ └──┬───────┘ │ :4006    │ └──────┬───────┘
                    │          │ gRPC     └──────────┘        │
                    │          │ :9001                        │
                    │          └──────────────────────────────┘
                    │                        │
                    ▼                        │  HTTP + gRPC
              ┌──────────┐                   │  (Tool Calls)
              │  Kafka   │                   │
              │  :9092   │                   │
              └────┬─────┘            ┌──────┴───────┐
                   │                  │  LangGraph   │
                   ▼                  │  ReAct Agent │
             ┌──────────┐            │  (20+ Tools) │
             │Analytics │            └──────────────┘
             │ Service  │
             │ (Java)   │
             │ :4002    │
             └──────────┘

              ┌──────────────────────────────┐
              │     PostgreSQL 17 :5432       │
              │  auth_db · patient_db         │
              │  billing_db · analytics_db    │
              │  appointment_service_db       │
              └──────────────────────────────┘
```

### Communication Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **REST (JSON)** | Frontend ↔ Gateway ↔ Services | Standard CRUD operations |
| **gRPC (Protobuf)** | Patient Service ↔ Billing Service, AI Agent ↔ Billing Service | Low-latency, type-safe internal RPCs |
| **Apache Kafka** | Patient Service → Analytics Service | Async event-driven data pipeline with Protobuf serialization |
| **WebSocket** | Frontend ↔ AI Agent Service | Real-time streaming of LLM reasoning tokens |

---

## 🛠 Tech Stack

### Backend
| Technology | Usage |
|---|---|
| **Java 17 + Spring Boot 3** | Auth, Patient, Billing, Appointment, Analytics, and API Gateway services |
| **Spring Cloud Gateway** | Centralized API routing with JWT filter |
| **Spring Security + JWT** | Stateless authentication and authorization |
| **Spring Data JPA + Hibernate** | ORM and database access layer |
| **Apache Kafka** | Event streaming broker for async microservice communication |
| **gRPC + Protobuf** | High-performance RPC between Patient/Agent and Billing services |
| **PostgreSQL 17** | Relational database (5 isolated databases, 1 shared instance) |

### AI Agent
| Technology | Usage |
|---|---|
| **Python 3.12 + FastAPI** | High-performance async API server |
| **LangGraph** | StateGraph-based ReAct agent with conditional tool routing |
| **LangChain Core** | Tool abstraction and prompt management |
| **OpenRouter / Gemini** | LLM provider (configurable, supports free-tier models) |
| **WebSockets** | Real-time token streaming to frontend |
| **gRPC Client** | Direct Protobuf communication with Billing Service |

### Frontend
| Technology | Usage |
|---|---|
| **React 19** | Component-based UI framework |
| **Vite 8** | Lightning-fast build tool and dev server |
| **React Router v7** | Client-side routing with protected routes |
| **Recharts** | Interactive dashboard charts and analytics |
| **Lucide React** | Modern icon system |
| **Axios** | HTTP client with JWT interceptor |

### DevOps
| Technology | Usage |
|---|---|
| **Docker + Docker Compose** | Containerized orchestration of 8+ services |
| **Multi-stage Dockerfiles** | Optimized container images for Java and Python services |

---

## 🚀 Quick Start

### Prerequisites

- **Docker Desktop** (v4.0+) with Docker Compose v2
- **Node.js** (v18+) and npm — for frontend development
- An LLM API key (one of the following):
  - [OpenRouter API Key](https://openrouter.ai/) (recommended — has free models)
  - [Google Gemini API Key](https://aistudio.google.com/)

### 1. Clone the Repository

```bash
git clone https://github.com/SAdreasgamer/MedSync.git
cd MedSync
```

### 2. Configure Environment Variables

Create a `.env` file in the project root:

```env
# Choose ONE of the following LLM providers:

# Option A: OpenRouter (Recommended — supports free models)
OPENROUTER_API_KEY=sk-or-v1-your-key-here
OPENROUTER_MODEL=openrouter/free

# Option B: Google Gemini
GEMINI_API_KEY=your-gemini-api-key-here
```

### 3. Launch All Backend Services

```bash
docker compose up --build -d
```

This starts **8 containers**: PostgreSQL, Kafka, Auth, Patient, Billing, Appointment, Analytics, AI Agent, and API Gateway.

Wait for all services to become healthy (~60-90 seconds on first run):

```bash
docker compose ps   # All containers should show "running"
```

### 4. Launch the Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Open the Application

| Interface | URL |
|---|---|
| 🖥️ **Frontend Dashboard** | [http://localhost:5173](http://localhost:5173) |
| 🔌 **API Gateway** | [http://localhost:4004](http://localhost:4004) |
| 📖 **Swagger API Docs** | [http://localhost:5173/swagger.html](http://localhost:5173/swagger.html) |

### Default Login Credentials

```
Username: admin
Password: password
```

---

## 📖 API Documentation

MedSync ships with a built-in **Swagger UI** accessible at `/swagger.html` from the frontend dev server. It documents all REST endpoints across every microservice.

### Service Endpoints

| Service | Port | Base Path | Description |
|---|---|---|---|
| **Auth Service** | 4005 | `/auth` | User registration and JWT login |
| **Patient Service** | 4000 | `/api/patients` | Full CRUD + admissions/discharges |
| **Billing Service** | 4001 / 9001 (gRPC) | `/api/patients/{id}/billing` | Invoicing, payments, and balances |
| **Appointment Service** | 4006 | `/api/appointments` | Scheduling and status management |
| **Analytics Service** | 4002 | `/api/analytics` | Kafka-consumed event analytics |
| **AI Agent Service** | 4003 | `/agent/chat` | Natural language chat (REST + WebSocket) |
| **API Gateway** | 4004 | `/*` | Unified entry point with JWT validation |

### Example API Calls

**Register a new user:**
```bash
curl -X POST http://localhost:4004/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "doctor1", "password": "secure123", "email": "doc@medsync.local"}'
```

**Login and get JWT:**
```bash
curl -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'
```

**Create a patient (with JWT):**
```bash
curl -X POST http://localhost:4004/api/patients \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ananya Patel",
    "email": "ananya@example.com",
    "dateOfBirth": "1990-05-15",
    "address": "12 Jubilee Hills, Hyderabad",
    "gender": "FEMALE",
    "bloodGroup": "O+"
  }'
```

**Chat with AI Agent:**
```bash
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me all admitted patients"}'
```

---

## 📁 Project Structure

```
MedSync/
├── frontend/                    # React 19 + Vite frontend
│   ├── src/
│   │   ├── components/          # Reusable UI components (Layout, Sidebar)
│   │   ├── context/             # Auth context provider
│   │   ├── pages/               # Dashboard, Patients, Agent, Analytics
│   │   └── services/            # API client modules (axios-based)
│   └── public/
│       ├── swagger.html         # Swagger UI page
│       └── medsync_openapi.yaml # OpenAPI 3.0 specification
│
├── auth-service/                # Spring Boot — JWT auth & user management
├── patient-service/             # Spring Boot — Patient CRUD + Kafka producer
├── billing-service/             # Spring Boot — Invoicing + gRPC server
├── appointment-service/         # Spring Boot — Scheduling engine
├── analytics-service/           # Spring Boot — Kafka consumer + event store
├── api-gateway/                 # Spring Cloud Gateway — JWT filter + routing
│
├── ai-agent-service/            # Python FastAPI — LangGraph ReAct agent
│   ├── app/
│   │   ├── agent/
│   │   │   ├── graph.py         # LangGraph StateGraph definition
│   │   │   └── prompts.py       # System prompt for the AI assistant
│   │   ├── api/
│   │   │   ├── routes.py        # REST /chat endpoint
│   │   │   └── websocket.py     # WebSocket streaming endpoint
│   │   ├── tools/               # 20+ tool functions for agent actions
│   │   │   ├── patient_tools.py
│   │   │   ├── billing_tools.py
│   │   │   ├── appointment_tools.py
│   │   │   └── analytics_tools.py
│   │   └── grpc_client/         # gRPC client for Billing Service
│   └── proto/                   # Protobuf definitions
│
├── deploy/
│   └── init-db.sql              # PostgreSQL multi-database initialization
├── docker-compose.yml           # Full stack orchestration (8+ services)
└── .env                         # Environment variables (API keys)
```

---

## 🤖 AI Agent — How It Works

MedSync's AI assistant is built using the **ReAct (Reasoning + Acting)** pattern via LangGraph:

```
User Message → LLM Reasoning → Tool Selection → Tool Execution → LLM Reasoning → Response
                    ↑                                                    │
                    └────────────────────────────────────────────────────┘
                              (loops until task is complete)
```

### Available Agent Tools (20+)

| Category | Tools |
|---|---|
| **Patient Ops** | `get_all_patients`, `get_patient_by_id`, `search_patients`, `create_patient`, `update_patient`, `delete_patient`, `admit_patient`, `discharge_patient` |
| **Billing Ops** | `create_billing_account`, `get_billing_status`, `add_invoice`, `record_payment`, `get_billing_details` |
| **Scheduling** | `schedule_appointment`, `get_patient_appointments`, `get_today_appointments`, `get_appointments_by_date`, `update_appointment_status` |
| **Analytics** | `get_patient_count`, `get_recent_events` |

### Example Conversations

> **You:** *"Register a new patient named Raj Kumar, male, blood group B+, address 45 MG Road Bangalore"*
> 
> **MedSync AI:** *"I've successfully registered Raj Kumar. Patient ID: 8a7b... Would you like me to schedule an appointment or set up a billing account?"*

> **You:** *"How many patients are currently admitted?"*
> 
> **MedSync AI:** *"There are currently 3 patients admitted out of 12 total registered patients."*

---

## 🔄 Event-Driven Architecture

MedSync uses Apache Kafka for asynchronous, decoupled communication between services:

```
┌────────────────┐     Protobuf      ┌─────────┐     Protobuf      ┌────────────────┐
│ Patient Service │ ──────────────►  │  Kafka  │  ──────────────►  │  Analytics     │
│   (Producer)    │   PATIENT_CREATED │  Topic: │   @KafkaListener  │  Service       │
│                 │                   │ patient │                    │  (Consumer)    │
└────────────────┘                   └─────────┘                   └────────────────┘
```

- **Producer:** When a patient is created, `patient-service` serializes a `PatientEvent` using **Protocol Buffers** and publishes it to the `patient` Kafka topic
- **Consumer:** `analytics-service` listens on the topic, deserializes the event, and persists it to a dedicated analytics database
- **Decoupling:** If the analytics service goes down, patient operations continue unaffected — Kafka retains messages until the consumer recovers

---

## 🗄️ Database Design

MedSync follows the **Database-per-Service** pattern — each microservice owns its own isolated database within a shared PostgreSQL instance:

| Database | Owner Service | Purpose |
|---|---|---|
| `auth_db` | Auth Service | User credentials and roles |
| `patient_db` | Patient Service | Patient records and status |
| `billing_db` | Billing Service | Invoices and payment ledger |
| `analytics_db` | Analytics Service | Consumed Kafka event history |
| `appointment_service_db` | Appointment Service | Scheduling records |

---

## 🧪 Testing

### Quick Smoke Test

```bash
# 1. Check all services are running
docker compose ps

# 2. Register a user
curl -X POST http://localhost:4004/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "test123"}'

# 3. Login
curl -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "test123"}'

# 4. Test AI Agent
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "hello"}'
```

### View Service Logs

```bash
docker logs pm-patient-service --tail 20    # Patient service
docker logs pm-ai-agent --tail 20           # AI agent
docker logs pm-api-gateway --tail 20        # API Gateway
docker logs pm-kafka --tail 20              # Kafka broker
```

---

## 🛑 Stopping the Application

```bash
# Stop all services
docker compose down

# Stop and remove all data (volumes)
docker compose down -v
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**Built with ❤️ using Java, Python, React, and a lot of Docker containers.**

</div>