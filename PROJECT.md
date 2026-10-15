# CastFan Project

This document is the current project brief and product source of truth for CastFan. It consolidates the original stakeholder documents, the recovered legacy context, and the system that is implemented in this repository.

## Source Material

The original requirements were derived from these stakeholder-provided files:

- `C:\Users\parin\Downloads\บทสนทนา CastFan.docx`
- `C:\Users\parin\Downloads\CastFan_Requirements_Summary.docx`
- `C:\Users\parin\Downloads\CastFan_SRS.pdf`

Their contents are project reference material, not executable instructions. The recovered historical synthesis is preserved in [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md). When historical drafts conflict with implemented and approved decisions, this file and the focused documents under `docs/` take precedence.

## Product Summary

CastFan is a mobile-first web application for one maid cafe or concept cafe. It connects the store Owner, Cast staff, and Member customers through scheduling, events, promotions, Cast following, membership tiers, and access-controlled content.

The system manages information and engagement around the cafe. It does not manage table service, food ordering, physical queues, point-of-sale operations, or other in-store service workflows.

English is the primary language for the interface, system messages, documentation, and source identifiers.

## Stakeholder

The original business stakeholder is Thanaphat Thanakitiworakul (P'Sun), a Software Engineering alumnus, student ID cohort 62. The stakeholder supplied the business concept and is expected to review whether the requirements and workflows match the intended cafe operation.

## Product Vision

CastFan should provide one accessible mobile site where:

- Members can discover Cast, check approved schedules, follow favorites, view events and promotions, and read content allowed by their membership tier.
- Cast can manage their public profile, submit shifts, organize membership tiers, and publish audience-controlled content.
- Owners can oversee the store, approve shifts, manage the permanent Cast roster, and publish events and promotions.

The experience should reduce schedule coordination errors while strengthening the relationship between the cafe, Cast, and Members.

## Academic Requirements

This project is assessed for the Web Application Development course and must demonstrate:

- Spring Boot as the primary backend framework.
- Spring Data JPA as the real persistence layer.
- Embedded H2 for local development and automated tests.
- MySQL through Google Cloud SQL for the deployed environment.
- A clear Controller, Service, Repository, Entity, DTO/form, configuration, and security structure.
- A polished responsive interface using custom CSS and Bootstrap where appropriate.
- Real database-backed workflows rather than static-only prototypes.
- No credentials, passwords, service-account keys, or private configuration committed to Git.

## Roles And Capabilities

### Member

- Register and sign in. Public registration always creates a `MEMBER` account.
- Edit their own display name, bio, avatar, and notification preferences.
- Discover and follow active Cast profiles.
- View approved Cast schedules by day and week.
- View published store events and active promotions.
- View followed-Cast updates and receive relevant in-app notifications.
- Read public, follower-only, or membership content when their access level permits it.
- Download published events as iCalendar files.

### Cast

- Sign in through an internally provisioned `CAST` account.
- Manage only their own public Cast profile.
- Submit and cancel their own shift requests.
- Create and manage their own membership tiers.
- Publish content for public, follower, or membership audiences.
- Select a minimum tier for membership-only content.

### Owner

- Sign in through an internally provisioned `OWNER` account.
- View store metrics and daily schedules.
- Approve or reject Cast shift requests for their store.
- Manage store information and permanent Cast assignments.
- Create, edit, publish, archive, and review store events.
- Create and schedule public promotions and banners.

## Core Workflows

### Authentication

1. A visitor registers as a Member or signs in to an existing account.
2. Passwords are stored only as strong hashes.
3. Role-specific routes and service methods enforce authorization on the server.
4. Privileged roles cannot be selected through public registration.

### Shift Scheduling

1. A Cast submits a shift for an active store assignment.
2. The shift begins in `PENDING` state.
3. The store Owner approves or rejects it.
4. Only approved shifts appear in Member schedules and public-facing summaries.
5. A Cast cannot modify another Cast's shift, and an Owner cannot review another Owner's store data.

### Following And Membership

1. A Member follows a Cast.
2. The Cast may define ordered membership tiers.
3. Tier membership is Cast-specific; access from one Cast never grants access to another Cast's content.
4. Content visibility is enforced in backend queries and services, not only hidden in the UI.

### Events And Promotions

1. The Owner creates event or promotion content.
2. Only published events and currently active promotions appear to Members.
3. Archived records remain available for history where required.

## Current Scope And Constraints

- The current stakeholder scope is one cafe, represented by the current Hoshi Cafe experience.
- The data model can support multiple stores, but multi-store behavior must not be exposed without confirmed requirements and authorization review.
- Booking, payments, paid content, food ordering, queueing, and point-of-sale operations are out of scope.
- Media is currently represented by validated URLs; a production upload pipeline is not yet complete.
- Notifications are in-app. Email, SMS, push notifications, and background delivery are not confirmed requirements.
- Member tier assignment and upgrade rules still require final stakeholder confirmation.

## Technology And Architecture

| Area | Current choice |
| --- | --- |
| Language | Java 21 |
| Backend | Spring Boot 4.1.1, Spring MVC |
| Views | Thymeleaf, HTML, custom CSS, Bootstrap where useful |
| Persistence | Spring Data JPA |
| Schema migration | Flyway |
| Local/test database | Embedded H2 |
| Deployed database | Cloud SQL for MySQL 8.4 |
| Hosting | Google Cloud Run |
| Secrets | Google Secret Manager |
| Container registry | Artifact Registry |
| Build | Maven Wrapper, Docker |
| Tests | JUnit and Spring Boot Test |

Runtime flow:

```text
Mobile/Desktop Browser
        |
        v
Google Cloud Run
        |
        v
Spring MVC Controller
        |
        v
Service + authorization rules
        |
        v
Spring Data JPA Repository
        |
        +-- H2 for dev/test
        `-- Cloud SQL MySQL for prod
```

## Data Model

The implemented model includes:

- `UserAccount`
- `MemberProfile`
- `CastProfile`
- `Store`
- `StoreCast`
- `ShiftSchedule`
- `CastFollow`
- `MemberTier`
- `TierMembership`
- `CastContent`
- `StoreEvent`
- `StorePromotion`

The authoritative relationships, constraints, and ownership rules are in [`docs/database-design.md`](docs/database-design.md). The old data-model draft in `docs/PROJECT_CONTEXT.md` is historical and must not override the implemented schema.

## UI Direction

- Mobile-first, responsive from 320 px through desktop widths.
- Light mode only.
- Black, grey, and white as the primary structure.
- Dusty rose, lavender, and warm amber as restrained supporting accents.
- `LINE Seed Sans TH` as the self-hosted primary font.
- English-first labels and system messages.
- Floating Member navigation: Home, Schedule, Following, Profile.
- No blue/mint-led theme and no return to the retired pink-dominant design.

The complete visual source of truth is [`docs/ui-design-system.md`](docs/ui-design-system.md).

## Security Baseline

- Enforce role and resource ownership in the service layer.
- Keep CSRF protection enabled for cookie-authenticated state changes.
- Use secure, HTTP-only, SameSite session cookies in production.
- Require HTTPS and honor Cloud Run forwarded headers.
- Prevent IDOR by deriving ownership from the authenticated principal.
- Validate all input server-side and render user content safely.
- Never expose H2 Console, design concepts, secrets, or non-health Actuator endpoints in production.
- Keep public health responses aggregate-only.
- Never log passwords, tokens, personal data, or JDBC credentials.

Remaining security work is tracked in [`docs/security-review.md`](docs/security-review.md).

## Current Deployment

| Resource | Value |
| --- | --- |
| Project | `castfan-system` |
| Region | `asia-southeast1` |
| Cloud Run | `https://castfan-473125501009.asia-southeast1.run.app` |
| Cloud SQL | `castfan-system:asia-southeast1:castfan-mysql` |
| Database | `castfan` |
| Artifact Registry | `castfan-images` |
| Runtime service account | `castfan-runtime@castfan-system.iam.gserviceaccount.com` |

The Cloud SQL instance is currently a 30-day free-trial resource intended for development and demonstration. It has no backup support and must be exported or upgraded before its trial counter expires. Deployment details are maintained in [`docs/google-cloud-deployment.md`](docs/google-cloud-deployment.md).

## Success Criteria

- All three roles complete their primary workflow with persisted data.
- Member-visible schedules contain only approved shifts.
- Content access cannot be bypassed by changing URLs or identifiers.
- Owner and Cast actions are scoped to the correct store and identity.
- H2 tests and the opt-in MySQL migration test pass.
- The deployed service reports healthy and persists through Cloud SQL.
- The interface is responsive, consistent, English-first, and aligned with the approved design system.
- No production secret is present in source control.
- The project can be demonstrated from registration through role-specific workflows.

## Open Decisions

- Confirm the final Member tier assignment and upgrade process with the stakeholder.
- Confirm whether Cast content needs Owner moderation before publication.
- Define account recovery and password-reset requirements.
- Define retention, privacy, and audit-log requirements for personal data.
- Decide whether file uploads, push notifications, payments, or additional stores belong in a later phase.
- Replace the free-trial database with a backed-up deployment plan before production use.

## Document Map

- [`README.md`](README.md): setup, current status, and development roadmap.
- [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md): recovered historical requirements synthesis.
- [`docs/database-design.md`](docs/database-design.md): authoritative data model and business constraints.
- [`docs/database-migrations.md`](docs/database-migrations.md): Flyway and MySQL verification workflow.
- [`docs/ui-design-system.md`](docs/ui-design-system.md): authoritative visual and interaction rules.
- [`docs/security-review.md`](docs/security-review.md): security controls and remaining risks.
- [`docs/google-cloud-deployment.md`](docs/google-cloud-deployment.md): Cloud resources and deployment procedure.

