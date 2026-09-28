# HealthBoxD Frontend

React-based clinic management interface for HealthBoxD Healthcare ERP.

## Setup

### Install Dependencies
```bash
npm install
```

### Environment Configuration
Create `.env.local`:
```bash
cp .env.example .env.local
```

Edit `.env.local` with your backend URLs:
```bash
VITE_API_URL=http://localhost:8080/api
VITE_AUTH_URL=http://localhost:8080/auth
VITE_APP_NAME=HealthBoxD
VITE_APP_ENVIRONMENT=development
```

### Development
```bash
npm run dev
# Runs on http://localhost:5173
```

### Production Build
```bash
npm run build
# Output in dist/
```

### Linting
```bash
npm run lint
```

## Architecture

- **Framework:** React 19 with Vite
- **Routing:** React Router v7
- **Styling:** Tailwind CSS
- **HTTP:** Axios with interceptors
- **State:** Context API (AppContext)
- **Charts:** Recharts

## File Structure

```
src/
├── components/      # Reusable UI components
├── pages/          # Page-level components
├── services/       # API integration
├── context/        # Global state (AppContext)
├── hooks/          # Custom React hooks
├── middleware/     # Axios interceptors
├── utils/          # Helper functions
├── constants/      # App constants
├── index.css       # Global styles
└── main.jsx        # Entry point
```

## Key Features

- **Multi-tenant clinic dashboard**
- **Doctor schedule management**
- **Patient appointments & consultations**
- **Prescription management**
- **Billing & charge management**
- **Payment processing (Razorpay)**
- **Pharmacy dispensing**
- **Responsive design** (desktop & mobile)

## API Integration

All API calls go through `/src/services/api.js` using Axios with automatic JWT token management.

### Token Management
- **Storage:** localStorage (authToken)
- **Header:** Authorization: Bearer {token}
- **Auto-refresh:** Interceptors handle token renewal

## Build & Deployment

### Vercel
```bash
vercel --prod
```

### Netlify
```bash
npm run build
# Deploy dist/ folder
```

### Docker
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY src ./src
COPY *.js *.json *.css *.html ./
RUN npm run build

FROM node:18-alpine
RUN npm install -g serve
WORKDIR /app
COPY --from=build /app/dist .
EXPOSE 3000
CMD ["serve", "-s", ".", "-l", "3000"]
```

## Troubleshooting

### CORS Errors
Ensure `FRONTEND_URL` in backend includes your frontend origin.

### API Connection Issues
Check that `VITE_API_URL` and `VITE_AUTH_URL` point to your backend.

### Token Expired
Refresh token should auto-renew. If not, clear localStorage and re-login.
