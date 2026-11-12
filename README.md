# PA Study Hub

> A spaced-repetition study platform for PA school students

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF.svg)](https://github.com/features/actions)

Built for Jocelyn, who starts PA school this spring. This app provides everything she needs to master the NCCPA content blueprint: spaced-repetition flashcards, timed practice exams, and streak tracking.

---

## Architecture

```mermaid
graph TB
    FE[React Frontend :3000] --> API[Spring Boot API :8080]
    API --> DB[(PostgreSQL\npa_study_hub)]

    subgraph Monolith Application
        API
        US[User Auth & Profiles]
        FS[Flashcards & SM-2]
        ES[Practice Exams]
        PS[Progress & Streaks]
    end
```

## Features

- [x] **Spaced-Repetition Flashcards** — SM-2 algorithm with Again/Hard/Good/Easy ratings
- [x] **Practice Exams** — Timed PANCE-style clinical vignette questions with detailed explanations
- [x] **Study Streak Tracking** — Daily goal progress, streak calendar, longest streak
- [x] **Performance Analytics** — Accuracy by category, trend charts, weakest area identification
- [x] **NCCPA Blueprint Categories** — Organized by Cardiology, Pharmacology, Neurology, and 14 more
- [x] **Public Deck Sharing** — Browse and clone decks created by other users

## Prerequisites

- Java 17+ (for local development without Docker)
- Node.js 20+ (for local frontend development)
- Docker Desktop with Docker Compose
- PostgreSQL 15 (if running without Docker)

## Quick Start (Docker — Recommended)

```bash
# 1. Clone the repo
git clone https://github.com/yourusername/pa-study-hub.git
cd pa-study-hub

# 2. Set up environment variables
cp .env.example .env
# Edit .env and fill in:
#   - JWT_SECRET (32+ random characters)
#   - Database password (or use the default)

# 3. Start everything
docker-compose up -d

# 4. Wait for services to be healthy (~60 seconds on first run)
docker-compose ps

# 5. Open the app
open http://localhost:3000

# Default seed user credentials:
# Email: jocelyn@pastudyhub.com
# Password: PAStudent2026!
```

## Running Locally (Without Docker)

Create the PostgreSQL database:

```sql
CREATE DATABASE pa_study_hub;
```

Start the backend:

```bash
# Backend API
./gradlew :app:bootRun

# API will be available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

Start the frontend:

```bash
# Frontend (separate terminal)
cd frontend
npm install
npm run dev
# Opens on http://localhost:3000
```

## Running Tests

```bash
# All backend tests
./gradlew test

# Same tests against a real PostgreSQL database, with the schema built by the
# Flyway migrations. The database gets wiped, so point it at an empty one.
TEST_DATABASE=postgres \nTEST_DB_URL=jdbc:postgresql://localhost:5432/pa_study_hub_test \nTEST_DB_USERNAME=postgres TEST_DB_PASSWORD=postgres \n./gradlew :app:test

# Backend tests with coverage report
./gradlew jacocoTestReport
# Open: app/build/reports/jacoco/test/html/index.html

# Frontend unit tests
cd frontend && npm test

# Frontend tests with coverage
cd frontend && npm test -- --coverage --watchAll=false

# E2E tests (app must be running)
cd frontend && npx cypress run
# or interactive mode:
cd frontend && npx cypress open
```

## Code Coverage

JaCoCo code coverage is enforced at 80% minimum line coverage. To view reports:

```bash
./gradlew jacocoTestReport
# Report at: app/build/reports/jacoco/test/html/index.html
```

## API Documentation

Swagger UI is available at:

- **All endpoints:** http://localhost:8080/swagger-ui.html

## Project Structure

```
pa-study-hub/
├── app/                      Spring Boot monolith (port 8080)
│   ├── src/main/java/com/pastudyhub/
│   │   ├── flashcard/        Decks, cards, SM-2 algorithm
│   │   ├── exam/             Practice exams, scoring
│   │   ├── progress/         Streaks, goals, analytics
│   │   ├── user/             Auth, JWT, profiles
│   │   ├── config/           OpenAPI, security configuration
│   │   └── exception/        Global exception handling
│   └── src/main/resources/
│       ├── db/migration/     Flyway SQL migrations
│       └── application.yml   App configuration
├── frontend/                 React + TypeScript + Tailwind (port 3000)
├── docker-compose.yml        Full local orchestration
└── .github/workflows/ci.yml  GitHub Actions CI/CD pipeline
```

## Design Decisions

**Why a monolith?** Simplified deployment and development. All features share a single database and codebase, making it easier to maintain consistency, enforce foreign key constraints across domains, and deploy to free-tier hosting platforms. Perfect for a portfolio project and early-stage applications.

**Why the SM-2 algorithm?** It's the scientifically-validated spaced repetition algorithm used in Anki and SuperMemo, proven over decades. The key insight is that ease factors and intervals are calculated per-card per-user, so the algorithm adapts to Jocelyn's specific strengths and weaknesses.

**Why JWT?** Stateless authentication means no server-side sessions. The backend validates the JWT once and extracts user identity for each request. This keeps the app horizontally scalable and works well with modern frontend frameworks.

**Why a single database?** Enforces referential integrity across domains with foreign keys. Simpler migrations, easier to reason about data relationships, and better transaction support for cross-domain operations.

## The SM-2 Algorithm

PA Study Hub implements the SuperMemo 2 (SM-2) algorithm for spaced repetition. See [docs/sm2-algorithm.md](docs/sm2-algorithm.md) for a full explanation.

In short: each card has an ease factor (default 2.5) and an interval (days until next review). After each review, you rate your recall quality 1-5. The algorithm adjusts the interval and ease factor so easy cards come back less often and hard cards come back more often.

## Future Improvements

- [ ] Mobile app (React Native) — study on the go between classes
- [ ] Study groups — shared decks, collaborative exam prep
- [ ] Push notification reminders at preferred study time
- [ ] PANCE score predictor based on practice exam trends and content areas
- [ ] Voice-based flashcard review for hands-free commute studying
- [ ] Offline mode with background sync

## License

[MIT](LICENSE) — © 2026 Samaad Turner
