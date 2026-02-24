#!/bin/bash

# PA Study Hub - Fly.io Deployment Script
# This script automates the deployment of all microservices to Fly.io

set -e  # Exit on error

echo "🚀 PA Study Hub - Fly.io Deployment"
echo "===================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if flyctl is installed
if ! command -v flyctl &> /dev/null; then
    echo "❌ Fly CLI not found. Installing..."
    curl -L https://fly.io/install.sh | sh
    echo "✅ Fly CLI installed. Please restart your terminal and run this script again."
    exit 1
fi

# Check if logged in
if ! flyctl auth whoami &> /dev/null; then
    echo "🔐 Please login to Fly.io..."
    flyctl auth login
fi

echo "✅ Authenticated to Fly.io"
echo ""

# Read secrets from .env.render
if [ ! -f .env.render ]; then
    echo "❌ .env.render file not found. Please create it with your secrets."
    exit 1
fi

source .env.render

echo "📦 Deploying services..."
echo ""

# Deploy User Service
echo -e "${BLUE}Deploying User Service...${NC}"
cd services/user-service
if ! flyctl status --app pa-study-user-service &> /dev/null; then
    flyctl launch --name pa-study-user-service --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set JWT_SECRET="$JWT_SECRET" --app pa-study-user-service
flyctl deploy --app pa-study-user-service
cd ../..
echo -e "${GREEN}✅ User Service deployed${NC}"
echo ""

# Deploy Flashcard Service
echo -e "${BLUE}Deploying Flashcard Service...${NC}"
cd services/flashcard-service
if ! flyctl status --app pa-study-flashcard-service &> /dev/null; then
    flyctl launch --name pa-study-flashcard-service --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set JWT_SECRET="$JWT_SECRET" --app pa-study-flashcard-service
flyctl deploy --app pa-study-flashcard-service
cd ../..
echo -e "${GREEN}✅ Flashcard Service deployed${NC}"
echo ""

# Deploy Exam Service
echo -e "${BLUE}Deploying Exam Service...${NC}"
cd services/exam-service
if ! flyctl status --app pa-study-exam-service &> /dev/null; then
    flyctl launch --name pa-study-exam-service --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set JWT_SECRET="$JWT_SECRET" --app pa-study-exam-service
flyctl deploy --app pa-study-exam-service
cd ../..
echo -e "${GREEN}✅ Exam Service deployed${NC}"
echo ""

# Deploy Progress Service
echo -e "${BLUE}Deploying Progress Service...${NC}"
cd services/study-progress-service
if ! flyctl status --app pa-study-progress-service &> /dev/null; then
    flyctl launch --name pa-study-progress-service --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set JWT_SECRET="$JWT_SECRET" --app pa-study-progress-service
flyctl deploy --app pa-study-progress-service
cd ../..
echo -e "${GREEN}✅ Progress Service deployed${NC}"
echo ""

# Deploy AI Service
echo -e "${BLUE}Deploying AI Service...${NC}"
cd services/ai-assistant-service
if ! flyctl status --app pa-study-ai-service &> /dev/null; then
    flyctl launch --name pa-study-ai-service --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set JWT_SECRET="$JWT_SECRET" ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" --app pa-study-ai-service
flyctl deploy --app pa-study-ai-service
cd ../..
echo -e "${GREEN}✅ AI Service deployed${NC}"
echo ""

# Deploy API Gateway
echo -e "${BLUE}Deploying API Gateway...${NC}"
cd services/api-gateway
if ! flyctl status --app pa-study-api-gateway &> /dev/null; then
    flyctl launch --name pa-study-api-gateway --region sjc --no-deploy --dockerfile Dockerfile
fi
flyctl secrets set \
  JWT_SECRET="$JWT_SECRET" \
  FLASHCARD_SERVICE_URL=https://pa-study-flashcard-service.fly.dev \
  EXAM_SERVICE_URL=https://pa-study-exam-service.fly.dev \
  PROGRESS_SERVICE_URL=https://pa-study-progress-service.fly.dev \
  USER_SERVICE_URL=https://pa-study-user-service.fly.dev \
  AI_SERVICE_URL=https://pa-study-ai-service.fly.dev \
  --app pa-study-api-gateway
flyctl deploy --app pa-study-api-gateway
cd ../..
echo -e "${GREEN}✅ API Gateway deployed${NC}"
echo ""

echo "🎉 All services deployed successfully!"
echo ""
echo "Your API Gateway is live at:"
echo "https://pa-study-api-gateway.fly.dev"
echo ""
echo "Next steps:"
echo "1. Deploy frontend to Vercel (see FLY-DEPLOYMENT.md)"
echo "2. Test your app!"
echo "3. Add it to your resume 🚀"
