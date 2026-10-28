# CastFan System

CastFan System is a web application for managing a maid cafe or concept cafe. It connects store owners (`Owner`), staff members (`Cast`), and customers (`Member`) in one system.

This project is being built from scratch for three courses: **Web Application Development**, **Software Requirements Analysis and Specification**, and **Database Systems**. It combines requirements engineering, relational database design, and a database-backed Spring Boot web application.

## Project Members

| Student ID | Name | Contributors |
| :---: | --- | --- |
| 6831503057 | Parinya Tanunti | XerflyzDev |
| 6831503053 | Nonnaphat Thirawatthanakamchon | NonnaphatTm |
| 6831503035 | Kunpriya Anwisat | 6831503035-Kun |
| 6831503082 | Areenee Hayiuma | niririniiii |
| 6831503118 | Sirimanee Jakip | 6831503118-SirimaneeJakip |

## Project Language

English is the primary language for the user interface, system messages, documentation, source-code identifiers, and future project content. Localized languages may be added later as optional translations.

## Project Status

> **Cloud Run health checks and production readiness**

The project currently includes the Spring Boot foundation, Maven Wrapper, H2/MySQL profiles, Flyway production schema migration, database-backed authentication, role profiles, Store and Cast scheduling domains, Member follow and membership workflows, and the approved mobile interface. Member Home, Notifications, day and week Schedule views, Following, Profile, public Cast Profile, public Content Detail, and public Event Detail use Spring Data JPA. The notification center aggregates access-controlled updates from followed Cast and upcoming published events while respecting each Member's persisted Cast-update and event-reminder preferences. Members can edit their display name, bio, profile photo, and notification preferences through an ownership-protected profile route. The Member Home includes a feed from followed Cast with server-side audience and tier access checks. Cast accounts share Home and the approved Schedule, have direct navigation to their own Member-facing Cast Profile, use Profile for editing that identity, and have a fifth Cast destination for their private workspace. Cast can upload content images, submit or cancel shifts, manage membership tiers, and publish audience-controlled content through a streamlined social composer with direct publishing, optional headlines, audience selection, and draft support. Owners can review CastFan metrics, inspect schedules by date, approve or reject shift requests, manage the permanent Cast roster, upload event and promotion images, publish events, and schedule promotion banners through ownership-protected routes. Store creation and selection are intentionally hidden because the first release represents one cafe only as CastFan; the internal Store entity remains for relational integrity. User-facing image forms accept JPG, PNG, or WebP uploads up to 2 MB instead of image links. Active, in-range promotions appear on the public Member Home. Published events also provide downloadable iCalendar files.

> **Requirement alignment notice:** The stakeholder confirmed on 27 October 2026 that the first release supports one cafe. The store-wide points core now provides zero-state registration, Owner-only earning/correction/reversal, an immutable receipt-protected ledger, and derived tiers at `1 THB = 1 point`. Owner and Member screens still need to replace the legacy self-subscription and Cast-specific tier UI.

## Objectives

- Simplify Cast shift submission and approval.
- Help Members find schedules and follow their favorite Cast.
- Support store events, promotions, and public information.
- Manage membership tiers and access to Cast content.
- Deliver an attractive, accessible, and responsive mobile-first interface.

## User Roles

### Member

- Register and sign in.
- Browse Cast profiles.
- Follow favorite Cast members.
- View Cast work schedules.
- View store events and promotions.
- View their points and store-wide tier.
- Access content based on their store-wide membership tier.

### Cast

- Use Home and approved Schedule like a Member, with direct Cast Profile, Profile settings, and Cast workspace destinations.
- Preview their own Member-facing Cast profile.
- Manage their profile.
- Submit and update work schedules.
- Publish content and define access permissions using the store-wide tiers managed by the system.

### Owner

- View an overview of Cast members and store schedules.
- Approve or reject submitted schedules.
- Create and manage events.
- Create and manage promotions.
- Manage banners and public store information.
- Search Members and record points after checking customer bills outside CastFan.

## Technology Stack

