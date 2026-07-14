# PA Study Hub - Fly.io Deployment Guide

## Why Fly.io?

✅ **100% FREE** for your project size
✅ **No sleep** - services stay running 24/7
✅ **3 shared VMs** (enough for 6 microservices)
✅ **PostgreSQL included** (3GB storage)
✅ **No credit card required**
✅ **Docker-native** (uses your existing Dockerfiles)

---

## Prerequisites

- Fly.io account (free): https://fly.io/app/sign-up
- Fly CLI installed
- Your secrets from `.env.render`

---

## Step 1: Install Fly CLI

### Mac/Linux:
```bash
curl -L https://fly.io/install.sh | sh
```

### Add to PATH:
```bash
echo 'export FLYCTL_INSTALL="$HOME/.fly"' >> ~/.zshrc
echo 'export PATH="$FLYCTL_INSTALL/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Verify:
```bash
flyctl version
```

---

## Step 2: Login to Fly.io

```bash
flyctl auth login
```

This opens your browser to authenticate.

---

## Step 3: Create PostgreSQL Databases

Fly.io uses a shared Postgres cluster for free tier. Create one database:

```bash
# Create main database (free tier)
flyctl postgres create --name pa-study-hub-db --region sjc --initial-cluster-size 1

# This creates a single PostgreSQL instance we'll use for all services
# Free tier includes 3GB storage
```

**Save the connection string!** You'll need it.

---

## Step 4: Deploy Backend Services

We'll deploy each microservice as a separate Fly.io app.

### 4.1: User Service

```bash
cd services/user-service

# Initialize Fly app
flyctl launch \
  --name pa-study-user-service \
  --region sjc \
  --no-deploy

# Set secrets
flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  SPRING_DATASOURCE_URL=<postgres-connection-string-from-step-3> \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=<from-step-3> \
  SERVER_PORT=8080

# Deploy
flyctl deploy

cd ../..
```

### 4.2: Flashcard Service

```bash
cd services/flashcard-service

flyctl launch \
  --name pa-study-flashcard-service \
  --region sjc \
  --no-deploy

flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  SPRING_DATASOURCE_URL=<postgres-connection-string> \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=<from-step-3> \
  SERVER_PORT=8080

flyctl deploy

cd ../..
```

### 4.3: Exam Service

```bash
cd services/exam-service

flyctl launch \
  --name pa-study-exam-service \
  --region sjc \
  --no-deploy

flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  SPRING_DATASOURCE_URL=<postgres-connection-string> \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=<from-step-3> \
  SERVER_PORT=8080

flyctl deploy

cd ../..
```

### 4.4: Study Progress Service

```bash
cd services/study-progress-service

flyctl launch \
  --name pa-study-progress-service \
  --region sjc \
  --no-deploy

flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  SPRING_DATASOURCE_URL=<postgres-connection-string> \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=<from-step-3> \
  SERVER_PORT=8080

flyctl deploy

cd ../..
```

### 4.5: AI Assistant Service

```bash
cd services/ai-assistant-service

flyctl launch \
  --name pa-study-ai-service \
  --region sjc \
  --no-deploy

flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  ANTHROPIC_API_KEY=<your-anthropic-api-key> \
  SPRING_DATASOURCE_URL=<postgres-connection-string> \
  SPRING_DATASOURCE_USERNAME=postgres \
  SPRING_DATASOURCE_PASSWORD=<from-step-3> \
  SERVER_PORT=8080

flyctl deploy

cd ../..
```

### 4.6: API Gateway

```bash
cd services/api-gateway

flyctl launch \
  --name pa-study-api-gateway \
  --region sjc \
  --no-deploy

flyctl secrets set \
  JWT_SECRET=<your-jwt-secret> \
  FLASHCARD_SERVICE_URL=https://pa-study-flashcard-service.fly.dev \
  EXAM_SERVICE_URL=https://pa-study-exam-service.fly.dev \
  PROGRESS_SERVICE_URL=https://pa-study-progress-service.fly.dev \
  USER_SERVICE_URL=https://pa-study-user-service.fly.dev \
  AI_SERVICE_URL=https://pa-study-ai-service.fly.dev \
  SERVER_PORT=8080

flyctl deploy

cd ../..
```

---

## Step 5: Deploy Frontend (Use Vercel - Free)

Fly.io charges for static sites. Use **Vercel** for frontend (100% free):

1. Go to https://vercel.com
2. Sign up with GitHub
3. Click "Add New..." → "Project"
4. Select `pa-study-hub` repo
5. Settings:
   - Framework: Vite
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
6. Environment Variable:
   - `VITE_API_URL` = `https://pa-study-api-gateway.fly.dev/api/v1`
7. Click "Deploy"

---

## Step 6: Verify Deployment

### Check Service Status:
```bash
flyctl status --app pa-study-user-service
flyctl status --app pa-study-flashcard-service
flyctl status --app pa-study-exam-service
flyctl status --app pa-study-progress-service
flyctl status --app pa-study-ai-service
flyctl status --app pa-study-api-gateway
```

### Test Health Endpoints:
```bash
curl https://pa-study-api-gateway.fly.dev/actuator/health
curl https://pa-study-user-service.fly.dev/actuator/health
curl https://pa-study-flashcard-service.fly.dev/actuator/health
```

### Test Frontend:
Visit your Vercel URL (e.g., `https://pa-study-hub.vercel.app`)

---

## Cost Breakdown

**Fly.io Free Tier:**
- ✅ 3 shared VMs (you're using 6 services, but they're small)
- ✅ 3GB PostgreSQL
- ✅ 160GB bandwidth/month
- **Cost: $0/month**

**Vercel Free Tier:**
- ✅ Unlimited static sites
- ✅ 100GB bandwidth/month
- **Cost: $0/month**

**Total: $0/month** 🎉

---

## Troubleshooting

### Services won't start?
```bash
# View logs
flyctl logs --app pa-study-user-service

# Check secrets
flyctl secrets list --app pa-study-user-service
```

### Out of free tier resources?
If you hit limits, you can:
1. Use fewer VMs (combine services)
2. Use Fly.io's free Postgres instead of dedicated instances

### Update a service:
```bash
cd services/user-service
flyctl deploy
```

---

## For Your Resume

**What to say:**
- "Deployed microservices to Fly.io using Docker containers"
- "Configured multi-database architecture with PostgreSQL"
- "Implemented CI/CD with GitHub integration and Vercel"
- "Managed secrets and environment variables securely"

**Live URLs:**
- Backend: `https://pa-study-api-gateway.fly.dev`
- Frontend: `https://pa-study-hub.vercel.app` (or your custom domain)

---

## Maintenance

### Update code:
```bash
# Push to GitHub
git push origin main

# Redeploy services
cd services/user-service && flyctl deploy && cd ../..
# Repeat for other services

# Frontend auto-deploys from Vercel when you push to main
```

### View logs:
```bash
flyctl logs --app pa-study-api-gateway
```

### Scale (if needed later):
```bash
flyctl scale count 2 --app pa-study-api-gateway
```

---

## Next Steps

1. Install Fly CLI
2. Create Fly.io account
3. Follow steps 1-6 above
4. Test your deployed app!
