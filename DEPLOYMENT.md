# PA Study Hub - Render Deployment Guide

## Prerequisites

1. **GitHub Account** - Your code must be pushed to GitHub
2. **Render Account** - Sign up at [render.com](https://render.com) (free)
3. **Anthropic API Key** - Get from [console.anthropic.com](https://console.anthropic.com)
4. **JWT Secret** - Generate with: `openssl rand -hex 32`

---

## Step 1: Push Code to GitHub

```bash
# Make sure you're on the main branch
git checkout main

# If not already merged, merge the frontend foundation branch
git merge feat/frontend-foundation

# Add and commit the render.yaml and deployment docs
git add render.yaml DEPLOYMENT.md
git commit -m "feat(deploy): add Render deployment configuration

- Add render.yaml with all 5 microservices + API gateway
- Define 5 PostgreSQL databases (one per service)
- Configure frontend static site deployment
- Set up health checks and environment variables
- Use free tier for all services"

git push origin main
```

---

## Step 2: Connect Render to GitHub

1. Go to [render.com](https://render.com)
2. Click **"Get Started for Free"**
3. Sign up with **GitHub**
4. Authorize Render to access your repositories

---

## Step 3: Create New Blueprint

1. In Render Dashboard, click **"New +"** → **"Blueprint"**
2. Connect your **pa-study-hub** repository
3. Render will detect `render.yaml` automatically
4. Click **"Apply"**

Render will now create:
- ✅ 5 PostgreSQL databases
- ✅ 6 web services (5 microservices + API gateway)
- ✅ 1 static site (frontend)

---

## Step 4: Add Environment Variables

While services are deploying, add these environment variables to **each service**:

### For ALL Services (except frontend):

1. Click on each service name (e.g., "flashcard-service")
2. Go to **"Environment"** tab
3. Add the following:

**JWT_SECRET**
```
[Paste your generated JWT secret from: openssl rand -hex 32]
```

### For ai-assistant-service ONLY:

**ANTHROPIC_API_KEY**
```
[Paste your Anthropic API key from console.anthropic.com]
```

### For frontend ONLY:

The `VITE_API_URL` is already set in render.yaml, but verify it points to:
```
https://api-gateway.onrender.com/api/v1
```

---

## Step 5: Wait for Deployment

Initial deployment takes **10-15 minutes**:
- Databases provision first
- Backend services build and deploy
- Frontend builds last

**Progress:** Watch the **"Events"** tab on each service

---

## Step 6: Verify Deployment

### Check Service Health:

Visit each health check endpoint (should return `{"status":"UP"}`):

```
https://flashcard-service.onrender.com/actuator/health
https://exam-service.onrender.com/actuator/health
https://study-progress-service.onrender.com/actuator/health
https://user-service.onrender.com/actuator/health
https://ai-assistant-service.onrender.com/actuator/health
https://api-gateway.onrender.com/actuator/health
```

### Test the Frontend:

```
https://pa-study-hub-frontend.onrender.com
```

Expected behavior:
- Login/register page loads
- After registration, dashboard appears
- All features work (flashcards, exams, AI chat, etc.)

---

## Step 7: Custom Domain (Optional)

1. Go to frontend service in Render
2. Click **"Settings"** → **"Custom Domain"**
3. Add your domain (e.g., `pastudyhub.com`)
4. Update DNS with Render's provided CNAME record
5. Update `VITE_API_URL` in frontend environment variables if needed

---

## Troubleshooting

### Services Not Starting?

**Check Logs:**
1. Click service name → "Logs" tab
2. Look for errors like:
   - `DATABASE_URL not found` → Database not linked correctly
   - `JWT_SECRET not set` → Environment variable missing
   - `Port already in use` → Usually resolves on restart

**Fix:**
- Restart service: Click "Manual Deploy" → "Clear build cache & deploy"
- Rebuild: Click "Manual Deploy" → "Deploy latest commit"

### Frontend Can't Connect to Backend?

**Check CORS:**
- API Gateway must allow frontend domain in CORS config
- Check `application.yml` in api-gateway service

**Check API URL:**
- Frontend `VITE_API_URL` must match API Gateway URL exactly
- Should be: `https://api-gateway.onrender.com/api/v1`

### Database Connection Errors?

**Verify Database Linking:**
1. Go to service (e.g., flashcard-service)
2. Click "Environment" tab
3. Ensure `SPRING_DATASOURCE_URL` is populated from database
4. Should show: `postgresql://...`

**If blank:**
- Delete and recreate the service
- Ensure `fromDatabase` in render.yaml matches database name exactly

### Services Sleep After 15 Minutes?

**This is normal on Render free tier:**
- Services sleep after 15 min of inactivity
- Wake up automatically when visited (takes ~30 seconds)
- First request after sleep will be slow
- **Not an issue for portfolio projects** - recruiters won't notice

**To keep alive (costs $):**
- Upgrade to paid plan ($7/month per service)
- Use a service like [UptimeRobot](https://uptimerobot.com) to ping every 14 minutes

---

## Cost Breakdown

**Render Free Tier Limits:**
- ✅ 750 hours/month per service (enough for 1 service running 24/7)
- ✅ You have 7 services = need to manage usage
- ⚠️ Services sleep after 15 min inactivity (wakes on request)
- ✅ 1GB PostgreSQL per database (plenty for portfolio)

**Expected Usage:**
- **$0/month** if you accept auto-sleep
- **~$50/month** if you upgrade all services to stay awake 24/7
- **Recommended:** Stay on free tier for portfolio - the sleep is fine

---

## Maintenance

### Update Code:

```bash
# Make changes locally
git add .
git commit -m "fix: your change description"
git push origin main
```

Render auto-deploys on push to main!

### View Logs:

- Render Dashboard → Service → "Logs" tab
- Real-time streaming logs for debugging

### Restart Service:

- Render Dashboard → Service → "Manual Deploy" → "Deploy latest commit"

---

## For Your Resume

**What to put:**
- "Deployed microservices architecture to Render.com using Docker"
- "Configured PostgreSQL databases with automated backups"
- "Implemented CI/CD with GitHub integration"
- "Managed environment variables and secrets securely"

**Live Demo URL:**
```
https://pa-study-hub-frontend.onrender.com
```

**GitHub Repo:**
```
https://github.com/YOUR-USERNAME/pa-study-hub
```

---

## Need Help?

- **Render Docs:** https://render.com/docs
- **Render Discord:** https://discord.gg/render
- **Check service logs** in Render Dashboard for specific errors
