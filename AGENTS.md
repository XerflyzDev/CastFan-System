# CastFan Agent Guide

These instructions apply to every coding agent working in this repository.

## Read First

Before making changes, read:

1. [`PROJECT.md`](PROJECT.md) for current product scope and requirements.
2. [`README.md`](README.md) for setup and implementation status.
3. The focused document under `docs/` for the area being changed.

`PROJECT.md` and the focused current documents take precedence over the historical `docs/PROJECT_CONTEXT.md` when they conflict.

## Product Rules

- Treat CastFan as a mobile-first web application for one cafe unless the user explicitly expands scope.
- Keep the interface, messages, documentation, and identifiers English-first.
- Preserve the three roles: `MEMBER`, `CAST`, and `OWNER`.
- Public registration must create only `MEMBER` accounts.
- Never add public privileged-role selection or a public Owner/Cast provisioning endpoint.
- Keep booking, payments, food ordering, physical queues, and point-of-sale features out of scope unless explicitly approved.

## Technology Constraints

- Use Java 21 and the repository's existing Spring Boot version.
- Use Spring MVC and Thymeleaf for server-rendered web flows.
- Use Spring Data JPA for persistence; do not replace it with ad hoc JDBC or another backend stack.
- Use H2 for `dev` and `test`, and Cloud SQL MySQL for `prod`.
- Use Flyway for production schema changes.
- Run builds through `mvnw.cmd` on Windows or `./mvnw` on Unix-like systems.
- Follow the existing Controller, Service, Repository, Entity, form/DTO, configuration, and security boundaries.

## Data And Migration Rules

- Treat [`docs/database-design.md`](docs/database-design.md) as the current data-model authority.
- Do not use `spring.jpa.hibernate.ddl-auto` to mutate production schema.
- Add forward-only Flyway migrations for every production schema change.
- Preserve existing migration files after they have been deployed.
- Enforce uniqueness, foreign keys, and ownership in both database constraints and service logic where appropriate.
- Use transactions for multi-record workflows such as shift approval, following, and membership changes.
- Do not expose JPA entities directly as unrestricted request payloads.

## Security Rules

- Enforce authorization in backend services and methods, not only in templates or JavaScript.
- Resolve the acting Member, Cast, or Owner from the authenticated principal.
- Never trust a client-supplied user, Cast, Member, Owner, or store ID as proof of ownership.
- Keep CSRF protection enabled for authenticated state changes.
- Validate every request and reject invalid enum values, dates, URLs, and cross-owner relationships.
- Keep passwords hashed with the configured password encoder.
- Do not log secrets, passwords, session identifiers, personal data, or complete JDBC URLs with credentials.
- Never commit `.env` files, service-account keys, database passwords, access tokens, or downloaded credentials.
- Keep `/h2-console/**`, `/concepts/**`, and non-health Actuator routes unavailable in production.
- Do not weaken secure cookies, HTTPS enforcement, CSP, HSTS, or other production headers without an explicit reviewed requirement.

## UI Rules

- Follow [`docs/ui-design-system.md`](docs/ui-design-system.md).
- Use `LINE Seed Sans TH` and English-first interface copy.
- Keep light mode, mobile-first layouts, neutral black/grey/white structure, and restrained dusty-rose/lavender/amber accents.
- Do not reintroduce the retired blue/mint or pink-dominant themes.
- Preserve the approved Member navigation order: Home, Schedule, Following, Profile.
- Build complete responsive, focus, validation, empty, error, disabled, hover, and selected states.
- Keep controls accessible and prevent text or navigation from overlapping content at 320 px and wider viewports.
- Reuse existing fragments, CSS tokens, components, and icon libraries before adding new patterns.

## Testing Expectations

Run the narrowest relevant tests while developing, then run the full suite before finalizing shared or deployment-facing changes:

```powershell
.\mvnw.cmd test
```

The MySQL migration integration test is opt-in and requires:

```powershell
$env:CASTFAN_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:3307/castfan'
$env:CASTFAN_MYSQL_TEST_USERNAME='castfan'
$env:CASTFAN_MYSQL_TEST_PASSWORD='castfan_test'
.\mvnw.cmd -Dtest=DatabaseMySqlMigrationTests test
```

For UI changes, verify relevant Member, Cast, and Owner pages at mobile and desktop sizes. For deployment changes, build the image and verify `/actuator/health` before releasing.

## Cloud And Deployment Rules

- Use project `castfan-system` and region `asia-southeast1` for the current environment.
- Use the dedicated runtime identity `castfan-runtime@castfan-system.iam.gserviceaccount.com`.
- Read database credentials from Secret Manager; never place values in source files or deployment commands.
- Pin Secret Manager references to numbered versions for releases.
- Prefer immutable image digests for Cloud Run revisions.
- Keep Cloud Run minimum instances at zero and maximum instances at two unless the user approves a cost change.
- Do not create service-account JSON keys.
- Do not enable paid scanning, high availability, replicas, or larger Cloud resources without explicit cost approval.
- Remember that the current Cloud SQL free-trial instance stops after 30 days and does not support backups.
- Follow [`docs/google-cloud-deployment.md`](docs/google-cloud-deployment.md) for the current release procedure.

## Change Discipline

- Inspect the repository and current Git status before editing.
- Work with existing user changes; never discard unrelated modifications.
- Keep changes scoped to the requested behavior.
- Update `PROJECT.md`, `README.md`, or focused docs when a decision, workflow, deployment resource, or source of truth changes.
- Do not copy legacy prototype code into the application without adapting it to the current Spring Boot architecture and security model.
- Do not commit or push unless the user requests it.
- Before reporting completion, run relevant verification and state any skipped tests or unresolved production risks.

