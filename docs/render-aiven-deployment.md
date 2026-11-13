# Render and Aiven Deployment

This document is the current deployment source of truth for CastFan. Render hosts the Spring Boot container and Aiven provides MySQL 8.4. The former Google Cloud deployment is retained only as historical recovery documentation in [`google-cloud-deployment.md`](google-cloud-deployment.md).

## Production Topology

```text
Mobile/Desktop Browser
        |
        v
Render Web Service (Singapore, Free)
        |
        v
Spring Boot / Spring MVC / Spring Data JPA
        |
        v
Aiven MySQL 8.4 (Singapore, TLS required)
```

| Resource | Current value |
| --- | --- |
| Repository | `XerflyzDev/CastFan-System` |
| Production branch | `main` |
| Render service | `CastFan-System` |
| Public URL | `https://castfan-system.onrender.com` |
| Render region/plan | Singapore / Free |
| Database service | Aiven MySQL 8.4 |
| Database project | `alpheshost` |
| Database name | `defaultdb` |
| Health endpoint | `/actuator/health` |

The Aiven service plan, network allowlist, and credentials are operational configuration and must not be copied into source control. Cost optimization and network restriction are tracked separately from application releases.

## Environment Variables

Render must provide these values as secrets or protected variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=prod` | Enables production security, MySQL, and Flyway configuration. |
| `DB_URL` | JDBC URL for Aiven MySQL, including `sslMode=REQUIRED`. |
| `DB_USERNAME` | Application database user. |
| `DB_PASSWORD` | Application database password. |
| `PORT` | Supplied by Render; the application reads it through `server.port=${PORT:8080}`. |

Never reveal environment-variable values in screenshots, logs, issues, pull requests, or repository files.

## CI and Release Flow

1. Open a pull request to `main`.
2. GitHub Actions runs the H2/MySQL suite against disposable MySQL 8.4, scans source dependencies, builds the Docker image, and scans the image.
3. Merge only after every required check passes.
4. Render automatically builds the merged `main` commit with `./Dockerfile` and deploys it.
5. Render starts the application with its `PORT` value and checks `/actuator/health`.
6. Flyway applies only forward migrations before Hibernate validates the schema.
7. Verify the live commit, migration version, health response, and role smoke tests.

GitHub Actions does not deploy to Google Cloud or trigger Render. Render's `On Commit` integration is the only production deployment trigger.

## Pre-deployment Database Gate

Before a migration-bearing release:

1. Confirm the latest Aiven backup is complete and restorable.
2. Create a logical MySQL export outside the repository when credentials and an approved secure destination are available.
3. Run `DatabaseMySqlMigrationTests` against disposable MySQL 8.4.
4. Check orphan records, duplicate receipt references, negative balances, and cross-store references required by the migration.
5. Do not edit an applied Flyway migration. Correct failures with a new forward migration.

## Post-deployment Checks

- `GET /actuator/health` returns HTTP 200 with `{"status":"UP"}`.
- Render logs show Aiven MySQL 8.4, successful Flyway validation/migration, Hibernate initialization, and the Render-provided port.
- `/login` exposes `Email or Cast ID` and `Open Your Store`.
- `/signup/store` remains public.
- `/s/castfan` opens the migrated CastFan Store without authentication.
- Member, Cast, and Owner smoke tests pass at 320 px, 390 px, and desktop widths.
- Store-scoped routes never expose another Store's data.

## Rollback

- If CI fails, do not merge and do not deploy.
- If the Render build fails, keep the last successful deployment live and inspect build logs.
- If application startup fails before a migration completes, fix the code or add a new forward migration before redeploying.
- If a migration completes, do not roll the database schema backward by editing or deleting migration history. Restore only from a verified backup when data integrity requires it.
- For an application-only regression, use Render's rollback to the last successful image while keeping the migrated schema forward-compatible.

The former Render deployment at commit `88680ff` is an application rollback reference only. It must not be used after Flyway V12 unless compatibility with the V12 schema has been verified.
