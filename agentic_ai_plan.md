# Agentic AI Integration Plan

> This is the standalone plan for **Phase 1 only** — integrating the Python AI agent into the existing patient management system. Frontend and deployment come later.

---

## What We're Building

A Python service (`ai-agent-service`) that acts as a **conversational management interface** for the entire system. Users chat with it in natural language and it executes operations by calling your existing Java microservices.

```
User: "Register patient Rahul, email rahul@test.com, DOB 1995-08-20, 
       address Mumbai. Then set up his billing."

Agent: ✓ calls create_patient → patient-service (REST)
       ✓ calls create_billing → billing-service (gRPC)
       → "Done! Patient Rahul registered (ID: xyz-789), 
          billing account #12345 is ACTIVE."
```

**NOT an AI doctor.** No medical advice, no treatment suggestions, no risk assessment. Just a smart operator for the system.

---

## Prerequisites

- Docker + Docker Compose installed locally
- A free Google Gemini API key from [aistudio.google.com](https://aistudio.google.com)
- That's it. One key, $0 cost.

---

## Step 1: Docker Compose (Get Everything Running Together)

> **Why first?** We need all Java services + Kafka + PostgreSQL running in Docker so the Python agent can call them. Currently each service runs independently — we need one `docker-compose up` to start everything.

### [NEW] Files to Create

#### `docker-compose.yml` (project root)

```yaml
services:
  # --- Infrastructure ---
  postgres:
    image: postgres:17
    environment:
      POSTGRES_USER: admin_user
      POSTGRES_PASSWORD: password
    volumes:
      - ./deploy/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin_user"]
      interval: 5s
      timeout: 5s
      retries: 5

  kafka:
    image: bitnami/kafka:3.7
    environment:
      - KAFKA_CFG_NODE_ID=0
      - KAFKA_CFG_PROCESS_ROLES=controller,broker
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_HEAP_OPTS=-Xmx200m -Xms200m
    ports:
      - "9092:9092"
    healthcheck:
      test: ["CMD", "kafka-topics.sh", "--bootstrap-server", "localhost:9092", "--list"]
      interval: 10s
      timeout: 10s
      retries: 5

  # --- Java Services ---
  auth-service:
    build: ./auth-service
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/auth_db
      SPRING_DATASOURCE_USERNAME: admin_user
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_SQL_INIT_MODE: always
      JWT_SECRET: Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM=
    ports:
      - "4005:4005"

  billing-service:
    build: ./billing-service
    depends_on:
      postgres: { condition: service_healthy }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/billing_db
      SPRING_DATASOURCE_USERNAME: admin_user
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
    ports:
      - "4001:4001"
      - "9001:9001"

  patient-service:
    build: ./patient-service
    depends_on:
      postgres: { condition: service_healthy }
      kafka: { condition: service_healthy }
      billing-service: { condition: service_started }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/patient_db
      SPRING_DATASOURCE_USERNAME: admin_user
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_SQL_INIT_MODE: always
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      BILLING_SERVICE_ADDRESS: billing-service
      BILLING_SERVICE_GRPC_PORT: 9001
    ports:
      - "4000:4000"

  analytics-service:
    build: ./analytics-service
    depends_on:
      postgres: { condition: service_healthy }
      kafka: { condition: service_healthy }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/analytics_db
      SPRING_DATASOURCE_USERNAME: admin_user
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    ports:
      - "4002:4002"

  ai-agent-service:
    build: ./ai-agent-service
    depends_on:
      patient-service: { condition: service_started }
      billing-service: { condition: service_started }
      analytics-service: { condition: service_started }
    environment:
      PATIENT_SERVICE_URL: http://patient-service:4000
      BILLING_SERVICE_HOST: billing-service
      BILLING_SERVICE_GRPC_PORT: 9001
      ANALYTICS_SERVICE_URL: http://analytics-service:4002
      GEMINI_API_KEY: ${GEMINI_API_KEY}
    ports:
      - "4003:4003"

  api-gateway:
    build: ./api-gateway
    depends_on:
      - auth-service
      - patient-service
      - billing-service
      - analytics-service
      - ai-agent-service
    environment:
      SPRING_PROFILES_ACTIVE: docker
      AUTH_SERVICE_URL: http://auth-service:4005
    ports:
      - "4004:4004"

volumes:
  postgres_data:
```

#### `deploy/init-db.sql`

```sql
-- Creates all databases in a single PostgreSQL instance
CREATE DATABASE auth_db;
CREATE DATABASE patient_db;
CREATE DATABASE billing_db;
CREATE DATABASE analytics_db;
```

### Verification

```bash
# Start just infra + existing services (no ai-agent-service yet)
docker-compose up postgres kafka auth-service billing-service patient-service analytics-service api-gateway

# Test: should return 200
curl http://localhost:4004/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}'
```

---

## Step 2: Add Missing Endpoints to patient-service

> **Why?** The agent needs `GET /patients/{id}` (get one patient) and `GET /patients/search?q=` (find by name/email). Currently the controller only has `GET /patients` (all), `POST`, `PUT`, `DELETE`.

### Files to Modify

#### [MODIFY] [PatientRepository.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/patient-service/src/main/java/com/pm/patientservice/repository/PatientRepository.java)

Add search method:
```java
// existing:
boolean existsByEmail(String email);
boolean existsByEmailAndIdNot(String email, UUID id);

// ADD:
List<Patient> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
```

#### [MODIFY] [PatientService.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/patient-service/src/main/java/com/pm/patientservice/service/PatientService.java)

Add two methods:
```java
// ADD: Get single patient by ID
public PatientResponseDTO getPatient(UUID id) {
    Patient patient = patientRepository.findById(id)
        .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));
    return PatientMapper.toDTO(patient);
}

// ADD: Search patients by name or email
public List<PatientResponseDTO> searchPatients(String query) {
    List<Patient> patients = patientRepository
        .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    return patients.stream().map(PatientMapper::toDTO).toList();
}
```

#### [MODIFY] [PatientController.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/patient-service/src/main/java/com/pm/patientservice/controller/PatientController.java)

Add two endpoints (insert BEFORE the existing `@PutMapping`):
```java
@GetMapping("/{id}")
@Operation(summary = "Get a Patient by ID")
public ResponseEntity<PatientResponseDTO> getPatient(@PathVariable UUID id) {
    PatientResponseDTO patient = patientService.getPatient(id);
    return ResponseEntity.ok().body(patient);
}

@GetMapping("/search")
@Operation(summary = "Search Patients by name or email")
public ResponseEntity<List<PatientResponseDTO>> searchPatients(@RequestParam String q) {
    List<PatientResponseDTO> patients = patientService.searchPatients(q);
    return ResponseEntity.ok().body(patients);
}
```

### Verification

```bash
# Create a patient first
curl -X POST http://localhost:4000/patients \
  -H "Content-Type: application/json" \
  -d '{"name":"Rahul","email":"rahul@test.com","dateOfBirth":"1995-08-20","address":"Mumbai","registeredDate":"2026-07-03"}'

# Get by ID (use the returned ID)
curl http://localhost:4000/patients/{id}

# Search
curl "http://localhost:4000/patients/search?q=Rahul"
```

---

## Step 3: Add Persistence + Query to billing-service

> **Why?** Currently [BillingGrpcService.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/billing-service/src/main/java/com/pm/billingservice/grpc/BillingGrpcService.java) returns a hardcoded `accountId: "12345"`. The agent needs to: (a) actually persist billing accounts, (b) query them via a new `GetBillingAccount` gRPC method.

### New Files to Create

#### [NEW] `billing-service/src/main/java/com/pm/billingservice/model/BillingAccount.java`

```java
@Entity
public class BillingAccount {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String patientId;
    
    private String name;
    private String email;
    private String status;  // ACTIVE, SUSPENDED, CLOSED
    private LocalDate createdDate;
    
    // getters + setters
}
```

#### [NEW] `billing-service/src/main/java/com/pm/billingservice/repository/BillingAccountRepository.java`

```java
@Repository
public interface BillingAccountRepository extends JpaRepository<BillingAccount, UUID> {
    Optional<BillingAccount> findByPatientId(String patientId);
    boolean existsByPatientId(String patientId);
}
```

### Files to Modify

#### [MODIFY] `billing-service/pom.xml`

Add JPA + PostgreSQL dependencies (currently only has web + gRPC):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### [MODIFY] `billing-service/src/main/resources/application.properties`

Add port config:
```properties
server.port=4001
grpc.server.port=9001
```

#### [MODIFY] [billing_service.proto](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/patient-service/src/main/proto/billing_service.proto)

> **Must update in BOTH `patient-service/src/main/proto/` AND `billing-service/src/main/proto/`**

Add `GetBillingAccount` RPC:
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "billing";
option java_outer_classname = "BillingServiceOuterClass";

service BillingService {
  rpc CreateBillingAccount (BillingRequest) returns (BillingResponse);
  rpc GetBillingAccount (GetBillingAccountRequest) returns (BillingResponse);  // NEW
}

message BillingRequest {
  string patientId = 1;
  string name = 2;
  string email = 3;
}

message BillingResponse {
  string accountId = 1;
  string status = 2;
}

// NEW
message GetBillingAccountRequest {
  string patientId = 1;
}
```

#### [MODIFY] [BillingGrpcService.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/billing-service/src/main/java/com/pm/billingservice/grpc/BillingGrpcService.java)

Replace hardcoded logic with actual persistence:
```java
@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    private final BillingAccountRepository repository;
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    public BillingGrpcService(BillingAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createBillingAccount(BillingRequest request,
        StreamObserver<BillingResponse> observer) {
        log.info("createBillingAccount request received {}", request);

        BillingAccount account = new BillingAccount();
        account.setPatientId(request.getPatientId());
        account.setName(request.getName());
        account.setEmail(request.getEmail());
        account.setStatus("ACTIVE");
        account.setCreatedDate(LocalDate.now());
        BillingAccount saved = repository.save(account);

        observer.onNext(BillingResponse.newBuilder()
            .setAccountId(saved.getId().toString())
            .setStatus(saved.getStatus())
            .build());
        observer.onCompleted();
    }

    @Override
    public void getBillingAccount(GetBillingAccountRequest request,
        StreamObserver<BillingResponse> observer) {
        log.info("getBillingAccount request received for patient {}", request.getPatientId());

        repository.findByPatientId(request.getPatientId()).ifPresentOrElse(
            account -> {
                observer.onNext(BillingResponse.newBuilder()
                    .setAccountId(account.getId().toString())
                    .setStatus(account.getStatus())
                    .build());
                observer.onCompleted();
            },
            () -> {
                observer.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("No billing account for patient " + request.getPatientId())
                    .asRuntimeException());
            }
        );
    }
}
```

### Verification

```bash
# Create a billing account via the existing gRPC test
# Then query it:
grpcurl -plaintext -d '{"patientId":"xyz-789"}' localhost:9001 BillingService/GetBillingAccount
```

---

## Step 4: Add Persistence + REST to analytics-service

> **Why?** Currently [KafkaConsumer.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/analytics-service/src/main/java/com/pm/analyticsservice/kafka/KafkaConsumer.java) only logs events. The agent needs REST endpoints to query analytics (patient counts, recent events).

### New Files to Create

#### [NEW] `analytics-service/src/main/java/com/pm/analyticsservice/model/PatientEventRecord.java`

```java
@Entity
public class PatientEventRecord {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String eventType;      // PATIENT_CREATED, PATIENT_UPDATED
    private LocalDateTime timestamp;
    // getters + setters
}
```

#### [NEW] `analytics-service/src/main/java/com/pm/analyticsservice/repository/PatientEventRepository.java`

```java
@Repository
public interface PatientEventRepository extends JpaRepository<PatientEventRecord, UUID> {
    long countByEventType(String eventType);
    long countByTimestampAfter(LocalDateTime after);
    List<PatientEventRecord> findTop20ByOrderByTimestampDesc();
}
```

#### [NEW] `analytics-service/src/main/java/com/pm/analyticsservice/controller/AnalyticsController.java`

```java
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final PatientEventRepository repository;

    @GetMapping("/patient-count")
    public ResponseEntity<Map<String, Long>> getPatientCount() {
        long count = repository.countByEventType("PATIENT_CREATED");
        return ResponseEntity.ok(Map.of("totalPatients", count));
    }

    @GetMapping("/recent-events")
    public ResponseEntity<List<PatientEventRecord>> getRecentEvents() {
        return ResponseEntity.ok(repository.findTop20ByOrderByTimestampDesc());
    }
}
```

### Files to Modify

#### [MODIFY] `analytics-service/pom.xml`

Add JPA + PostgreSQL:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### [MODIFY] [KafkaConsumer.java](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/analytics-service/src/main/java/com/pm/analyticsservice/kafka/KafkaConsumer.java)

Add persistence (keep existing logging):
```java
@Service
public class KafkaConsumer {
    private final PatientEventRepository repository;  // ADD
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    public KafkaConsumer(PatientEventRepository repository) {  // ADD
        this.repository = repository;
    }

