# HealthBoxD Quick Start Guide

Get HealthBoxD running locally in 5 minutes.

## Prerequisites
- Docker & Docker Compose (recommended for DB)
- Java 21 (for Maven)
- Node.js 18+
- Git

## Local Development (Fastest)

### 1. Clone & Configure
```bash
git clone <repository-url>
cd healthboxd

# Copy environment file
cp .env.example .env

# Optional: Edit .env if you need custom SMTP/Razorpay config
# Default config runs fine for development
```

### 2. Start Database (Docker)
```bash
# Terminal 1
docker-compose up -d db
# Wait for: "database system is ready to accept connections"
```

### 3. Start Backend (Port 8080)
```bash
# Terminal 2
cd backend
./mvnw spring-boot:run
# Wait for: "Started HospitalManagementSystemApplication in X seconds"
```

### 4. Start Frontend (Port 5173)
```bash
# Terminal 3
cd frontend
npm install
npm run dev
# Open: http://localhost:5173
```

## Test the Application

### Login Flow
1. **Register Hospital (Clinic)**
   - Fill form with hospital name & code
   - Use temporary OTP: `123456` (dev mode)
   - Creates admin account automatically

2. **Login as Admin**
   - Use credentials from registration
   - Dashboard shows clinic statistics

3. **Create Appointment**
   - Go to "Appointments" → "Schedule"
   - Add doctor slot
   - Create appointment for existing/new patient

4. **Finalize & Pay**
   - Create consultation (DRAFT)
   - Add prescription & tests
   - Finalize → Auto-generate charges
   - Pay with test Razorpay credentials (dev only)

## Full Stack with Docker (Single Command)

```bash
docker-compose up --build

# This starts:
# - PostgreSQL database (port 5432)
# - Spring Boot backend (port 8080)
# - Auto-runs Flyway migrations
#
# Then manually start frontend:
cd frontend && npm install && npm run dev
```

## Project Structure

```
healthboxd/
├── backend/                # Spring Boot API
│   ├── src/main/java/     # Source code (65 Java files)
│   ├── src/main/resources/
│   │   ├── db/migration/  # Flyway migrations (7 SQL files)
│   │   └── application.properties
│   ├── pom.xml            # Maven config
│   └── Dockerfile         # Production image
│
├── frontend/              # React SPA
│   ├── src/
│   │   ├── pages/        # Page components
│   │   ├── components/   # Reusable UI
│   │   ├── services/     # API integration
│   │   ├── context/      # Global state
│   │   └── hooks/        # Custom hooks
│   ├── index.html
│   ├── package.json      # Dependencies
│   ├── vite.config.js    # Build config
│   └── Dockerfile        # Production image
│
├── docker-compose.yml    # Local dev stack
├── .env.example          # Environment template
├── README.md             # Architecture & features
├── DEPLOYMENT.md         # Production deployment guide
└── QUICKSTART.md         # This file
```

## Key Endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /auth/login` | Doctor/Admin/Patient login |
| `POST /auth/register-hospital` | Register new clinic |
| `GET /api/appointments` | List appointments |
| `POST /api/appointments` | Create appointment |
| `GET /api/dashboard/summary` | Admin dashboard stats |
| `POST /api/payments/checkout` | Initiate payment |

## Common Commands

### Backend
```bash
cd backend

# Development
./mvnw spring-boot:run

# Production build
./mvnw clean package -DskipTests
# JAR: target/hms-backend-1.0.0.jar

# Run tests
./mvnw test

# View dependencies
./mvnw dependency:tree
```

### Frontend
```bash
cd frontend

# Development server
npm run dev

# Build for production
npm run build
# Output: dist/

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Database
```bash
# Start database only
docker-compose up -d db

# Connect to database
psql -h localhost -U postgres -d hms
# Password: postgres

# View database logs
docker-compose logs db

# Stop database
docker-compose down
```

## Default Credentials (Dev Only)

If `SUPERADMIN_*` env vars are set, auto-bootstrap creates:

```
Username: superadmin
Password: SecurePassword123!
Email: admin@healthboxd.io
Phone: +919999999999
```

Change these immediately in production!

## Environment Variables (Dev Defaults)

```
DATABASE_URL=jdbc:postgresql://localhost:5432/hms
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=9hklsjSTS+3sH5UY5y6zsc81TEuJTsWxZfaQOC5dz2o=
FRONTEND_URL=http://localhost:5173,http://localhost:5174
```

For production, see `.env.example` and `DEPLOYMENT.md`.

## Troubleshooting

### Backend won't start
```bash
# Check if port 8080 is free
lsof -i :8080

# Check database connection
docker-compose logs db

# Clear Maven cache
./mvnw clean
```

### Frontend shows "API error"
```bash
# Ensure backend is running on port 8080
curl http://localhost:8080/health

# Check browser console for CORS errors
# Verify FRONTEND_URL env var if needed
```

### Database connection refused
```bash
# Start database
docker-compose up -d db

# Wait for database to be ready (health check)
docker-compose exec db pg_isready -U postgres
```

### Port conflicts
```bash
# Change ports in docker-compose.yml or .env
# Example:
#   Backend: PORT=8081
#   Database: 5433:5432
#   Frontend: vite config --port 5174
```

## Next Steps

1. **Read Architecture:** See `README.md` for system design
2. **Deploy:** Follow `DEPLOYMENT.md` for production setup
3. **Configure:** Set SMTP, Razorpay credentials in `.env`
4. **Test:** Create hospital → doctor → appointment → payment flow
5. **Customize:** Modify UI colors, add features in React/Java

## Getting Help

- **Backend Issues:** Check `backend/target/spring.log`
- **Database:** `docker-compose logs db`
- **Frontend:** Browser DevTools → Console tab
- **API:** `curl http://localhost:8080/api/endpoint`

## Health Checks

```bash
# Backend running?
curl http://localhost:8080/health

# Database connected?
curl http://localhost:8080/health | grep db

# Frontend accessible?
curl http://localhost:5173

# Can reach backend from frontend?
# Browser Console: fetch('http://localhost:8080/api/dashboard/summary')
```

---

**Ready to go!** 🚀

For production deployment, see `DEPLOYMENT.md`.
