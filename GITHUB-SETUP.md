# GitHub Setup Instructions

## Step 1: Create GitHub Repository

1. Go to **https://github.com/new**
2. Repository name: `pa-study-hub`
3. Description: `Full-stack PA school study platform with microservices architecture, spaced repetition, practice exams, and AI-powered study assistance`
4. **Public** (for portfolio visibility)
5. **DO NOT** initialize with README, .gitignore, or license (we already have these)
6. Click **"Create repository"**

## Step 2: Connect Your Local Repo to GitHub

After creating the repo, GitHub will show you commands. Use these:

```bash
# Add GitHub as remote origin
git remote add origin https://github.com/YOUR-USERNAME/pa-study-hub.git

# Push your code
git push -u origin main
```

Replace `YOUR-USERNAME` with your actual GitHub username!

## Step 3: Verify Push

After pushing, refresh your GitHub repo page. You should see:
- ✅ All your code files
- ✅ README.md displaying properly
- ✅ 96 files total
- ✅ Main branch with recent commits

## Step 4: Your Deployment Secrets

**IMPORTANT:** Your JWT secret is saved in `.env.render` (NOT committed to GitHub).

To view it:
```bash
cat .env.render
```

You'll need:
1. **JWT_SECRET**: Already generated and saved in `.env.render`
2. **ANTHROPIC_API_KEY**: Get from https://console.anthropic.com/settings/keys

## Ready for Render?

Once you've pushed to GitHub successfully:
1. ✅ Code is on GitHub
2. ✅ You have your JWT secret
3. ✅ You have your Anthropic API key

**Next:** Go to https://render.com and follow DEPLOYMENT.md
