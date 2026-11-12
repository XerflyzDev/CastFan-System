# CastFan System

CastFan System is a multi-store web platform for cafes and venues with Cast staff. It connects Store Owners (`Owner`), store-specific staff accounts (`Cast`), and global customer accounts (`Member`) while isolating membership and operational data per Store.

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

The project includes the Spring Boot foundation, Maven Wrapper, H2/MySQL profiles, Flyway production migrations, database-backed authentication, multi-store onboarding, QR Store entry, StoreMembership isolation, store-specific Cast credentials, scheduling, following, content, event, promotion, and points workflows. Owners create one active Store and can download its permanent QR. Members retain one global account while joining multiple Stores independently. Cast accounts are issued by one Store Owner with a temporary password and forced first-login password change. User-facing image forms accept JPG, PNG, or WebP uploads up to 2 MB. Published Store data is always queried through a validated Store context.

Every active Cast profile is public and available for Member discovery and following. Cast can edit their public identity but cannot hide an active profile.

> **Requirement alignment notice:** The scope changed on 12 November 2026 to a multi-store platform. Owners open one active Store, Members enter through `/s/{slug}` QR links, and points, tier, following, notifications, content, shifts, events, and promotions are isolated by Store. Cast accounts are created by that Store's Owner and use a store-specific Cast ID.

## Objectives

- Simplify Cast shift submission and approval.
- Help Members find schedules and follow their favorite Cast.
- Support store events, promotions, and public information.
- Derive store-wide membership tiers from points and control access to Cast content.
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

Public Member registration always creates a `MEMBER`. `Open Your Store` atomically creates an `OWNER` and exactly one Store. There is no public role selector. Only the authenticated Store Owner creates or resets a store-specific `CAST` account.

Member and Cast identities are deliberately separate. When a Cast joins another Store, its new Owner creates a new Cast account; the former Store account and history remain isolated and can be deactivated.

## Core Data Model

Implemented and planned entities:

- `UserAccount`
- `AccountAvatar`
- `MemberProfile`
- `StoreMembership`
- `MemberPointTransaction`
- `CastProfile`
- `Store`
- `StoreCast`
- `ShiftSchedule`
- `CastFollow`
- `CastContent`
- `StoreEvent`
- `StorePromotion`

Entity details, relationships, constraints, and implementation order are documented in [docs/database-design.md](docs/database-design.md).

The confirmed store-wide points and tier workflow and service/view contract are documented in [docs/membership-points.md](docs/membership-points.md). Its Flyway V6 core, V9 legacy-schema removal, Owner workflow, Member views, and Cast content-access integration are implemented.

The production deployment target and release checklist are documented in [docs/google-cloud-deployment.md](docs/google-cloud-deployment.md).

Pushes to `main` are prepared to run the full H2/MySQL test suite and deploy an immutable image to Cloud Run through keyless GitHub Actions Workload Identity Federation. The workflow is stored at [`.github/workflows/google-cloud-deploy.yml`](.github/workflows/google-cloud-deploy.yml).

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
- [x] Implement store-wide membership points, derived tiers, and content access checks.
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
- [x] Implement the Cast profile, shift submission, cancellation, and content workflow.
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
- [x] Support multiple independently scoped stores, with one store per Owner and QR-based Member entry.
- [x] Use the confirmed Owner-recorded, store-wide points model as the only membership workflow.
- [ ] Add validation, error handling, and broader automated tests.
- [x] Complete security and responsive UI reviews.
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
