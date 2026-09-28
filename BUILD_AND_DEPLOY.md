# Build & Deploy HealthBoxD

Step-by-step instructions for building and deploying HealthBoxD locally and to production.

## Prerequisites

### Local Development
```bash
# Check Java
java -version
# Expected: Java 21 or higher

# Check Node.js  
node -v npm -v
# Expected: Node 18+, npm 9+

# Check Docker
docker --version docker-compose --version
# Expected: Docker 20.10+, Docker Compose 2.0+
```

## Local Build

### 1. Backend Build

```bash
cd backend

# Clean build
./mvnw clean package -DskipTests

# Output: target/hms-backend-1.0.0.jar (approximately 100MB)

# Run tests (optional)
./mvnw test

# Run locally
./mvnw spring-boot:run
# Backend available at http://localhost:8080
```

### 2. Frontend Build

```bash
cd frontend

# Install dependencies
npm ci  # Uses package-lock.json for reproducible builds

# Build for production
npm run build
# Output: dist/ (contains optimized assets)

# Preview production build
npm run preview
# Available at http://localhost:4173
```

### 3. Database Setup

```bash
# Start PostgreSQL (Docker)
docker-compose up -d db

# Verify database is ready
docker-compose logs db | grep "database system is ready"

# Connect to database (optional)
psql -h localhost -U postgres -d hms
# Password: postgres
```

### 4. Run Full Stack

```bash
# Terminal 1: Database
docker-compose up -d db

# Terminal 2: Backend
cd backend && ./mvnw spring-boot:run

# Terminal 3: Frontend
cd frontend && npm run dev

# Verify
# Backend: curl http://localhost:8080/health
# Frontend: http://localhost:5173
# Database: psql -h localhost -U postgres -d hms
```

## Docker Build

### Build Backend Image

```bash
cd backend

# Build
docker build -t healthboxd-backend:latest .

# Build with version tag
docker build -t healthboxd-backend:1.0.0 .

# Verify
docker image ls | grep healthboxd-backend
```

### Build Frontend Image

```bash
cd frontend

# Build
docker build -t healthboxd-frontend:latest .

# Build with version tag  
docker build -t healthboxd-frontend:1.0.0 .

# Verify
docker image ls | grep healthboxd-frontend
```

### Run Docker Compose Stack

```bash
# From root directory
docker-compose up --build

# This starts:
# - PostgreSQL (port 5432)
# - Spring Boot Backend (port 8080)
# - Automatically runs Flyway migrations

# Check status
docker-compose ps
docker-compose logs -f backend

# Stop stack
docker-compose down
```

## Production Build

### Backend (JAR)

```bash
cd backend

# Production build (no tests, optimized)
./mvnw clean package -DskipTests -Dspring.profiles.active=prod

# Verify JAR
ls -lh target/hms-backend-1.0.0.jar

# Run JAR with production settings
java -Xmx1g -Xms256m \
  -Dspring.profiles.active=prod \
  -DDATABASE_URL="postgresql://prod-db:5432/hms" \
  -DDATABASE_USERNAME="produser" \
  -DDATABASE_PASSWORD="strong-password" \
  -DJWT_SECRET="$(openssl rand -base64 32)" \
  -jar target/hms-backend-1.0.0.jar
```

### Frontend (Static Bundle)

```bash
cd frontend

# Build production bundle
VITE_API_URL="https://api.healthboxd.io/api" \
VITE_AUTH_URL="https://api.healthboxd.io/auth" \
npm run build

# Verify build
ls -lh dist/
# Should contain: index.html, assets/*, etc.

# Size check (gzipped)
gzip -c dist/index.html | wc -c
# Should be < 50KB
```

## Deploy to Render.com

### Backend Deployment

1. **Create Render Account**
   - https://dashboard.render.com
   - Connect GitHub repository

2. **Create Web Service**
   - Name: `healthboxd-backend`
   - Environment: Docker
   - Build Command: (use Dockerfile)
   - Start Command: (automatic from Dockerfile)

3. **Set Environment Variables**
   ```
   DATABASE_URL=postgresql://user:pass@hostname:5432/dbname
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=your-secure-password
   JWT_SECRET=<generated-secret>
   FRONTEND_URL=https://your-frontend.vercel.app
   SMTP_USERNAME=your-email@gmail.com
   SMTP_PASSWORD=your-app-password
   RAZORPAY_KEY_ID=rzp_live_...
   RAZORPAY_KEY_SECRET=...
   RAZORPAY_WEBHOOK_SECRET=...
   SUPERADMIN_USERNAME=superadmin
   SUPERADMIN_PASSWORD=GenerateSecurePassword!
   SUPERADMIN_EMAIL=admin@healthboxd.io
   SUPERADMIN_PHNO=+919999999999
   ```

4. **Create PostgreSQL Database**
   - Render Dashboard → PostgreSQL
   - Create new database
   - Copy connection string to DATABASE_URL
   - Flyway auto-runs on first deploy

5. **Deploy**
   - Push to main branch
   - Render auto-deploys
   - Check logs: Dashboard → Logs tab
   - Verify: `curl https://your-backend.onrender.com/health`

### Frontend Deployment (Vercel)

