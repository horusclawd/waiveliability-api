# WaiveLiability API — Claude Instructions

## Project
Spring Boot 3.3 / Java 21 backend for WaiveLiability. See the full architecture and roadmap in the sibling docs repo:
- `/Users/jon/Development/waiveliability/waiveliability/ARCHITECTURE.md`
- `/Users/jon/Development/waiveliability/waiveliability/ROADMAP.md`

## Git Workflow
- Each sprint has its own branch named `sprint-N/description` (e.g. `sprint-1/auth-identity`)
- Branch off `main` at the start of each sprint
- Commit regularly with clear, descriptive messages as work progresses
- At the end of each sprint:
  1. Stage and commit all remaining changes with an appropriate summary commit message
  2. Push the branch to origin
  3. Open a PR against `main` with a title and description summarizing what was completed in the sprint
  4. Do not merge — leave the PR for the owner to review and approve

## Tech Stack
- Java 21, Spring Boot 3.3, Gradle (Kotlin DSL)
- PostgreSQL 16, Flyway migrations, Spring Data JPA + Hibernate
- Spring Security + JJWT (stateless JWT, HTTP-only cookies)
- Redis (Lettuce), AWS SDK v2 (S3), Apache PDFBox 3
- Spring Mail + AWS SES, Stripe Java SDK
- SpringDoc OpenAPI 3 (auto-generated spec consumed by Angular)
- JUnit 5, Mockito, Testcontainers
- Docker + Docker Compose (PG + Redis + LocalStack for local dev)

## Key Conventions
- Multi-tenancy via `TenantContext` ThreadLocal — never accept `tenantId` as a caller parameter
- Plan limits enforced via AOP `@CheckPlanLimit` at the service layer only
- All migrations in `src/main/resources/db/migration/` as `VN__description.sql`
- RFC 7807 problem details for all error responses via `GlobalExceptionHandler`
- Base API path: `/api/v1`