    @KafkaListener(topics="patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);

            // existing logging stays
            log.info("Received Patient Event: [PatientId={},PatientName={},PatientEmail={}]",
                patientEvent.getPatientId(), patientEvent.getName(), patientEvent.getEmail());

            // ADD: persist to database
            PatientEventRecord record = new PatientEventRecord();
            record.setPatientId(patientEvent.getPatientId());
            record.setPatientName(patientEvent.getName());
            record.setPatientEmail(patientEvent.getEmail());
            record.setEventType(patientEvent.getEventType());
            record.setTimestamp(LocalDateTime.now());
            repository.save(record);

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage());
        }
    }
}
```

### Verification

```bash
# Create a patient (triggers Kafka → analytics persists it)
curl -X POST http://localhost:4000/patients -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@test.com","dateOfBirth":"1990-01-01","address":"Delhi","registeredDate":"2026-07-03"}'

# Query analytics
curl http://localhost:4002/analytics/patient-count
# → {"totalPatients": 1}

curl http://localhost:4002/analytics/recent-events
# → [{patientId: "...", eventType: "PATIENT_CREATED", ...}]
```

---

## Step 5: Build the Python AI Agent Service

> This is the main event. A new Python service using LangGraph + FastAPI + Gemini.

### File Structure

```
ai-agent-service/
├── Dockerfile
├── requirements.txt
├── proto/
│   └── billing_service.proto       # Copy from billing-service
├── app/
│   ├── main.py                     # FastAPI entrypoint
│   ├── config.py                   # Pydantic Settings (env vars)
│   ├── agent/
│   │   ├── graph.py                # LangGraph ReAct agent
│   │   └── prompts.py             # System prompt
│   ├── tools/
│   │   ├── patient_tools.py       # 6 tools (get_all, get_by_id, search, create, update, delete)
│   │   ├── billing_tools.py       # 2 tools (create_account, get_status)
│   │   └── analytics_tools.py    # 2 tools (patient_count, recent_events)
│   ├── api/
│   │   ├── routes.py              # POST /agent/chat
│   │   └── websocket.py          # WS /agent/stream
│   └── grpc_client/
│       ├── billing_client.py      # Python gRPC client wrapper
│       └── generated/             # protoc output (billing_pb2.py, billing_pb2_grpc.py)
└── tests/
    └── test_tools.py
