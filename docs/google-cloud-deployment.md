# Google Cloud Deployment

## Deployment Decision

CastFan will use the following production services:

| Responsibility | Google Cloud service |
| --- | --- |
| Spring Boot hosting | Cloud Run |
| Production database | Cloud SQL for MySQL |
| Database credentials | Secret Manager |
| Source build | Cloud Build |
| Container storage | Artifact Registry |
| Logs and runtime metrics | Cloud Logging and Cloud Monitoring |

Cloud Run is the preferred hosting target because it supports Java source deployments, scales automatically, and does not require the project team to maintain a virtual machine.

## Environment Strategy

- Local development uses the `dev` Spring profile and embedded H2.
- Automated tests use the `test` Spring profile and isolated H2.
- Google Cloud uses the `prod` Spring profile and Cloud SQL for MySQL.
- The Cloud Run region and Cloud SQL region should match to reduce latency and network cost.
- Production resources should use a dedicated service account with only the required IAM roles.

## Required Runtime Configuration

The Cloud Run service must receive these values:

| Variable | Source | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Environment variable | Must be `prod` |
| `DB_URL` | Environment variable or Secret Manager | Cloud SQL JDBC URL |
| `DB_USERNAME` | Secret Manager | MySQL application user |
| `DB_PASSWORD` | Secret Manager | MySQL application password |

Cloud Run provides `PORT` automatically. The application reads it through `server.port=${PORT:8080}` and falls back to port `8080` for local development.

Never commit service-account keys, database passwords, `.env` files, or production property files containing credentials.

## First Deployment Checklist

1. Create or select a Google Cloud project and enable billing alerts.
2. Enable Cloud Run, Cloud Build, Artifact Registry, Cloud SQL Admin, and Secret Manager APIs.
3. Select one region for both Cloud Run and Cloud SQL.
4. Create a Cloud SQL for MySQL instance, database, and least-privilege application user.
5. Add a schema migration tool and apply the production schema before starting the service. Hibernate must remain set to `validate` in production.
6. Configure the Cloud SQL Java Connector and grant the Cloud Run service account the Cloud SQL Client role.
7. Store the database username and password in Secret Manager and grant the service account access only to those secrets.
8. Deploy the service from source to Cloud Run with the `prod` profile.
9. Allow unauthenticated access only to the public web service; keep administrative resources private.
10. Verify registration, sign-in, authorization, database persistence, HTTPS redirects, logs, and health after deployment.
11. Configure a budget alert, backups, point-in-time recovery where appropriate, and a cleanup plan for unused resources.

## Deployment Command Template

Run deployment commands only after the Cloud SQL connection and secrets are ready. Replace every uppercase placeholder with the actual project values.

```bash
gcloud run deploy castfan \
  --source . \
  --project PROJECT_ID \
  --region REGION \
  --allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL=JDBC_URL \
  --set-secrets DB_USERNAME=castfan-db-username:VERSION,DB_PASSWORD=castfan-db-password:VERSION \
  --service-account castfan-runtime@PROJECT_ID.iam.gserviceaccount.com
```

Pin secret environment variables to a numbered version for predictable releases. Rotate by creating a new secret version and deploying a new Cloud Run revision.

## Production Readiness Gates

The application is not ready for production deployment until all of these are complete:

- Database migrations work against MySQL, not only H2.
- The Cloud SQL connector and connection pool limits are configured for Cloud Run.
- Authentication rate limiting, account recovery, and production session settings are implemented.
- Owner and Cast authorization tests pass.
- A health endpoint and post-deployment smoke test are available.
- Logs contain no passwords, tokens, personal data, or JDBC credentials.
- Cloud SQL backups and cost alerts are enabled.

## Official References

- [Deploy services from source code](https://cloud.google.com/run/docs/deploying-source-code)
- [Connect from Cloud Run to Cloud SQL for MySQL](https://cloud.google.com/sql/docs/mysql/connect-run)
- [Configure secrets for Cloud Run services](https://cloud.google.com/run/docs/configuring/services/secrets)