```bash
cd frontend

# Install Vercel CLI
npm i -g vercel

# Deploy
vercel --prod

# Set environment variables in Vercel dashboard
VITE_API_URL=https://your-backend.onrender.com/api
VITE_AUTH_URL=https://your-backend.onrender.com/auth

# Verify
curl https://your-frontend.vercel.app
```

### Frontend Deployment (Netlify)

```bash
cd frontend

# Build locally
npm run build

# Deploy
netlify deploy --prod --dir=dist

# Or connect GitHub for auto-deploy
# Settings → Build & Deploy → Deploy method → GitHub
```

## Database Migrations

### Auto-Migration (Recommended)

Flyway runs automatically on application startup:

```java
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

**First deployment:**
1. Render/Railway detects Dockerfile
2. Builds image with Flyway dependency
3. On startup, Spring Boot triggers Flyway
4. Migrations V1-V7 run sequentially
5. App becomes ready when migrations complete

### Manual Migration (if needed)

```bash
cd backend

./mvnw flyway:migrate \
  -Dflyway.url="jdbc:postgresql://prod-db:5432/hms" \
  -Dflyway.user=postgres \
  -Dflyway.password=secure-password \
  -Dflyway.locations="filesystem:src/main/resources/db/migration"

# Verify
./mvnw flyway:info
```

## CI/CD Pipeline (GitHub Actions)

The `.github/workflows/build-deploy.yml` automatically:

1. **On Push to Main:**
   - Builds backend JAR
   - Runs tests
   - Builds Docker image
   - Pushes to registry
   - Triggers Render deployment

2. **On Push to Develop:**
   - Builds & tests only
   - No Docker push or deploy

### Secrets Required (GitHub Settings)

```
RENDER_DEPLOY_HOOK = https://api.render.com/deploy/srv-xxxxx?key=xxxxx
DOCKER_REGISTRY_TOKEN = (if using private registry)
```

## Performance Tuning for Production

### Backend JVM Settings

```bash
# In Dockerfile or launch script
java -Xms512m \
     -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UnlockExperimentalVMOptions \
     -XX:G1NewCollectionPercentThreshold=30 \
     -Dspring.profiles.active=production \
     -jar app.jar
```

### Database Connection Tuning

```properties
# application-prod.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
```

### Frontend CDN Caching

```
# Vercel/Netlify automatically cache:
# - Static assets (JS, CSS, images): 1 year
# - index.html: 0 (always fresh)
# - API calls: not cached (via Axios)
```

## Rollback Procedures

### Backend (Render)

```bash
# Option 1: Redeploy previous Git commit
git revert <commit-hash>
git push origin main
# Render auto-redeploys

# Option 2: Manual rollback in Render Dashboard
Dashboard → Your Service → Deploys tab → Click previous version
```

### Frontend (Vercel/Netlify)

```bash
# Netlify
netlify deploy --prod --dir=dist --alias=rollback

# Vercel Dashboard
Deployments → Click previous version → Promote to Production
```

### Database (PostgreSQL)

```bash
# Render/Railway manage backups automatically
# Restore via dashboard: Backups tab → Restore

# Or point-in-time recovery (if supported)
# Contact support for assistance
```

## Health Checks & Monitoring

### Backend Health Endpoint

```bash
# Should return 200 with status UP
curl https://your-backend.onrender.com/health

# Response:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "livenessState": { "status": "UP" }
  }
}
```

### Monitor Logs

```bash
# Render
Render Dashboard → Your Service → Logs tab

# Verify no errors
- grep "ERROR" logs
- grep "Exception" logs
```

### Test API

```bash
# Login
curl -X POST https://your-backend.onrender.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Dashboard
curl https://your-backend.onrender.com/api/dashboard/summary \
  -H "Authorization: Bearer <token>"
```

## Troubleshooting

### Build Fails

```bash
# Backend
cd backend
./mvnw clean compile  # Checks compilation
./mvnw test           # Runs tests

# Frontend
cd frontend
npm ci                # Clean install
npm run build         # Debug build errors
```

### Database Won't Start

```bash
# Check Docker
docker-compose ps
docker-compose logs db

# Restart
docker-compose down
docker-compose up -d db
```

### Deploy Stuck on Render

```bash
# Check logs
Render Dashboard → Logs tab

# Common issues:
# - Database connection string incorrect
# - Environment variables missing
# - Docker build timeout (increase to 1 hour in settings)
# - Port already in use (Render manages this)
```

### Token Errors on Frontend

```bash
# Clear localStorage
localStorage.clear()

# Refresh page
# Login again

# Check token format
# DevTools → Console → localStorage.getItem('authToken')
# Should be: eyJhbGc... (base64)
```

## Deployment Checklist

Before deploying to production:

- [ ] All env vars configured (DATABASE_URL, JWT_SECRET, etc.)
- [ ] Super admin password changed (not default)
- [ ] Database backups enabled
- [ ] HTTPS enforced (automatic on Render/Vercel)
- [ ] Frontend CORS includes exact origin
- [ ] Error logging configured (Sentry/DataDog)
- [ ] Uptime monitoring configured (UptimeRobot)
- [ ] Team knows how to rollback
- [ ] Incident response plan documented

See `DEPLOYMENT_CHECKLIST.md` for complete checklist.

---

**Happy deploying!** 🚀

Questions? See:
- QUICKSTART.md (local dev)
- DEPLOYMENT.md (production guide)
- ARCHITECTURE.md (system design)