```

### Key File: `app/agent/graph.py`

```python
from langgraph.graph import StateGraph, MessagesState
from langgraph.prebuilt import ToolNode, tools_condition
from langchain_google_genai import ChatGoogleGenerativeAI
from app.agent.prompts import SYSTEM_PROMPT
from app.tools.patient_tools import (
    get_all_patients, get_patient_by_id, search_patients,
    create_patient, update_patient, delete_patient
)
from app.tools.billing_tools import create_billing_account, get_billing_status
from app.tools.analytics_tools import get_patient_count, get_recent_events

def build_agent(settings):
    llm = ChatGoogleGenerativeAI(
        model="gemini-2.0-flash",
        google_api_key=settings.gemini_api_key
    )

    all_tools = [
        get_all_patients, get_patient_by_id, search_patients,
        create_patient, update_patient, delete_patient,
        create_billing_account, get_billing_status,
        get_patient_count, get_recent_events,
    ]

    llm_with_tools = llm.bind_tools(all_tools)

    def reasoning_node(state: MessagesState):
        messages = [("system", SYSTEM_PROMPT)] + state["messages"]
        return {"messages": [llm_with_tools.invoke(messages)]}

    graph = StateGraph(MessagesState)
    graph.add_node("reason", reasoning_node)
    graph.add_node("tools", ToolNode(all_tools))

    graph.set_entry_point("reason")
    graph.add_conditional_edges("reason", tools_condition)
    graph.add_edge("tools", "reason")

    return graph.compile()
