# HealthBoxD Architecture & Technical Design

## Runtime Architecture

HealthBoxD is a **multi-tenant modular monolith**. Domain-oriented modules are
kept under `modules/` and expose application services through REST adapters;
shared infrastructure lives under `config/`, `security/`, and `shared/`.

- **Layered/hexagonal boundaries:** controllers are inbound adapters, services
   are application/domain boundaries, repositories are outbound persistence
   ports, and payment/SMS providers remain replaceable adapters.
- **Tenant isolation:** JWT claims populate a request-scoped tenant context;
   the transaction boundary sets PostgreSQL `app.tenant_id` with `set_config`.
   Migration `V16__postgres_row_level_tenancy.sql` enables and forces RLS on
   tenant tables, so database policy enforcement backs up service checks.
- **Stateless security:** Spring Security uses stateless sessions and signed
   JWTs. No server session or in-memory tenant state is required, so instances
   can scale horizontally.
- **Domain events:** modules publish typed events through the shared
   `DomainEventPublisher` port and Spring adapter. Appointment and payment
   workflows communicate without direct module coupling.
- **Deployment:** Docker images are immutable and the compose deployment
   declares two backend replicas, health checks, restart policy, and resource
   limits. Use a reverse proxy/load balancer for the published backend port in
   production.

## System Overview

HealthBoxD is a **multi-tenant healthcare ERP** built on a modern Spring Boot + React stack with production-grade security, scalability, and reliability.

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React 19 + Vite)               │
│              (Vercel/Netlify - Auto-deployed)               │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS + JWT Token
                       │
    ┌──────────────────▼──────────────────┐
    │   API Gateway / Load Balancer       │
    │   (Render/Railway/K8s)              │
    └──────────────────┬──────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│          Backend (Spring Boot 3.5.7 + Java 21)              │
│         (Stateless, Horizontally Scalable)                  │
│                                                               │
│  ├─ Security Layer (Spring Security + JWT)                  │
│  │  └─ TenantContext Filter (Row-level security)            │
│  │                                                            │
│  ├─ REST API Controllers (65 endpoints)                     │
│  │  ├─ Auth (login, register, OTP)                          │
│  │  ├─ Appointments (CRUD, scheduling)                      │
│  │  ├─ Consultations (DRAFT → FINALIZED)                    │
│  │  ├─ Prescriptions (manage drugs)                         │
│  │  ├─ Billing (charges, line items)                        │
│  │  ├─ Payments (Razorpay integration)                      │
│  │  ├─ Pharmacy (dispensing records)                        │
│  │  └─ Dashboard (analytics)                                │
│  │                                                            │
│  ├─ Service Layer (Business Logic)                          │
│  │  └─ Multi-tenant isolation @ query level                 │
│  │                                                            │
│  ├─ Repository Layer (Spring Data JPA)                      │
│  │  └─ Automatic tenant filtering                           │
│  │                                                            │
│  └─ Data Validation                                         │
│     └─ Bean Validation (@Valid)                             │
└──────────────────────┬──────────────────────────────────────┘
                       │ JDBC (HikariCP connection pool)
                       │
┌──────────────────────▼──────────────────────────────────────┐
│         PostgreSQL 16 (Managed: Render/Railway/Neon)        │
│                                                               │
│  ├─ Users (login identity - global)                         │
│  ├─ Hospitals (tenant/clinic records)                       │
│  ├─ Patients (clinical profiles)                            │
│  ├─ Appointments (scheduling)                               │
│  ├─ Consultations (medical encounters)                      │
│  ├─ Prescriptions (drug management)                         │
│  ├─ Charges (line items per consultation)                   │
│  ├─ Payments (transaction tracking)                         │
│  ├─ Pharmacy (dispensing records)                           │
│  └─ OTP Tokens (email verification)                         │
│                                                               │
│  └─ All tables include `hospital_id` for multi-tenancy      │
└──────────────────────────────────────────────────────────────┘
```

## Multi-Tenancy Design

### Strategy: Shared Database, Row-Level Security

**Why this approach?**
- Simpler operational complexity than DB-per-tenant
- Cost-effective for thousands of tenants
- No data migration between databases
- Shared schema simplifies maintenance

### Enforcement Layers

1. **Authentication Layer**
   ```
   Login → JWT Token with hospitalId claim
           ↓
   Token contains: { subject, role, hospitalId, phno }
   ```

2. **Request Filter**
   ```
   JwtAuthenticationFilter
   ├─ Extracts token from Authorization header
   ├─ Validates signature (RS256)
   └─ Sets TenantContext (ThreadLocal) with hospitalId
   ```

3. **Query Filtering (Spring Data JPA)**
   ```
   PatientRepository.findAll()
   ├─ Spring Data intercepts
   ├─ Appends "WHERE hospital_id = ?" to query
   └─ Returns only current tenant's data
   ```

4. **Database Indexes**
   ```
   CREATE INDEX idx_patients_hospital ON patients(hospital_id);
   CREATE INDEX idx_appointments_hospital_doctor 
     ON appointments(hospital_id, doctor_phno);
   ```

### Trust Boundary
- **Never trust client-supplied tenant_id**: Always derive from JWT
- **Every JPA query auto-scoped**: No manual WHERE clauses needed
- **Cross-tenant tests**: Verify isolation at integration level

## Authentication & Authorization

### JWT Flow

```
1. User submits credentials
   POST /auth/login { username, password }
   
