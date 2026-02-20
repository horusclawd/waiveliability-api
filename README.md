# WaiveLiability API

Spring Boot 3.3 / Java 21 backend service.

## Prerequisites

- Java 21
- Docker & Docker Compose
- (Optional) IntelliJ IDEA or VS Code with Java extensions

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL** (port 5432) — `waiveliability` / `changeme`
- **Redis** (port 6379) — password: `changeme`
- **LocalStack** (port 4566) — S3 + SES for local AWS emulation
- **Mailhog** (port 1025) — SMTP server with web UI at http://localhost:8025

### 2. Configure Environment

Copy the example env file:

```bash
cp .env.example .env
```

Edit `.env` and set any required values. The defaults work for local development.

### 3. Run the Application

```bash
./gradlew bootRun
```

The API will start at http://localhost:8080

## Common Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Clean build
./gradlew clean build
```

## API Documentation

Once running, Swagger UI is available at:
http://localhost:8080/swagger-ui.html

## Project Structure

```
src/main/java/com/waiveliability/
├── config/           # Security, JWT, S3, Stripe, Redis, CORS
├── common/           # Exception handling, pagination, validation
├── modules/
│   ├── identity/     # Auth, users
│   ├── tenant/       # Multi-tenancy
│   ├── forms/        # Form definitions
│   ├── submissions/  # Form submissions
│   ├── documents/    # PDF generation
│   ├── templates/    # Template library
│   ├── billing/      # Stripe integration
│   └── notifications/# Email
└── security/         # JWT filter, tenant context, plan enforcement
```

## Notes

- Multi-tenancy is handled via `TenantContext` (ThreadLocal)
- Plan limits are enforced via `@CheckPlanLimit` AOP annotation
- Migrations run automatically via Flyway on startup