```

### Key File: `app/tools/patient_tools.py`

```python
import httpx
import json
from langchain_core.tools import tool
from app.config import get_settings

settings = get_settings()
BASE_URL = settings.patient_service_url

@tool
def get_all_patients() -> str:
    """Get a list of all patients in the system."""
    response = httpx.get(f"{BASE_URL}/patients")
    return json.dumps(response.json(), indent=2)

@tool
def get_patient_by_id(patient_id: str) -> str:
    """Get details of a specific patient by their UUID."""
    response = httpx.get(f"{BASE_URL}/patients/{patient_id}")
    if response.status_code == 404:
        return f"Patient with ID {patient_id} not found."
    return json.dumps(response.json(), indent=2)

@tool
def search_patients(query: str) -> str:
    """Search patients by name or email. Returns matching patients."""
    response = httpx.get(f"{BASE_URL}/patients/search", params={"q": query})
    return json.dumps(response.json(), indent=2)

@tool
def create_patient(name: str, email: str, date_of_birth: str, address: str) -> str:
    """Register a new patient. date_of_birth must be YYYY-MM-DD format."""
    from datetime import date
    response = httpx.post(f"{BASE_URL}/patients", json={
        "name": name,
        "email": email,
        "dateOfBirth": date_of_birth,
        "address": address,
        "registeredDate": str(date.today())
    })
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    return f"Error creating patient: {response.text}"

