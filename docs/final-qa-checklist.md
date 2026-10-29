# Final QA Checklist

Review date: 29 October 2026

This checklist records the final local verification for the one-cafe, store-wide points release. It does not replace the production Cloud Run and Cloud SQL release gates in `docs/google-cloud-deployment.md`.

## Automated Verification

- `UiConsistencyTests` passes the shared navigation, one-store UI, store-wide points language, and Cast shift-approval copy checks.
- The full Maven test suite passed: 119 tests, 0 failures, 0 errors, and 1 skipped opt-in MySQL migration test.
- The opt-in MySQL migration test still requires a disposable MySQL 8.4 instance and the `CASTFAN_MYSQL_TEST_*` environment variables.
- GitHub Actions runs Trivy source and immutable container-image scans and blocks deployment on unfixed high or critical findings.

## Responsive And Accessibility Smoke Review

The following authenticated routes were reviewed with seeded development data at 320 px. Key shells were also reviewed at 390 px and desktop width.

- Member: Home, Schedule, Following, Account, and Membership points.
- Cast: Dashboard, shared Schedule, public Cast Profile, Profile editor, shift requests, and content composer.
- Owner: Dashboard, shift review, Member points, events, promotions, and Account.

Checks performed:

- No root-level horizontal overflow on the reviewed routes.
- Navigation labels remain readable at 320 px, including the five-item Cast and Owner navigation bars.
- Visible controls expose accessible names; hidden date/time helpers and CSRF inputs are excluded from the visible-control check.
- Reviewed images provide alternate text.
- Fixed bottom navigation does not cover the final actionable content.

## Intentionally Separate Production Gates

Before a real production release, complete the remaining account recovery, edge rate limiting, personal-data retention, Cloud SQL backup, Secret Manager rotation, budget alert, and least-privilege IAM decisions documented in `docs/security-review.md` and `docs/google-cloud-deployment.md`.