2. Backend verifies password
   ├─ Query user by username
   ├─ bcrypt.matches(password, hashed)
   └─ On mismatch: throw 401
   
3. Generate JWT token
   Header: { alg: "RS256", typ: "JWT" }
   Payload: {
     sub: "username",
     hospitalId: 123,
     role: "ADMIN",
     phno: "+919876543210",
     iat: 1693730400,
     exp: 1693816800  (24 hours)
   }
   Signature: HMACSHA256(RS256_KEY, header.payload)
   
4. Return LoginResponse
   { token, username, role, hospitalId, phno }
   
5. Client stores token in localStorage
   
6. Frontend adds to all API requests
   Authorization: Bearer <token>
   
7. Backend filter validates token
   ├─ Parse and verify signature
   ├─ Check expiration
   └─ Extract claims into TenantContext
```

### Roles & Permissions

| Role | Abilities | Tenant-Scoped |
|------|-----------|---------------|
| SUPER_ADMIN | Platform analytics, user management | Global (all tenants) |
| ADMIN | Hospital/clinic management, staff, charge master | Current tenant only |
| PATIENT | View appointments, consultations, prescriptions | Current tenant only |

## API Endpoints (65 total)

### Authentication (`/auth`)
```
POST   /login                     Login
POST   /register-hospital         Register new clinic + admin
POST   /register-patient          Register patient under clinic
POST   /otp/request               Request OTP for email verification
```

### Appointments (`/api/appointments`)
```
GET    /                          List tenant's appointments
POST   /                          Create appointment
GET    /{id}                      Get appointment details
DELETE /{id}                      Cancel appointment
```

### Consultations (`/api/consultations`)
```
GET    /                          List consultations (DRAFT + FINALIZED)
POST   /                          Create DRAFT consultation
GET    /{id}                      Get consultation details
PATCH  /{id}/finalize             Finalize (lock) consultation
```

### And 52 more endpoints for:
- Prescriptions, Billing, Charges, Payments, Pharmacy, Scheduling, Dashboard, etc.

## Data Flow: Appointment → Payment

```
1. Doctor schedules slots
   POST /api/schedule { hospital_id, doctor_phno, day_of_week, time }
   
2. Patient books appointment
   POST /api/appointments { slot_id, patient_phno }
   └─ Creates Appointment(status=SCHEDULED)
   
3. Doctor creates DRAFT consultation
   POST /api/consultations { appointment_id, diagnosis, type: "OP" }
   └─ Creates Consultation(status=DRAFT)
   
4. Doctor adds prescription items
   POST /api/prescriptions { consultation_id, drug, dosage, qty }
   
5. Doctor finalizes consultation
   PATCH /api/consultations/{id}/finalize
   ├─ Consultation.status → FINALIZED
   ├─ ConsultationMaterializer.generate() runs:
   │  ├─ Reads Charge Master (fees, medicines, tests)
   │  ├─ Creates Billing record
   │  ├─ Creates ChargeLineItems (each line)
   │  └─ Billing.status → PENDING
   └─ Charges screen now shows editable line items
   
6. Billing staff edits charges (optional)
   PATCH /api/charges/{id}
   ├─ qty or price override
   ├─ Logs edit (edited_by, edited_at)
   ├─ Recalculates total
   └─ Stays PENDING
   