@tool
def update_patient(patient_id: str, name: str, email: str, date_of_birth: str, address: str) -> str:
    """Update an existing patient's information."""
    response = httpx.put(f"{BASE_URL}/patients/{patient_id}", json={
        "name": name, "email": email,
        "dateOfBirth": date_of_birth, "address": address
    })
    if response.status_code == 200:
        return json.dumps(response.json(), indent=2)
    return f"Error updating patient: {response.text}"

@tool
def delete_patient(patient_id: str) -> str:
    """Delete a patient from the system. This action cannot be undone."""
    response = httpx.delete(f"{BASE_URL}/patients/{patient_id}")
    if response.status_code == 204:
        return f"Patient {patient_id} has been successfully deleted."
    return f"Error deleting patient: {response.text}"
```

### Key File: `app/tools/billing_tools.py`

```python
import grpc
import json
from langchain_core.tools import tool
from app.grpc_client.generated import billing_pb2, billing_pb2_grpc
from app.config import get_settings

settings = get_settings()

@tool
def create_billing_account(patient_id: str, name: str, email: str) -> str:
    """Create a billing account for a patient."""
    channel = grpc.insecure_channel(f"{settings.billing_service_host}:{settings.billing_service_grpc_port}")
    stub = billing_pb2_grpc.BillingServiceStub(channel)
    response = stub.CreateBillingAccount(
        billing_pb2.BillingRequest(patientId=patient_id, name=name, email=email)
    )
    channel.close()
    return json.dumps({"accountId": response.accountId, "status": response.status})

@tool
def get_billing_status(patient_id: str) -> str:
    """Check a patient's billing account status by their patient ID."""
    channel = grpc.insecure_channel(f"{settings.billing_service_host}:{settings.billing_service_grpc_port}")
    stub = billing_pb2_grpc.BillingServiceStub(channel)
    try:
        response = stub.GetBillingAccount(
            billing_pb2.GetBillingAccountRequest(patientId=patient_id)
        )
        channel.close()
        return json.dumps({"accountId": response.accountId, "status": response.status})
    except grpc.RpcError as e:
        channel.close()
        return f"No billing account found for patient {patient_id}"
```

### Key File: `app/api/routes.py`

```python
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()

class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    response: str
    tool_calls: list[dict] = []

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    result = await agent.ainvoke({
        "messages": [("user", request.message)]
    })
    
    # Extract tool calls from the message history for transparency
    tool_calls = []
    for msg in result["messages"]:
        if hasattr(msg, "tool_calls") and msg.tool_calls:
            for tc in msg.tool_calls:
                tool_calls.append({"name": tc["name"], "args": tc["args"]})
    
    return ChatResponse(
        response=result["messages"][-1].content,
        tool_calls=tool_calls
    )
