# MedSync — AI-Powered Patient Management System

MedSync is a production-grade, microservice-based Patient Management System (PMS). It integrates Spring Boot Java microservices, a stateful Python AI Agent (LangGraph + Gemini-2.5-flash) acting as the conversational nervous system, and a professional, light-theme React dashboard.

---

## System Architecture

```
                       ┌─────────────────────────┐
                       │   React SPA (Vite)      │
                       │   (Inter / Teal Vibe)   │
                       └────────────┬────────────┘
                                    │ HTTP / WS
                                    ▼
                       ┌─────────────────────────┐
                       │  Spring Cloud Gateway   │ (Port 4004)
                       └────────────┬────────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         │ HTTP                     │ HTTP / WS                │ HTTP
         ▼                          ▼                          ▼
┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
│  Auth Service   │        │  Patient Serv.  │        │ Analytics Serv. │
│   (Port 4005)   │        │   (Port 4000)   │        │   (Port 4002)   │
└────────┬────────┘        └────────┬────────┘        └────────┬────────┘
         │                          │                          │
         │                          │ Kafka Event              │ Kafka Event
         │                          ▼                          ▼
         │                 ┌─────────────────┐        ┌─────────────────┐
         │                 │   Apache Kafka  │───────▶│  PostgreSQL DB  │
         │                 │   (Port 9092)   │        │   (Port 5432)   │
         │                 └─────────────────┘        └────────┬────────┘
         │                                                     ▲
         │                                                     │
         │                 ┌─────────────────┐                 │
         └────────────────▶│    AI Agent     │─────────────────┘
           HTTP            │   (Port 4003)   │   gRPC (Port 9001)
                           └────────┬────────┘
                                    │
                                    ▼
                           ┌─────────────────┐
                           │ Billing Service │
                           │   (Port 4001)   │
                           └─────────────────┘
```

### Components

1. **Frontend Dashboard:** A professional React 18 + Vite SPA styled with a clean light theme (Teal accent `#0F766E`, no purple, no gradients) using Inter font. Features full Patient CRUD, charts, and an interactive AI Assistant panel with live tool call status.
2. **API Gateway:** Spring Cloud Gateway (port `4004`) managing routing, global CORS config, and token-based validation filter.
3. **Auth Service:** Spring Boot Security service generating JWT tokens. Includes seeded admin user.
4. **Patient Service:** Spring Boot JPA service managing patient records and scheduling. Emits Kafka events on patient registration.
5. **Billing Service:** Spring Boot gRPC service persisting billing accounts to database.
6. **Analytics Service:** Spring Boot Kafka listener saving events to database and exposing REST endpoints for metrics.
7. **AI Agent Service:** Python FastAPI service built with LangGraph and `gemini-2.5-flash`. Runs tool-calling workflows to perform multi-step hospital actions (e.g., register patient + set up billing in one prompt) via REST and gRPC calls.

---

## Quick Start (Local Docker Compose)

### Prerequisites
- Docker Desktop running
- A Gemini API Key

### Running the Application

1. Create a `.env` file in the root directory:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

2. Start the services:
   ```bash
   docker-compose up --build -d
   ```

3. Start the React frontend development server:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. Open [http://localhost:5173/](http://localhost:5173/) in your browser.

---

## Demo Credentials

Sign in with the seeded admin account:
*   **Email:** `testuser@test.com`
*   **Password:** `password123`

---

## AI Agent Capabilities

The AI Agent acts as a stateful chatbot helper for hospital admins.

*   **Allowed Operations:**
    *   Querying patients (e.g., *"Find patient Rahul"*)
    *   Adding new patient records (e.g., *"Register Jane Doe..."*)
    *   Setting up and checking billing status (gRPC)
    *   Chaining multi-step workflows (e.g., *"Register Priya Sharma, email priya@test.com, DOB 1990-10-15, address Delhi, then set up her billing account"*).
    *   Viewing hospital-wide metrics (REST)
*   **Strict Limits (No AI Doctor):**
    *   The system prompt explicitly blocks the agent from providing medical advice, treatment recommendations, clinical risk assessment, or diagnoses. It will refuse clinical prompts gracefully.