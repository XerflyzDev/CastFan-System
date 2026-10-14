# Database Migrations

Flyway owns the production database schema. Hibernate validates that the JPA mappings match the migrated schema and must not create or update production tables.

## Profile Behavior

| Profile | Database | Flyway | Hibernate schema mode |
| --- | --- | --- | --- |
| `dev` | Embedded H2 | Disabled | `update` |
| `test` | Isolated H2 | Disabled by default | `create-drop` |
| `prod` | MySQL | Enabled | `validate` |

The dedicated `DatabaseMigrationTests` test enables Flyway against H2 in MySQL compatibility mode, applies every MySQL migration, checks the expected tables and current version, and starts Hibernate with `ddl-auto=validate`.

## Migration Files

Production migrations live in `src/main/resources/db/migration/mysql` and use Flyway's versioned naming convention.

- `V1__create_castfan_schema.sql` creates the initial 12 application tables, relationships, constraints, and query indexes.

Never edit a versioned migration after it has been applied to a shared database. Add a new migration such as `V2__add_audit_log.sql` for every later schema change.

## Local Verification

Run the migration-focused test:

```powershell
.\mvnw.cmd -Dtest=DatabaseMigrationTests test
```

Run the complete test suite before committing:

```powershell
.\mvnw.cmd test
```

## MySQL Release Gate

Before the first Cloud SQL deployment, run the application with the `prod` profile against a new disposable MySQL database. Startup must apply every Flyway migration successfully and Hibernate validation must complete without schema errors. This real-MySQL check is required because H2 compatibility mode cannot reproduce every MySQL behavior.

Production cleanup remains disabled with `spring.flyway.clean-disabled=true`. Do not use Flyway baseline options unless adopting a pre-existing database has been reviewed and documented.