```

### Key File: `app/api/websocket.py`

```python
from fastapi import WebSocket, APIRouter

router = APIRouter()

@router.websocket("/stream")
async def stream(websocket: WebSocket):
    await websocket.accept()
    data = await websocket.receive_json()

    async for event in agent.astream_events(
        {"messages": [("user", data["message"])]},
        version="v2"
    ):
        kind = event["event"]
        if kind == "on_chat_model_stream":
            chunk = event["data"]["chunk"]
            if chunk.content:
                await websocket.send_json({"type": "token", "content": chunk.content})
        elif kind == "on_tool_start":
            await websocket.send_json({
                "type": "tool_start",
                "name": event["name"],
                "input": str(event["data"].get("input", ""))
            })
        elif kind == "on_tool_end":
            await websocket.send_json({
                "type": "tool_end",
                "name": event["name"],
                "output": event["data"].content if hasattr(event["data"], "content") else str(event["data"])
            })

    await websocket.send_json({"type": "done"})
    await websocket.close()
```

### API Gateway Route

#### [MODIFY] [application.yml](file:///Users/shreyanand/dev_proj/patient-management-system-github/java-spring-microservices/api-gateway/src/main/resources/application.yml)

Add routes for ai-agent and analytics after existing routes:
```yaml
        - id: ai-agent-route
          uri: http://ai-agent-service:4003
          predicates:
            - Path=/agent/**
          filters:
            - JwtValidation

        - id: analytics-route
          uri: http://analytics-service:4002
          predicates:
            - Path=/api/analytics/**
          filters:
            - StripPrefix=1
            - JwtValidation
```

### Final Verification

```bash
# Start everything
GEMINI_API_KEY=your-key-here docker-compose up --build

# 1. Simple query
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me all patients"}'

# 2. Multi-step workflow
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Register patient Rahul, email rahul@test.com, DOB 1995-08-20, address Mumbai. Then set up his billing."}'
# Expected: Agent calls create_patient, then create_billing_account, reports both results

# 3. Search
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Find patient Rahul"}'

# 4. Analytics
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "How many patients are in the system?"}'

# 5. Refusal test (should NOT give medical advice)
curl -X POST http://localhost:4003/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What medication should Rahul take for his headache?"}'
# Expected: "I cannot provide medical advice..."

# 6. Through API Gateway (with JWT auth)
TOKEN=$(curl -s -X POST http://localhost:4004/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@test.com","password":"password123"}' | jq -r '.token')

curl -X POST http://localhost:4004/agent/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"message": "List all patients and their count"}'
```

---

## Summary: All Changes at a Glance

| # | File | Action | Step |
|---|---|---|---|
| 1 | `docker-compose.yml` | NEW | Step 1 |
| 2 | `deploy/init-db.sql` | NEW | Step 1 |
| 3 | `PatientRepository.java` | ADD search method | Step 2 |
| 4 | `PatientService.java` | ADD getPatient + searchPatients | Step 2 |
| 5 | `PatientController.java` | ADD GET /{id} + GET /search | Step 2 |
| 6 | `billing-service/pom.xml` | ADD JPA + PostgreSQL deps | Step 3 |
| 7 | `BillingAccount.java` | NEW (entity) | Step 3 |
| 8 | `BillingAccountRepository.java` | NEW (repo) | Step 3 |
| 9 | `billing_service.proto` (×2) | ADD GetBillingAccount RPC | Step 3 |
| 10 | `BillingGrpcService.java` | REWRITE with persistence | Step 3 |
| 11 | `billing application.properties` | ADD port config | Step 3 |
| 12 | `analytics-service/pom.xml` | ADD JPA + PostgreSQL deps | Step 4 |
| 13 | `PatientEventRecord.java` | NEW (entity) | Step 4 |
| 14 | `PatientEventRepository.java` | NEW (repo) | Step 4 |
| 15 | `AnalyticsController.java` | NEW (REST endpoints) | Step 4 |
| 16 | `KafkaConsumer.java` | ADD persistence | Step 4 |
| 17 | `ai-agent-service/*` | NEW (entire Python service) | Step 5 |
| 18 | `api-gateway application.yml` | ADD agent + analytics routes | Step 5 |
