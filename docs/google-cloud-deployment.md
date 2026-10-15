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
- Cloud Run, Cloud SQL, and Artifact Registry use `asia-southeast1` (Singapore) to reduce latency and network cost.
- Production resources should use a dedicated service account with only the required IAM roles.

## Required Runtime Configuration

The Cloud Run service must receive these values:

| Variable | Source | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Environment variable | Must be `prod` |
| `DB_URL` | Environment variable or Secret Manager | Cloud SQL JDBC URL |
| `DB_USERNAME` | Secret Manager | MySQL application user |
| `DB_PASSWORD` | Secret Manager | MySQL application password |
| `SESSION_TIMEOUT` | Environment variable | Optional session lifetime; defaults to `30m` |
| `DB_POOL_MAX_SIZE` | Environment variable | Optional per-instance database pool cap; defaults to `5` |

Cloud Run provides `PORT` automatically. The application reads it through `server.port=${PORT:8080}` and falls back to port `8080` for local development.

Never commit service-account keys, database passwords, `.env` files, or production property files containing credentials.

## First Deployment Checklist

1. Create or select a Google Cloud project and enable billing alerts.
2. Enable Cloud Run, Cloud Build, Artifact Registry, Cloud SQL Admin, and Secret Manager APIs.
3. Select one region for both Cloud Run and Cloud SQL.
4. Create a Cloud SQL for MySQL instance, database, and least-privilege application user.
5. Run the committed Flyway migrations against a disposable MySQL database, then allow the service to apply them to Cloud SQL before Hibernate validates the schema.
6. Configure the Cloud SQL Java Connector and grant the Cloud Run service account the Cloud SQL Client role.
7. Store the database username and password in Secret Manager and grant the service account access only to those secrets.
8. Deploy the service from source to Cloud Run with the `prod` profile.
9. Allow unauthenticated access only to the public web service; keep administrative resources private.
10. Verify registration, sign-in, authorization, database persistence, HTTPS redirects, logs, and health after deployment.
11. Configure a budget alert, backups, point-in-time recovery where appropriate, and a cleanup plan for unused resources.

## CastFan Cloud Resources

| Resource | Value |
| --- | --- |
| Project | `castfan-system` |
| Region | `asia-southeast1` |
| Cloud SQL instance | `castfan-system:asia-southeast1:castfan-mysql` |
| Database | `castfan` |
| Artifact Registry repository | `castfan-images` |
| Runtime service account | `castfan-runtime@castfan-system.iam.gserviceaccount.com` |
| Cloud Run service | `https://castfan-473125501009.asia-southeast1.run.app` |

The initial non-production Cloud Run deployment completed on 15 October 2026. It runs revision `castfan-00002-zbc` with zero minimum instances, a maximum of two instances, and Cloud SQL/Secret Manager access through the dedicated runtime service account.

The current Cloud SQL instance is a 30-day free trial intended for development and demonstration. It does not support backups and stops serving requests when the trial ends unless it is upgraded. Export or upgrade the database before the trial counter expires.

The production JDBC URL uses the Cloud SQL Java Connector. It authenticates with the runtime service account and does not contain a database password:

```text
jdbc:mysql:///castfan?cloudSqlInstance=castfan-system:asia-southeast1:castfan-mysql&socketFactory=com.google.cloud.sql.mysql.SocketFactory&cloudSqlRefreshStrategy=lazy&ipTypes=PUBLIC
```

## Build And Push

Build and test locally before publishing the image:

```powershell
.\mvnw.cmd test
docker build -t asia-southeast1-docker.pkg.dev/castfan-system/castfan-images/castfan-app:latest .
docker push asia-southeast1-docker.pkg.dev/castfan-system/castfan-images/castfan-app:latest
```

## Deployment Command

Run the deployment only after the Cloud SQL connection, numbered secret versions, and image are ready.

```powershell
gcloud.cmd run deploy castfan `
  --image asia-southeast1-docker.pkg.dev/castfan-system/castfan-images/castfan-app:latest `
  --project castfan-system `
  --region asia-southeast1 `
  --allow-unauthenticated `
  --startup-probe httpGet.path=/actuator/health,initialDelaySeconds=10,failureThreshold=20,timeoutSeconds=5,periodSeconds=5 `
  --liveness-probe httpGet.path=/actuator/health/liveness,initialDelaySeconds=30,failureThreshold=3,timeoutSeconds=3,periodSeconds=10 `
  --env-vars-file deploy/cloud-run.env.yaml `
  --set-secrets DB_USERNAME=castfan-db-username:1,DB_PASSWORD=castfan-db-password:1 `
  --service-account castfan-runtime@castfan-system.iam.gserviceaccount.com `
  --min 0 `
  --max 2
```

Pin secret environment variables to a numbered version for predictable releases. Rotate by creating a new secret version and deploying a new Cloud Run revision.

The `prod` profile denies `/h2-console/**`, `/concepts/**`, and every non-health Actuator route; requires HTTPS using forwarded proxy headers; enables restrictive browser security headers; and issues secure session cookies. `/actuator/health` and `/actuator/health/liveness` are public for Cloud Run probes but expose only aggregate status. Do not override these controls in a deployed revision.

## Production Readiness Gates

The deployed application is not ready for production use until all of these are complete:

- The version 1 Flyway migration must continue to pass the opt-in MySQL integration test before every production schema release. It was first verified against MySQL 8.4.11 on 15 October 2026.
- The Cloud SQL connector and connection pool limits are configured for Cloud Run.
- Production session settings are implemented.
- Authentication rate limiting is configured at the edge and an account recovery process is available.
- Owner and Cast authorization tests pass.
- A post-deployment smoke test is available and verifies the deployed health endpoint.
- Logs contain no passwords, tokens, personal data, or JDBC credentials.
- Cloud SQL backups and cost alerts are enabled.

See [security-review.md](security-review.md) for the current control inventory and remaining release blockers.

## Official References

- [Deploy services from source code](https://cloud.google.com/run/docs/deploying-source-code)
- [Connect from Cloud Run to Cloud SQL for MySQL](https://cloud.google.com/sql/docs/mysql/connect-run)
- [Configure secrets for Cloud Run services](https://cloud.google.com/run/docs/configuring/services/secrets)
