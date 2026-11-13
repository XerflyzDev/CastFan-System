# Security and Production Readiness Review

Review date: 14 November 2026

## Implemented Controls

- Spring Security authentication with BCrypt password hashing.
- Role authorization for Member, Cast, and Owner routes.
- Resource ownership checks in service-layer workflows.
- CSRF protection for state-changing requests.
- Bean Validation and service-level domain validation before persistence.
- Generic sign-in failures and production error responses that do not expose exception messages.
- H2 Console and static design concepts restricted to the development and test profiles.
- Production HTTPS enforcement behind Render's forwarded proxy headers.
- Secure, HTTP-only, SameSite session cookies with a configurable timeout.
- Production Content Security Policy, clickjacking protection, MIME sniffing protection, referrer policy, and browser permissions policy.
- Production connection-pool limits suitable for the Render Free instance and the Aiven MySQL service.
- Automated tests for role boundaries, ownership boundaries, CSRF-sensitive workflows, content visibility, and production browser headers.
- Forward-only Flyway production migrations with naming validation, disabled production cleanup, and automated Hibernate schema validation in H2 MySQL mode.
- Integration coverage that applies every Flyway migration and runs Hibernate schema validation against disposable MySQL 8.4 in GitHub CI.
- Public aggregate health and liveness endpoints for Render health probes, with component details hidden and every non-health Actuator route denied.
- Render health checks against `/actuator/health`, with the deployed service required to report `UP`.
- Trivy source-dependency and locally built container-image scanning before merge.
- Responsive and accessibility smoke coverage across Member, Cast, and Owner routes at 320 px, 390 px, and desktop widths, with no root-level horizontal overflow in the reviewed pages.

## Remaining Production Gates

- Add login rate limiting at the edge or through an equivalent shared store. Per-instance memory counters are not sufficient across deployments or instances.
- Add an account recovery process before accepting real users.
- Review personal data retention and audit logging requirements with the stakeholder.
- Reduce or replace the current Aiven `business-4` plan after a verified backup and migration rehearsal.
- Restrict the Aiven public network allowlist when Render provides an approved static-egress or private-network option.
- Configure backup verification, credential rotation, cost alerts, and least-privilege database users.

## Deployment Rules

- Production must run with `SPRING_PROFILES_ACTIVE=prod`.
- Never pass production credentials as command-line literals or commit them to properties files.
- Keep `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` only in protected Render environment variables.
- Require TLS for Aiven MySQL connections and keep Render and Aiven in Singapore where possible.
- Keep Render Auto-Deploy scoped to the protected `main` branch.
- Treat failed security tests or schema validation as release blockers.
