# Cloudflare Edge Deployment

## Deployment Decision

Cloudflare is prepared as an optional edge gateway in front of the existing
Google Cloud deployment. Spring Boot remains on Cloud Run and production data
remains in Cloud SQL for MySQL. This preserves the course requirements, the
Cloud SQL Java Connector, the dedicated Google runtime identity, and the
current Flyway migration workflow.

```text
Browser
  |
  v
Cloudflare Worker (DNS, edge policy, optional WAF/rate limiting)
  |
  v
Google Cloud Run (Spring Boot)
  |
  v
Google Cloud SQL for MySQL
```

The Worker source and Wrangler configuration live under `cloudflare/`. The
gateway forwards methods, bodies, cookies, CSRF headers, and query strings to
Cloud Run. Redirects that point back to the Cloud Run hostname are rewritten to
the public Cloudflare hostname. Only versioned public static paths are eligible
for edge caching; dynamic HTML and authenticated routes are not cached by the
Worker.

This setup does not deploy the Spring Boot image to Cloudflare Containers.
Containers require a Workers Paid plan and do not provide the attached Google
service account identity currently used by the Cloud SQL Java Connector. Do
not create a service-account JSON key to work around that boundary.

## Prerequisites

- Node.js 20 or newer.
- A Cloudflare account with Workers enabled.
- Access to the existing Cloud Run origin.
- Docker is not required for the edge-only Worker.

The default `workers.dev` deployment can be used for a preview. A production
custom domain and any paid WAF or rate-limiting feature require separate review
and explicit cost approval.

## Local Validation

From the repository root:

```powershell
Set-Location .\cloudflare
npm install
npm run check
npm run deploy:dry-run
npm run dev
```

Open the local URL printed by Wrangler and verify:

- `/actuator/health` returns an aggregate healthy response.
- Registration and sign-in preserve cookies and CSRF validation.
- An authenticated Member, Cast, and Owner can navigate their allowed pages.
- Redirects stay on the Worker hostname and do not expose the Cloud Run URL.
- `/css/**`, `/js/**`, `/fonts/**`, and `/images/**` load successfully.

Local Wrangler state and `.dev.vars` files are ignored by Git. Do not put
database passwords, Google credentials, session identifiers, or API tokens in
`wrangler.jsonc`.

## Preview Deployment

Authentication changes Cloudflare account state. Run these commands only after
the account and cost boundary have been approved:

```powershell
Set-Location .\cloudflare
npx wrangler login
npm run deploy
```

Wrangler deploys `castfan-edge` to a `workers.dev` URL. Deployment does not
change the Cloud Run service or Cloud SQL database.

After deployment, run a smoke test against the generated URL:

```powershell
$env:CASTFAN_EDGE_URL='https://castfan-edge.<workers-subdomain>.workers.dev'
npm run smoke
Remove-Item Env:CASTFAN_EDGE_URL
```

Do not proceed to a custom production hostname until login rate limiting,
origin protection, cache behavior, and the complete role-based smoke test have
been reviewed.

## Production Domain And Edge Security

For a production hostname:

1. Add the hostname to the Cloudflare zone and route it to `castfan-edge`.
2. Keep TLS in strict mode.
3. Add rate limits for state-changing authentication endpoints, especially
   `POST /login` and `POST /signup`.
4. Bypass edge caching for HTML, authenticated routes, responses containing
   `Set-Cookie`, and every non-GET request.
5. Cache only public static assets with bounded TTLs.
6. Verify the forwarded scheme and host before enabling production traffic.
7. Add origin protection before treating Cloudflare WAF rules as a complete
   security boundary.

The current `run.app` origin remains publicly reachable. Users who know that
URL can bypass Cloudflare controls. A later reviewed change should restrict the
origin using a supported Google/Cloudflare architecture without embedding a
shared secret in source control or blocking Cloud Run health probes.

## Rollback

Cloudflare is an additive edge layer. Roll back by removing the production
route or restoring DNS to the Cloud Run-backed hostname. Do not delete the
Cloud Run service, Cloud SQL instance, Artifact Registry images, secrets, or
service account as part of an edge rollback.

## Official References

- [Cloudflare Workers configuration](https://developers.cloudflare.com/workers/wrangler/configuration/)
- [Wrangler commands](https://developers.cloudflare.com/workers/wrangler/commands/)
- [Cloudflare rate limiting rules](https://developers.cloudflare.com/waf/rate-limiting-rules/)
- [Cloud Run session affinity limitations](https://cloud.google.com/run/docs/configuring/session-affinity)
