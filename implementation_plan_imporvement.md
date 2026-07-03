# Enhancing MedSync — From Basic CRUD to a Proper Patient Management System

## What We Have Now (Gaps)

| Area | Current State | Problem |
|---|---|---|
| **Patient Model** | name, email, address, DOB, registeredDate | No phone, gender, blood group, emergency contact, room/bed, admission status |
| **Billing** | Create account + status (ACTIVE) | No invoices, line items, payments, outstanding balance, insurance info |
| **Frontend** | Table + search + modal form | No patient detail page, no billing page, dashboard is empty-feeling |
| **Analytics** | Kafka event count | No meaningful charts, no admission stats, no billing summaries |
| **Appointments** | ❌ Doesn't exist | A real PMS needs scheduling |
| **AI Agent** | Works, but limited data to query | Can't query admissions, appointments, or billing details |

---

## Proposed Changes

### Module 1: Expanded Patient Data Model

> The patient record should look like what a hospital front-desk actually uses.

#### [MODIFY] Patient Entity + DTO + Seed Data

**New fields on `Patient`:**

| Field | Type | Example |
|---|---|---|
| `phone` | String | `+91-9876543210` |
| `gender` | Enum (MALE/FEMALE/OTHER) | `MALE` |
| `bloodGroup` | String | `O+`, `AB-` |
| `emergencyContactName` | String | `Priya Sharma` |
| `emergencyContactPhone` | String | `+91-9123456789` |
| `status` | Enum (ACTIVE/DISCHARGED/ADMITTED) | `ACTIVE` |
| `roomNumber` | String (nullable) | `ICU-201`, `GEN-305` |
| `bedNumber` | String (nullable) | `A`, `B` |
| `admissionDate` | LocalDate (nullable) | Only set when status=ADMITTED |
| `dischargeDate` | LocalDate (nullable) | Only set when status=DISCHARGED |
| `notes` | Text (nullable) | Free-text clinical notes |

**Files to modify:**
- `Patient.java` — add new fields
- `PatientRequestDTO.java` / `PatientResponseDTO.java` — add new fields (optional ones nullable)
- `data.sql` — update all 15 seed patients with realistic data (some ADMITTED with room/bed, some DISCHARGED, most ACTIVE)

---

### Module 2: Enhanced Billing

> Currently billing is just "create account → ACTIVE". A real system tracks invoices and payments.

#### [MODIFY] BillingAccount + [NEW] Invoice Entity

**Enhanced `BillingAccount` fields:**

| Field | Type | Purpose |
|---|---|---|
| `insuranceProvider` | String | `Star Health`, `ICICI Lombard` |
| `insurancePolicyNumber` | String | `POL-12345-2026` |
| `totalBilled` | BigDecimal | Sum of all invoices |
| `totalPaid` | BigDecimal | Sum of all payments |
| `outstandingBalance` | BigDecimal | `totalBilled - totalPaid` |

**New `Invoice` entity:**

| Field | Type | Purpose |
|---|---|---|
| `id` | UUID | Primary key |
| `billingAccountId` | UUID | FK to BillingAccount |
| `description` | String | `Room Charge - ICU (3 days)`, `Lab Work - CBC` |
| `amount` | BigDecimal | `15000.00` |
| `invoiceDate` | LocalDate | When the charge was created |
| `status` | Enum | `PENDING`, `PAID`, `OVERDUE` |

**New gRPC methods:**
- `AddInvoice` — add a charge to a patient's billing
- `RecordPayment` — mark invoice as paid
- `GetBillingDetails` — returns account + all invoices

**Files:**
- `Invoice.java` — new entity
- `InvoiceRepository.java` — new repo
- `billing_service.proto` — add new RPCs and messages
- `BillingGrpcService.java` — implement new methods
- Seed some invoices in `data.sql` for billing-service

---

### Module 3: Appointment Scheduling (New REST Endpoints on Patient Service)

> Rather than a whole new microservice, add appointments as a feature within patient-service.

**New `Appointment` entity:**

| Field | Type | Purpose |
|---|---|---|
| `id` | UUID | Primary key |
| `patientId` | UUID | FK to Patient |
| `doctorName` | String | `Dr. Mehta` |
| `department` | String | `Cardiology`, `General Medicine` |
| `appointmentDate` | LocalDate | `2026-07-10` |
| `timeSlot` | String | `10:00 AM - 10:30 AM` |
| `status` | Enum | `SCHEDULED`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
| `notes` | String (nullable) | `Follow-up for ECG results` |

**New endpoints:**
- `POST /patients/{id}/appointments` — schedule appointment
- `GET /patients/{id}/appointments` — list patient's appointments
- `GET /appointments?date=2026-07-10` — list all appointments for a date
- `PUT /appointments/{id}` — update status (complete/cancel)

**Files:**
- `Appointment.java` — new entity
- `AppointmentRepository.java`
- `AppointmentController.java`
- `AppointmentService.java`
- Update `data.sql` with sample appointments

---

### Module 4: Frontend Enhancements

#### 4a. Patient Detail Page (`/patients/:id`)

A full detail view when you click a patient row:

```
┌────────────────────────────────────────────────────────────────┐
│ ← Back to Patients                            [Edit] [Delete] │
│                                                                │
│  Rahul Sharma                          Status: ● ADMITTED      │
│  rahul@test.com  •  +91-9876543210  •  Male  •  O+            │
│                                                                │
│  [Overview]  [Appointments]  [Billing]                         │
│  ─────────────────────────────────────────────────────────────  │
│                                                                │
│  Personal Info              Admission Info                     │
│  ┌────────────────────┐    ┌────────────────────────────┐     │
│  │ DOB: 1995-08-20    │    │ Room: ICU-201 / Bed: A      │     │
│  │ Address: Mumbai     │    │ Admitted: 2026-06-28         │     │
│  │ Registered: 2026-06 │    │ Emergency: Priya (+91-912..)│     │
│  └────────────────────┘    └────────────────────────────┘     │
│                                                                │
│  Notes                                                         │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Post-surgery observation. Vitals stable.                  │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
```

#### 4b. Appointments Tab (within Patient Detail)

```
│  Upcoming Appointments                    [+ Schedule New]     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Jul 10, 2026  •  10:00 AM  •  Dr. Mehta (Cardiology)  │    │
│  │ Status: SCHEDULED                                      │    │
│  │ Notes: Follow-up for ECG results                       │    │
│  └────────────────────────────────────────────────────────┘    │
```

#### 4c. Billing Tab (within Patient Detail)

```
│  Billing Account: #12345  •  Status: ACTIVE                   │
│  Insurance: Star Health (POL-12345-2026)                       │
│                                                                │
│  Outstanding Balance: ₹12,500                                  │
│                                                                │
│  Invoices                                    [+ Add Charge]    │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Description         │ Amount   │ Date       │ Status    │ │
│  │ Room Charge (3 days)│ ₹15,000  │ 2026-06-30 │ PAID      │ │
│  │ Lab - CBC           │ ₹2,500   │ 2026-07-01 │ PENDING   │ │
│  │ Medicine            │ ₹10,000  │ 2026-07-02 │ PENDING   │ │
│  └──────────────────────────────────────────────────────────┘ │
```

#### 4d. Enhanced Dashboard

Add real numbers:
- **Admitted Patients** count (status=ADMITTED)
- **Today's Appointments** count
- **Outstanding Billing** total
- **Registration Trend** chart (using Recharts — we already have it installed)

#### 4e. Patients Table Enhancements

- Add **Status** column with colored badges (ACTIVE=green, ADMITTED=blue, DISCHARGED=gray)
- Add **Room** column
- Click row → navigate to Patient Detail page

---

### Module 5: AI Agent Updates

Add new tools so the agent can interact with the enhanced data:

| Tool | What it does |
|---|---|
| `admit_patient` | Set status=ADMITTED, assign room/bed |
| `discharge_patient` | Set status=DISCHARGED, clear room |
| `schedule_appointment` | Create appointment for a patient |
| `get_appointments` | List appointments for patient or date |
| `add_invoice` | Add billing charge to patient |
| `get_billing_details` | Get full billing with invoices |

Example conversations that should work:
- *"Admit patient Rahul to room ICU-201, bed A"*
- *"Schedule an appointment for John Doe with Dr. Mehta in Cardiology on July 10 at 10 AM"*
- *"Add a lab charge of ₹2500 for CBC test to Rahul's billing"*
- *"What is the outstanding balance for patient Rahul?"*
- *"Show me all appointments for today"*

---

## What We Are NOT Adding

> [!IMPORTANT]
> Staying true to the original philosophy — **management system, not AI doctor.**

- ❌ No medical records / diagnosis / prescription management
- ❌ No treatment recommendations
- ❌ No drug interaction checks
- ❌ No clinical decision support
- ❌ No role-based access (keeping single ADMIN role for simplicity)

---

## Implementation Order

| Step | Task | Estimated Effort |
|---|---|---|
| 1 | **Patient model expansion** — add fields to entity, DTO, seed data | Medium |
| 2 | **Patient service endpoints** — admit/discharge/room assignment | Small |
| 3 | **Appointments** — entity, repo, controller, service, seed data | Medium |
| 4 | **Billing enhancement** — Invoice entity, new gRPC methods, seed data | Medium |
| 5 | **Frontend: Patient Detail page** — with Overview/Appointments/Billing tabs | Large |
| 6 | **Frontend: Dashboard enhancement** — real stats, Recharts trend chart | Medium |
| 7 | **Frontend: Patients table** — status badges, room column, row click nav | Small |
| 8 | **AI Agent** — add 6 new tools for admissions, appointments, billing | Medium |
| 9 | **API Gateway** — add appointment route if needed | Small |
| 10 | **Seed data refresh** — realistic hospital data across all entities | Small |

---

## Open Questions

> [!IMPORTANT]
> Please review these before I start executing:

1. **Currency** — Should billing amounts display in ₹ (INR) or $ (USD)?
2. **Departments** — Do you want a fixed list of departments (Cardiology, Orthopedics, General Medicine, etc.) or free-text?
3. **Room numbering** — Should rooms follow a pattern like `ICU-201`, `GEN-305`, `PED-101` or just plain numbers?
4. **Appointments** — Should we keep them in patient-service or create a separate `appointment-service` microservice?
5. **Any other features** you want added that I haven't listed?