7. Patient pays (cash or online)
   POST /api/payments/checkout { billing_id, amount }
   ├─ Calls Razorpay API
   ├─ Returns order (order_id, amount)
   └─ Creates PaymentOrder(status=PENDING)
   
8. Customer verifies payment
   POST /api/payments/verify { order_id, payment_id, signature }
   ├─ Validates Razorpay signature
   ├─ Billing.status → PAID
   ├─ Creates DoctorEarning (if applicable)
   └─ Charges locked (no further edits)
   
9. Pharmacy dispenses
   PATCH /api/pharmacy/{id}/dispense { quantity }
   └─ PharmacyRecord.status → DISPENSED
```

## Consultation State Machine

```
                    CREATE DRAFT
                         ↓
        ┌────────────────────────────────┐
        │    CONSULTATION (DRAFT)        │
        │ • Can add/edit prescriptions   │
        │ • Can attach lab reports       │
        │ • Can switch between drafts    │
        └────────────────────────────────┘
                    ↓ FINALIZE
                    │
        ┌───────────▼─────────────────┐
        │ CONSULTATION (FINALIZED)    │
        │ • Charges auto-generated    │
        │ • Locked (no further edits) │
        │ • Visible to patient        │
        └───────────┬─────────────────┘
                    │
     ┌──────────────┴────────────────┐
     ↓                               ↓
   ┌─────────┐                   ┌─────────┐
   │  PAID   │ ← Payment OK       │ UNPAID  │
   │ LOCKED  │                   │ PENDING │
   └─────────┘                   └─────────┘
```

**Key Rule:** Only FINALIZED + PAID consultations visible to patient app or in reports.

## Security Architecture

### 1. Transport Security
- HTTPS enforced (automatic on Render/Railway)
- TLS 1.2+ only
- HSTS headers (Strict-Transport-Security)

### 2. Application Security
- Spring Security filter chain
- CORS restricted to frontend domain only
- CSRF protection (SameSite cookies)
- Rate limiting on auth endpoints (brute-force prevention)

### 3. Data Security
- Input validation (@Valid DTOs)
- Parameterized queries (JPA/Hibernate)
- SQL injection immune
- XSS prevention (no inline scripts in frontend)
- Password hashing (bcrypt, 10 rounds)

### 4. Audit Trail
```sql
-- Every charge edit logged
ChargeLineItem:
  edited_by: user.phno,
  edited_at: timestamp

-- Every payment tracked
PaymentOrder:
  order_id: razorpay_order_id,
  payment_id: razorpay_payment_id,
  signature: verified_signature

-- Consultation finalization tracked
Consultation:
  finalized_at: timestamp,
  finalized_by: doctor_phno
```

### 5. HIPAA-Aligned Baseline
- Encryption at rest (database-level or app-level AES-256)
- Encryption in transit (TLS)
- Access logs (who viewed what, when)
- Data retention policy enforced
- PII minimization (phone numbers indexed, never in URLs)

## Performance Tuning

### Database
```properties
# Connection Pooling (HikariCP)
maximum-pool-size=20
minimum-idle=5
connection-timeout=30s
idle-timeout=10m

# Batch Processing
hibernate.jdbc.batch_size=50
hibernate.order_inserts=true
hibernate.order_updates=true

# Indexing
idx_patients_hospital (hospital_id)
idx_appointments_hospital_doctor (hospital_id, doctor_phno)
idx_prescriptions_hospital (hospital_id)
idx_charges_hospital (hospital_id)
```

### Caching (Optional)
```
Redis cache for:
  - Charge Master (read-heavy, changes rare)
  - JWT token blacklist (on logout)
  - OTP tokens (5min TTL)
```

### Frontend
```
- Code splitting (lazy load routes)
- Bundle size <500KB (gzipped)
- Image optimization (responsive sizes)
- Cache-busting (hash-based filenames)
- CDN for static assets (Vercel/Netlify)
```

## Scalability

### Horizontal Scaling
- **Backend:** Stateless (JWT-only) → Multiple instances behind load balancer
- **Database:** Managed PostgreSQL (Render/Railway handles replication)
- **Scheduled Jobs:** Use ShedLock for distributed locks (draft expiry)

### Vertical Scaling
- JVM heap: `-Xms256m -Xmx1g`
- Database pool: `maximum-pool-size=20+`
- Tomcat threads: `200 max`

### Growth Path
- Shared DB scales to 10,000+ tenants comfortably
- At 100,000+ tenants: consider DB-per-tenant sharding
- No schema changes needed, only operational decisions

## Deployment Architecture

### Local Development
```
docker-compose up
├─ PostgreSQL 16 (localhost:5432)
├─ Spring Boot (localhost:8080)
└─ Frontend: npm run dev (localhost:5173)
```

### Production (Render.com example)
```
GitHub Push
    ↓
