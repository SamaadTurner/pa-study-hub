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

# By default the app calls the API at /api/v1 on its own address, and nginx
# passes those requests on to the backend (see nginx.conf). The address is fixed
# when the bundle is built. To build for an API that lives somewhere else:
#   docker build --build-arg VITE_API_BASE_URL=https://api.example.com/api/v1 .
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

# ==========================================
# Stage 2: Serve (nginx)
# ==========================================
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# React Router needs all requests served by index.html (client-side routing)
COPY nginx.conf /etc/nginx/conf.d/app.conf
COPY nginx-security-headers.conf /etc/nginx/security-headers.conf

# Copy built assets from Stage 1
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -q --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
