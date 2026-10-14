# Security and Production Readiness Review

Review date: 14 October 2026

## Implemented Controls

- Spring Security authentication with BCrypt password hashing.
- Role authorization for Member, Cast, and Owner routes.
- Resource ownership checks in service-layer workflows.
- CSRF protection for state-changing requests.
- Bean Validation and service-level domain validation before persistence.
- Generic sign-in failures and production error responses that do not expose exception messages.
- H2 Console and static design concepts restricted to the development and test profiles.
- Production HTTPS enforcement behind forwarded Cloud Run headers.
- Secure, HTTP-only, SameSite session cookies with a configurable timeout.
- Production Content Security Policy, clickjacking protection, MIME sniffing protection, referrer policy, and browser permissions policy.
- Production connection-pool limits suitable for an initial Cloud Run deployment.
- Automated tests for role boundaries, ownership boundaries, CSRF-sensitive workflows, content visibility, and production browser headers.

## Remaining Production Gates

- Add Flyway migrations and verify the complete schema against MySQL.
- Add login rate limiting at the edge with Google Cloud Armor or an equivalent shared store. Per-instance memory counters are not sufficient when Cloud Run scales horizontally.
- Add an account recovery process before accepting real users.
- Add a dedicated health endpoint and a post-deployment smoke test.
- Review personal data retention and audit logging requirements with the stakeholder.
- Run dependency and container image vulnerability scanning in CI.
- Verify responsive layouts and accessibility with production-like data.
- Configure Cloud SQL backups, Secret Manager rotation, budget alerts, and least-privilege IAM.

## Deployment Rules

- Production must run with `SPRING_PROFILES_ACTIVE=prod`.
- Never pass production credentials as command-line literals or commit them to properties files.
- Use numbered Secret Manager versions for predictable releases.
- Keep Cloud Run and Cloud SQL in the same region.
- Treat failed security tests or schema validation as release blockers.
