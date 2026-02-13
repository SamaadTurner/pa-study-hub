# PA Study Hub — Frontend Dockerfile
# Multi-stage: Node build → nginx serve
# Final image is ~25 MB (vs ~1 GB with Node included)

# ==========================================
# Stage 1: Build (Node 20 LTS)
# ==========================================
FROM node:20-alpine AS build

WORKDIR /app

# Install dependencies first (layer-cached unless package files change)
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build
COPY . .

# VITE_API_URL is baked into the bundle at build time.
# Pass it as a build arg: docker build --build-arg VITE_API_URL=https://api.example.com .
ARG VITE_API_URL=/api/v1
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# ==========================================
# Stage 2: Serve (nginx)
# ==========================================
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# React Router needs all requests served by index.html (client-side routing)
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy built assets from Stage 1
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