GitHub Actions CI/CD
    ├─ Build backend (Maven)
    ├─ Run tests
    ├─ Build Docker image
    └─ Push to Container Registry
         ↓
    Render.com (auto-triggered)
    ├─ Deploy backend (docker pull + run)
    ├─ Run Flyway migrations (auto)
    ├─ Health check (pass → live)
    └─ Scale: 1 instance (or more)

Frontend (Vercel/Netlify)
    ├─ GitHub Push → auto-deploy
    ├─ npm run build
    ├─ Deploy to CDN (worldwide)
    └─ Environment: VITE_API_URL=production-backend
```

## Database Schema (Simplified View)

```sql
-- Multi-tenant isolation via hospital_id
hospitals {
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(150),
  code VARCHAR(30) UNIQUE,
  created_at TIMESTAMP
}

users {
  phno VARCHAR(15) PRIMARY KEY,
  username VARCHAR(100),
  password VARCHAR(255),  -- bcrypt hashed
  email VARCHAR(100),
  role ENUM('SUPER_ADMIN', 'ADMIN', 'PATIENT'),
  hospital_id BIGINT REFERENCES hospitals(id)
}

patients {
  id BIGSERIAL PRIMARY KEY,
  hospital_id BIGINT REFERENCES hospitals(id),
  phone VARCHAR(15),  -- Unique within hospital_id
  name VARCHAR(150),
  dob DATE,
  UNIQUE(hospital_id, phone)
}

consultations {
  id BIGSERIAL PRIMARY KEY,
  hospital_id BIGINT REFERENCES hospitals(id),
  appointment_id BIGINT,
  doctor_phno VARCHAR(15),
  status ENUM('DRAFT', 'FINALIZED'),
  finalized_at TIMESTAMP,
  created_at TIMESTAMP
}

billing {
  id BIGSERIAL PRIMARY KEY,
  hospital_id BIGINT REFERENCES hospitals(id),
  consultation_id BIGINT REFERENCES consultations(id),
  status ENUM('PENDING', 'PAID'),
  total_amount BIGINT,  -- In paise
  created_at TIMESTAMP
}

-- Every query auto-filters:
-- SELECT * FROM patients WHERE hospital_id = ?
-- (enforced by Spring Data + TenantContext filter)
```

## Testing Strategy

### Unit Tests
- Service layer business logic
- DTO validation
- JWT token generation/validation

### Integration Tests
- API endpoints (auth, CRUD)
- Multi-tenant isolation (cross-tenant queries blocked)
- Database transactions

### Security Tests
- SQL injection attempts (blocked)
- XSS attempts (blocked)
- CSRF protection (verified)
- Authentication required (401 on missing token)

### Load Tests
- 100 concurrent users
- Response time <2s (95th percentile)
- Database connection pool doesn't exhaust

## Monitoring & Observability

### Logs
- Centralized logging (Sentry/DataDog)
- Error alerts (Slack/email)
- Sensitive data excluded (no passwords/tokens)
- Production log level: INFO

### Metrics
- API response times (APM)
- Database query performance (slow query log)
- Error rate (% of failed requests)
- CPU/Memory usage (system metrics)

### Health Checks
```
GET /actuator/health
Response:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

## Summary

HealthBoxD combines:
- **Simplicity:** Shared-DB multi-tenancy, no complex sharding logic
- **Security:** JWT + ABAC, row-level isolation, audit trails
- **Scalability:** Stateless backend, managed database, CDN for frontend
- **Reliability:** Automated deployments, health checks, monitoring
- **Maintainability:** Spring Boot conventions, clean code, modular structure

Perfect for healthcare SaaS serving 100s of clinics with 1000s of patient records each.

---
**Architecture Version:** 1.0  
**Last Updated:** 2026-09-03
