# Contributing to PA Study Hub

Thank you for your interest in contributing to PA Study Hub!

## Development Workflow

We follow a feature-branch workflow with conventional commits.

### Branch Naming

```
feat/<feature-name>       # New features
fix/<bug-description>     # Bug fixes
chore/<task>              # Maintenance (deps, config, build)
docs/<topic>              # Documentation
test/<what-is-tested>     # Adding or fixing tests
refactor/<what>           # Refactoring without behavior change
```

### Commit Messages (Conventional Commits)

All commits must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types:** `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `style`, `perf`, `ci`

**Scopes:** `user`, `flashcard`, `exam`, `progress`, `ai`, `gateway`, `frontend`, `infra`, `ci`

**Examples:**
```
feat(flashcard): add SM-2 spaced repetition engine
test(user): add PasswordPolicy unit tests
fix(exam): handle null selectedOptionId on skip
docs: update architecture diagram with AI service
chore(ci): add JaCoCo coverage threshold check
```

### Pull Request Process

1. Fork the repo and create your branch from `main`
2. Write code and tests (80%+ coverage required)
3. Ensure all tests pass: `./gradlew check` (backend) and `npm test` (frontend)
4. Ensure linting passes: `npm run lint`
5. Update documentation if needed
6. Open a PR using the PR template
7. Request a review
8. Merge only after CI passes and review approved

### Code Standards

**Backend (Java):**
- All public classes and methods must have Javadoc
- All DTOs must use Jakarta Bean Validation annotations
- All endpoints must be documented with SpringDoc OpenAPI annotations
- No raw SQL queries — use Spring Data JPA or parameterized `@Query`
- All custom exceptions extend `StudyHubException` and include HTTP status
- Follow the existing package structure per service

**Frontend (TypeScript):**
- Strict TypeScript — no `any` types
- All components are functional with hooks
- All forms use controlled components with validation
- Tailwind classes only — no inline styles
- All API calls go through the service layer (`src/services/`)

### Testing Requirements

- **Backend:** JUnit 5 + Mockito unit tests, MockMvc integration tests, Testcontainers for DB tests
- **Frontend:** Jest unit tests, React Testing Library for components
- **E2E:** Cypress tests for critical user flows
- **Minimum coverage:** 80% line coverage for all backend services

### Running Tests Locally

```bash
# Backend (all services)
./gradlew test

# Backend (single service)
./gradlew :services:flashcard-service:test

# Coverage report
./gradlew :services:flashcard-service:jacocoTestReport
# Open: services/flashcard-service/build/reports/jacoco/test/html/index.html

# Frontend
cd frontend && npm test

# E2E (requires running app)
cd frontend && npx cypress run
```

### Reporting Issues

Please open an issue with:
- A clear title and description
- Steps to reproduce (for bugs)
- Expected vs. actual behavior
- Screenshots if applicable

## Code of Conduct

Be respectful. Focus on the code and ideas, not people. Medical content should be accurate — if you spot errors in flashcard or exam content, please flag them.
