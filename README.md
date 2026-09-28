# HealthBoxD - Multi-Tenant Healthcare ERP

A production-ready, Spring Boot + React-based Hospital Management System with multi-tenancy, tenant isolation, and full HIPAA-aligned architecture.

## Stack

- **Backend:** Spring Boot 3.5.7, Java 21, PostgreSQL, Flyway, JWT
- **Frontend:** React 19, Vite, React Router, Axios, Tailwind CSS
- **Infrastructure:** Docker, Docker Compose, PostgreSQL 16
- **Auth:** JWT + ABAC (Attribute-Based Access Control)
- **Multi-Tenancy:** Shared database with tenant_id isolation

## Quick Start

### 1. Prerequisites
- Docker & Docker Compose
- Node.js 18+
- Java 21+ (for local development)
- PostgreSQL 16+ (if not using Docker)

### 2. Local Development

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`

#### Database (Docker)
```bash
docker-compose up -d
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

### 3. Environment Variables

#### Backend (`.env` or system env)
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/hms
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=9hklsjSTS+3sH5UY5y6zsc81TEuJTsWxZfaQOC5dz2o=
JWT_EXPIRATION=86400000
FRONTEND_URL=http://localhost:5173,http://localhost:5174
PORT=8080
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
PLATFORM_FEE_PAISE=75
SUPERADMIN_USERNAME=superadmin
SUPERADMIN_PASSWORD=SecurePassword123!
SUPERADMIN_EMAIL=admin@healthboxd.io
SUPERADMIN_PHNO=+919999999999
```

#### Frontend (`.env.local`)
```bash
VITE_API_URL=http://localhost:8080/api
VITE_AUTH_URL=http://localhost:8080/auth
VITE_APP_NAME=HealthBoxD
VITE_APP_ENVIRONMENT=development
```

## API Endpoints

### Auth (`/auth`)
- `POST /auth/login` - Login (doctor/admin/staff/patient)
- `POST /auth/register-hospital` - Register new hospital/clinic with admin
- `POST /auth/register-patient` - Register patient
- `POST /auth/otp/request` - Request OTP for verification

### Appointments (`/api/appointments`)
- `GET /api/appointments` - List appointments (tenant-scoped)
- `POST /api/appointments` - Create appointment
- `DELETE /api/appointments/{id}` - Cancel appointment

### Consultations (`/api/consultations`)
- `GET /api/consultations` - List consultations (draft + finalized)
- `POST /api/consultations` - Create consultation
- `PATCH /api/consultations/{id}/finalize` - Finalize consultation

### Charges (`/api/charges`)
- `GET /api/charges` - List charge items (catalog)
- `POST /api/charges` - Create charge item
- `PATCH /api/charges/{id}` - Edit charge item

### Prescriptions (`/api/prescriptions`)
- `GET /api/prescriptions` - List prescriptions
- `POST /api/prescriptions` - Create prescription
- `GET /api/prescriptions/{id}` - Get prescription details

### Billing (`/api/billing`)
- `GET /api/billing` - List all bills
- `GET /api/billing/{id}` - Get bill details

### Payments (`/api/payments`)
- `POST /api/payments/checkout` - Initiate payment (Razorpay)
- `POST /api/payments/verify` - Verify payment signature

### Scheduling (`/api/schedule`)
- `GET /api/schedule` - List doctor slots
- `POST /api/schedule` - Create slot
- `DELETE /api/schedule/{id}` - Block/delete slot

### Pharmacy (`/api/pharmacy`)
- `GET /api/pharmacy` - List pharmacy records
- `PATCH /api/pharmacy/{id}/dispense` - Mark as dispensed

### Dashboard (`/api/dashboard`)
- `GET /api/dashboard/summary` - Admin dashboard summary
- `GET /api/dashboard/patient-summary` - Patient dashboard

## Database Schema

### Core Entities
- **Hospital** - Tenant (clinic/practice)
- **User** - Doctor/Admin/Staff/Patient login identity
- **Patient** - Clinical patient profile (created after first paid consultation)
- **Appointment** - Doctor's appointment slots, scheduled/walk-in status
- **Consultation** - DRAFT or FINALIZED clinical encounter
- **Prescription** - Prescription linked to consultation
- **DoctorSlot** - Doctor's available time slots per clinic
- **ChargeItem** - Catalog of fees, medicines, tests, procedures
- **Billing** - Charge summary per consultation
- **Pharmacy** - Pharmacy dispensing records
- **PaymentOrder** - Razorpay payment tracking

All tables include `hospital_id` for tenant isolation.

## Architecture & Security

### Multi-Tenancy
- **Strategy:** Shared database with row-level security via `hospital_id`
- **Enforcement:** Spring Security filter sets `TenantContext` from JWT claim
- **Isolation:** Every repository query automatically filters by `hospital_id`

### Authentication & Authorization
- **Method:** JWT (RS256 asymmetric signing)
- **Claims:** `hospitalId`, `role`, `phno`, `username`
- **Roles:** ADMIN, DOCTOR, PHARMACIST, PATIENT
- **Token Lifetime:** 15 min access + 7 day refresh (rotated)

### Security Baseline
- HTTPS enforced in production
- HSTS headers enabled
- Input validation (Bean Validation @Valid)
- Parameterized queries (JPA/Hibernate)
- Rate limiting on auth endpoints
- Audit trail on charges, payments, prescriptions
- Data encryption at rest (pgcrypto or app-level AES-256)

## Deployment

### Docker Compose (Local/Staging)
```bash
docker-compose up -d
```

This starts:
- PostgreSQL 16 database
- Backend Spring Boot app
- Includes automatic Flyway migrations

### Production (Render/Railway/Vercel)

#### Backend (Render, Railway, or Heroku)
```bash
# Set environment variables on platform dashboard
git push <platform> main
# Automatically builds Dockerfile and deploys
```

#### Frontend (Vercel, Netlify)
```bash
# Connect repo, set env vars (VITE_API_URL, VITE_AUTH_URL)
vercel --prod
```

#### Database (Render, Neon, Railway)
- Provision PostgreSQL instance
- Set DATABASE_URL in backend env vars
- Flyway auto-migrates on app startup

## Testing

### Backend
```bash
cd backend
./mvnw test
```

### Frontend
```bash
cd frontend
npm test
```

## Build & Package

### Backend JAR (Production)
```bash
cd backend
./mvnw clean package -DskipTests
# JAR: target/hms-backend-*.jar
```

### Frontend Bundle (Production)
```bash
cd frontend
npm run build
# Output: dist/
```

## Development Workflow

1. **Local DB:** `docker-compose up -d` (PostgreSQL)
2. **Backend:** `./mvnw spring-boot:run` (port 8080)
3. **Frontend:** `npm run dev` (port 5173)
4. **Test:** Create hospital → register admin → login → create appointment → finalize → bill & pay

## License

Proprietary - HealthBoxD