| Layer | Technology |
| --- | --- |
| Backend | Java, Spring Boot |
| Web | Spring MVC, Thymeleaf |
| Persistence | Spring Data JPA |
| Development Database | Embedded H2 Database |
| Deployment Database | Cloud SQL for MySQL |
| Application Hosting | Google Cloud Run |
| Secret Storage | Google Cloud Secret Manager |
| Frontend | HTML, JavaScript, Bootstrap, Custom CSS |
| Build Tool | Maven |
| Testing | JUnit, Spring Boot Test |

## Planned Architecture

```text
Client / Mobile Browser
          |
          v
Google Cloud Run
          |
          v
Spring MVC Controller
          |
          v
Service Layer
          |
          v
Spring Data JPA Repository
          |
          +-- H2 Database (development and test)
          |
          `-- Cloud SQL MySQL (deployment)
```

The backend will be organized by responsibility into `Controller`, `Service`, `Repository`, `Entity`, `DTO`, configuration, and security packages to keep the system maintainable and testable.

## Database Environments

- `dev` - Embedded H2 for local development.
- `test` - Isolated H2 database for automated tests.
- `prod` - Cloud SQL for MySQL, configured through environment variables or a secret manager.

Sensitive data such as database passwords, API secrets, and service-account keys must never be committed to the repository.

Public registration always creates a `MEMBER`. Privileged `CAST` and `OWNER` accounts are created only through the internal provisioning service; there is no public role-selection or privileged-account endpoint.

## Core Data Model

Implemented and planned entities:

- `UserAccount`
- `AccountAvatar`
- `MemberProfile`
- `MemberPointTransaction`
- `CastProfile`
- `Store`
- `StoreCast`
- `ShiftSchedule`
- `MemberTier`
- `TierMembership`
- `CastFollow`
- `CastContent`
- `StoreEvent`
- `StorePromotion`

Entity details, relationships, constraints, and implementation order are documented in [docs/database-design.md](docs/database-design.md).

The confirmed store-wide points and tier workflow and service/view contract are documented in [docs/membership-points.md](docs/membership-points.md). Its core model is implemented in Flyway V6; dependent Owner and Member/Cast UI migration remains.

The production deployment target and release checklist are documented in [docs/google-cloud-deployment.md](docs/google-cloud-deployment.md).

Pushes to `main` are prepared to run the full H2/MySQL test suite and deploy an immutable image to Cloud Run through keyless GitHub Actions Workload Identity Federation. The workflow is stored at [`.github/workflows/google-cloud-deploy.yml`](.github/workflows/google-cloud-deploy.yml).

An optional Cloudflare Worker edge gateway is prepared under `cloudflare/`.
It proxies to the existing Cloud Run service without moving Spring Boot or
Cloud SQL. Setup, validation, security boundaries, and rollback are documented
in [docs/cloudflare-deployment.md](docs/cloudflare-deployment.md).

The implemented controls and remaining security gates are documented in [docs/security-review.md](docs/security-review.md).

The production schema migration workflow is documented in [docs/database-migrations.md](docs/database-migrations.md).

The approved visual language, color tokens, components, and responsive UI rules are documented in [docs/ui-design-system.md](docs/ui-design-system.md).

The consolidated product requirements, scope, stakeholder context, and current decisions are documented in [PROJECT.md](PROJECT.md). Coding agents should follow [AGENTS.md](AGENTS.md). The recovered original synthesis remains available as historical reference in [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md).

## Development Roadmap

- [ ] Finalize functional requirements and acceptance criteria.
- [x] Design the Member Home, Schedule, Following, Profile, Cast Profile, and Event Detail interfaces.
- [x] Design the Owner Dashboard and Shift Review flow.
- [x] Complete the Owner Event Management interface.
- [x] Complete Owner Store Management interface.
- [x] Design the ER diagram and database schema.
- [x] Create the Spring Boot project.
- [x] Configure Spring Data JPA and H2.
- [x] Implement Member registration and database-backed authentication.
- [x] Implement the initial Member, Cast, and Store domain model.
- [x] Implement Store-Cast assignments and the Shift approval domain workflow.
- [x] Add role-based route and method authorization for Cast and Owner workspaces.
- [x] Implement Cast discovery and Member follow/unfollow workflow.
- [x] Implement Cast membership tiers and Member tier access checks.
- [x] Connect the real Member Home to approved shifts, published events, and follow state.
- [x] Connect the protected Member Schedule to approved shifts with date selection.
- [x] Connect Member Following and Profile to the existing follow and membership workflows.
- [x] Connect public Cast Profile and Event Detail, including calendar export.
- [x] Add the followed-Cast content feed and access-controlled Content Detail route.
- [x] Add Member profile editing with server-side validation and ownership protection.
- [x] Add the Member weekly schedule view with real approved-shift data and week navigation.
- [x] Add the Member Notification Center for followed-Cast updates and upcoming events.
- [x] Add persisted Member preferences for Cast-update and event notifications.
- [x] Complete the remaining Member workflow.
- [x] Implement the Cast profile, shift submission, cancellation, and membership tier workflow.
- [x] Implement Cast content publishing and tier-based visibility.
- [x] Implement the Owner Dashboard, daily schedule, and shift approval workflow.
- [x] Implement Owner Event Management with image uploads and draft, publish, and archive states.
- [x] Implement Owner Store Management and permanent Cast roster controls.
- [x] Implement Owner Promotion and Banner Management with scheduled public visibility.
- [x] Add the production security baseline for HTTPS, secure sessions, browser headers, and development-only routes.
- [x] Add the initial Flyway production schema and validate it against H2 MySQL mode and MySQL 8.4.
- [x] Add the store-wide Membership Points core, immutable ledger, tier boundaries, and Owner-only service contract in Flyway V6.
- [x] Add restricted production health and liveness endpoints for Cloud Run probes.
- [x] Connect Cloud SQL for MySQL through the Cloud SQL Java Connector.
- [x] Deploy the Spring Boot service to Google Cloud Run.
- [x] Store production credentials in Google Cloud Secret Manager.
- [ ] Restrict the first release to one cafe and remove exposed multi-store creation behavior.
- [ ] Replace Cast-specific tiers and Member self-subscription with the confirmed Owner-recorded, store-wide points model.
- [ ] Add validation, error handling, and broader automated tests.
- [ ] Complete security and responsive UI reviews.
- [ ] Complete production-readiness gates and prepare the project demonstration.

## Security Requirements

- Enforce authentication and authorization on every protected backend endpoint.
- Restrict data and actions by role and resource ownership.
- Hash passwords with a secure password encoder.
- Validate all input before saving it to the database.
- Protect against IDOR, XSS, CSRF, SQL injection, and brute-force sign-in attempts.
- Keep credentials and secrets out of source control.
- Validate the type and size of uploaded files.
- Record audit logs for important actions.

## Getting Started

### Prerequisites

- JDK 21
- Maven does not need to be installed separately because the repository includes Maven Wrapper.

### Run with H2

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Application health is available at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). The public response exposes only the aggregate status.

The development H2 Console is available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console). Use JDBC URL `jdbc:h2:mem:castfan`, username `sa`, and an empty password.

### Run Tests

```powershell
.\mvnw.cmd test
```

### Production Database

Set `SPRING_PROFILES_ACTIVE=prod` and the following environment variables before connecting to Cloud SQL for MySQL:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Production will run on Google Cloud Run and connect to Cloud SQL for MySQL. Credentials must be provided from Secret Manager rather than committed files. See [docs/google-cloud-deployment.md](docs/google-cloud-deployment.md) for the planned deployment process.

## Repository

[https://github.com/XerflyzDev/CastFan-System](https://github.com/XerflyzDev/CastFan-System)

## Academic Context

This project is developed for three courses:

- **Web Application Development** — Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, responsive interface design, security, testing, and deployment.
- **Software Requirements Analysis and Specification** — stakeholder analysis, system scope, functional and non-functional requirements, use cases, acceptance criteria, and requirements traceability.
- **Database Systems** — relational data modeling, keys and constraints, normalization, SQL database integration, schema migration, and data integrity.

The supplied Web Application Development grading and submission criteria are recorded in [docs/web-application-development-grading.md](docs/web-application-development-grading.md).
