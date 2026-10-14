# CastFan System

CastFan System is a web application for managing a maid cafe or concept cafe. It connects store owners (`Owner`), staff members (`Cast`), and customers (`Member`) in one system.

This project is being built from scratch for the **Web Application Development** course. It uses Spring Boot, Spring Data JPA, and H2/MySQL as required by the course.

## Project Language

English is the primary language for the user interface, system messages, documentation, source-code identifiers, and future project content. Localized languages may be added later as optional translations.

## Project Status

> **Member notification preferences integration**

The project currently includes the Spring Boot foundation, Maven Wrapper, H2/MySQL profiles, database-backed authentication, role profiles, Store and Cast scheduling domains, Member follow and membership workflows, and the approved mobile interface. Member Home, Notifications, day and week Schedule views, Following, Profile, public Cast Profile, public Content Detail, and public Event Detail use Spring Data JPA. The notification center aggregates access-controlled updates from followed Cast and upcoming published events while respecting each Member's persisted Cast-update and event-reminder preferences. Members can edit their display name, bio, avatar, and notification preferences through an ownership-protected profile route. The Member Home includes a feed from followed Cast with server-side audience and tier access checks. Cast can manage their public profile, submit or cancel shifts, manage membership tiers, and publish audience-controlled content. Owners can review store metrics, inspect schedules by date, approve or reject shift requests, manage stores and permanent Cast rosters, and publish events through ownership-protected routes. Published events also provide downloadable iCalendar files.

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
- Access content based on membership tier.

### Cast

- Manage their profile.
- Submit and update work schedules.
- Create membership tiers for followers.
- Manage members in each tier.
- Publish content and define access permissions.

### Owner

- View an overview of Cast members and store schedules.
- Approve or reject submitted schedules.
- Create and manage events.
- Create and manage promotions.
- Manage banners and public store information.

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
- `MemberProfile`
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

The production deployment target and release checklist are documented in [docs/google-cloud-deployment.md](docs/google-cloud-deployment.md).

The approved visual language, color tokens, components, and responsive UI rules are documented in [docs/ui-design-system.md](docs/ui-design-system.md).

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
- [x] Implement Owner Event Management with draft, publish, and archive states.
- [x] Implement Owner Store Management and permanent Cast roster controls.
- [ ] Connect Cloud SQL for MySQL.
- [ ] Deploy the Spring Boot service to Google Cloud Run.
- [ ] Store production credentials in Google Cloud Secret Manager.
- [ ] Add validation, error handling, and broader automated tests.
- [ ] Complete security and responsive UI reviews.
- [ ] Prepare deployment and the project demonstration.

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

This project is developed for the **Web Application Development** course. It demonstrates Spring Boot, Spring Data JPA, database connectivity, and interface design with Bootstrap and custom CSS.
