# HealthBoxD Deployment Guide

Complete guide to deploying HealthBoxD to production.

## Table of Contents
1. [Local Development](#local-development)
2. [Docker Deployment](#docker-deployment)
3. [Render.com Deployment](#rendercom-deployment)
4. [Railway.app Deployment](#railwayapp-deployment)
5. [Production Checklist](#production-checklist)

---

## Local Development

### Prerequisites
- Docker & Docker Compose
- Node.js 18+
- Java 21+ (for direct Maven runs)
- PostgreSQL 16+ (if not using Docker)

### Quick Start
```bash
# 1. Clone repo
git clone <repo-url>
cd healthboxd

# 2. Set up environment
cp .env.example .env
# Edit .env with your values (SMTP, Razorpay, etc.)

# 3. Start database
docker-compose up -d db

# 4. Backend (terminal 1)
cd backend
./mvnw spring-boot:run

# 5. Frontend (terminal 2)
cd frontend
npm install
npm run dev
```

**URLs:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Database: localhost:5432

---

## Docker Deployment

### Build & Run Locally
```bash
# Full stack (DB + Backend + Frontend)
docker-compose up --build

# Backend only
docker build -t healthboxd-backend ./backend
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/hms" \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
   healthboxd-backend
```

### Docker Image for Registry
```bash
# Build backend image
docker build -t your-registry/healthboxd-backend:1.0.0 ./backend
docker push your-registry/healthboxd-backend:1.0.0

# Build frontend image
cd frontend
npm run build
docker build -t your-registry/healthboxd-frontend:1.0.0 .
docker push your-registry/healthboxd-frontend:1.0.0
```

---

## Render.com Deployment

### Backend (Render Web Service)

1. **Connect Repository**
   - Go to https://dashboard.render.com
   - Click "Create +" → "Web Service"
   - Connect your GitHub repo

2. **Configure Service**
   - **Name:** healthboxd-backend
   - **Environment:** Docker
   - **Region:** Choose nearest
   - **Plan:** Free or Paid (Starter)

3. **Set Environment Variables**
   - Click "Advanced" → "Environment"
   - Add all variables from `.env.example`:
     ```
     DATABASE_URL=postgresql://user:pass@hostname:5432/dbname
     DATABASE_USERNAME=postgres
     DATABASE_PASSWORD=your-secure-password
     JWT_SECRET=your-secret-key
     FRONTEND_URL=https://your-frontend.vercel.app,https://your-frontend.netlify.app
     SMTP_USERNAME=your-email@gmail.com
     SMTP_PASSWORD=your-app-password
     RAZORPAY_KEY_ID=...
     RAZORPAY_KEY_SECRET=...
     RAZORPAY_WEBHOOK_SECRET=...
     SUPERADMIN_USERNAME=superadmin
     SUPERADMIN_PASSWORD=strong-password
     SUPERADMIN_EMAIL=admin@example.com
     SUPERADMIN_PHNO=+919999999999
     ```

4. **Database (Render PostgreSQL)**
   - Create a new PostgreSQL database on Render
   - Use the connection string as `DATABASE_URL`
   - Ensure Flyway migrations run automatically on app start

5. **Deploy**
   - Push to `main` branch
   - Render auto-deploys
   - Check logs: Dashboard → Your Service → Logs

### Frontend (Vercel/Netlify)

**Vercel:**
```bash
npm install -g vercel
cd frontend
vercel --prod
```

Set environment variables in Vercel dashboard:
- `VITE_API_URL=https://healthboxd-backend.onrender.com/api`
- `VITE_AUTH_URL=https://healthboxd-backend.onrender.com/auth`

---

## Railway.app Deployment

### Backend Setup
1. Go to https://railway.app
2. Click "Create Project"
3. Select "Deploy from GitHub"
4. Choose your repo and connect

### Environment Variables
```bash
# In Railway dashboard, go to Variables
DATABASE_URL=postgresql://...
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=...
JWT_SECRET=your-key
FRONTEND_URL=https://your-frontend.vercel.app
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-password
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
```

### Dockerfile for Railway
Create `backend/Dockerfile`:
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

---

## Production Checklist

### Security
- [ ] Change all default passwords
- [ ] Generate strong JWT_SECRET (use: `openssl rand -base64 32`)
- [ ] Enable HTTPS everywhere (automatic on Render/Railway/Vercel)
- [ ] Set HSTS headers in SecurityConfig
- [ ] Enable CORS only for your frontend domain
- [ ] Use environment variables for all secrets (never commit .env)
- [ ] Enable database connection SSL/TLS
- [ ] Set rate limiting on auth endpoints
- [ ] Rotate JWT secrets periodically

### Database
- [ ] Use managed PostgreSQL (Render, Railway, Neon, AWS RDS)
- [ ] Enable automated backups
- [ ] Set up monitoring/alerts for disk space
- [ ] Create read replicas if needed
- [ ] Run Flyway migrations before app start
- [ ] Test backup restore procedures

### Monitoring
- [ ] Set up application logs (Render/Railway dashboards)
- [ ] Enable error tracking (Sentry, DataDog, or cloud native)
- [ ] Monitor database performance
- [ ] Set up uptime alerts
- [ ] Track API latency and error rates

### Frontend
- [ ] Build production bundle (`npm run build`)
- [ ] Enable gzip compression
- [ ] Set Cache-Control headers
- [ ] Use CDN for static assets (Vercel does this)
- [ ] Enable CORS on backend for frontend domain

### Deployment
- [ ] Test staging deployment first
- [ ] Use blue-green deployments
- [ ] Automate database migrations (Flyway)
- [ ] Have rollback plan for critical changes
- [ ] Document deployment procedures
- [ ] Set up monitoring after deployment

### Performance
- [ ] Enable database connection pooling (HikariCP)
- [ ] Use PostgreSQL indexes on common queries
- [ ] Cache charge master items if needed
- [ ] Monitor and optimize N+1 queries
- [ ] Use pagination for large result sets

---

## Environment Variable Reference

### Critical
| Variable | Purpose | Example |
|----------|---------|---------|
| `DATABASE_URL` | PostgreSQL connection | `postgresql://user:pass@host:5432/hms` |
| `JWT_SECRET` | Token signing key | `generated-base64-string` |
| `FRONTEND_URL` | CORS origins | `https://app.example.com` |

### Email (OTP)
| Variable | Purpose | Example |
|----------|---------|---------|
| `SMTP_HOST` | Mail server | `smtp.gmail.com` |
| `SMTP_USERNAME` | Sender email | `admin@example.com` |
| `SMTP_PASSWORD` | App password (Gmail) | `xxx-xxx-xxx-xxx` |

### Payments (Razorpay)
| Variable | Purpose | Example |
|----------|---------|---------|
| `RAZORPAY_KEY_ID` | Public key | `rzp_live_XXXXX` |
| `RAZORPAY_KEY_SECRET` | Secret key | `XXXXXXXXXXXXX` |

### Admin Bootstrap
| Variable | Purpose | Default |
|----------|---------|---------|
| `SUPERADMIN_USERNAME` | Initial admin username | `superadmin` |
| `SUPERADMIN_PASSWORD` | Initial admin password | `SuperPassword123!` |
| `SUPERADMIN_EMAIL` | Initial admin email | `admin@healthboxd.io` |

---

## Troubleshooting

### Flyway Migrations Fail
```bash
# Manually run migrations
cd backend
./mvnw flyway:migrate \
  -Dflyway.url="jdbc:postgresql://localhost:5432/hms" \
  -Dflyway.user=postgres \
  -Dflyway.password=postgres
```

### Database Connection Issues
- Check `DATABASE_URL` format
- Verify credentials
- Ensure database is running
- Check firewall/security groups
- Test connection: `psql <DATABASE_URL>`

### CORS Errors on Frontend
- Ensure backend `FRONTEND_URL` includes your frontend origin
- Check exact origin (protocol + domain + port)
- Restart backend after changing CORS settings

### JWT Token Errors
- Ensure `JWT_SECRET` is same across all instances
- Check token expiration settings
- Verify Authorization header format: `Bearer <token>`

---

## Scaling

### Horizontal Scaling
1. Backend is stateless (JWT-based)
2. Multiple instances can run behind load balancer
3. Share database connection string across instances
4. Ensure scheduled jobs (draft expiry) use locking (ShedLock)

### Vertical Scaling
- Increase JVM heap: `-Xmx1g -Xms512m`
- Increase database pool size: `spring.datasource.hikari.maximum-pool-size=20`
- Enable query caching for charge master

---

## Support & Documentation

- Backend API Docs: `/swagger-ui.html` (if Springdoc added)
- Frontend Code: `/frontend/README.md`
- Architecture: Root `/README.md`
- Schema: `/backend/src/main/resources/db/migration/`

---

**Last Updated:** 2026-09-03
