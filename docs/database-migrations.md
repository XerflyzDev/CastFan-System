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
- `V2__add_account_avatars.sql` adds role-neutral profile photo storage linked one-to-one with user accounts.
- `V3__add_store_event_images.sql` adds uploaded event image storage linked one-to-one with store events.
- `V4__add_content_and_promotion_images.sql` adds uploaded Cast content and promotion image storage.
- `V5__add_cast_audience_profiles.sql` gives existing Cast accounts the audience profile required for schedules and following.
- `V6__add_store_membership_points.sql` introduces store-wide Member points and the immutable point ledger.
- `V7__require_store_tier_for_member_content.sql` requires a store tier for membership-only content.
- `V8__make_all_cast_profiles_public.sql` makes active Cast profiles discoverable.
- `V9__remove_legacy_cast_memberships.sql` removes the retired Cast-specific tier model.
- `V10__add_store_promotion_status.sql` introduces the promotion lifecycle status.
- `V11__remove_store_promotion_active.sql` removes the superseded promotion active flag.
- `V12__introduce_multi_store_identity_and_memberships.sql` migrates existing data to store-scoped identities, memberships, following, points, and content.

Never edit a versioned migration after it has been applied to a shared database. Add a new migration such as `V6__add_audit_log.sql` for every later schema change.

## Local Verification

Run the migration-focused test:

```powershell
.\mvnw.cmd -Dtest=DatabaseMigrationTests test
```

Run the complete test suite before committing:

```powershell
.\mvnw.cmd test
```

### Real MySQL Verification

Start a disposable MySQL 8.4 container:

```powershell
docker run --rm -d --name castfan-mysql-test `
  -e MYSQL_DATABASE=castfan `
  -e MYSQL_USER=castfan `
  -e MYSQL_PASSWORD=castfan_test `
  -e MYSQL_ROOT_PASSWORD=root_test `
  -p 127.0.0.1:3307:3306 mysql:8.4
```

Run the opt-in MySQL integration test after the database is ready:

```powershell
$env:CASTFAN_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:3307/castfan'
$env:CASTFAN_MYSQL_TEST_USERNAME='castfan'
$env:CASTFAN_MYSQL_TEST_PASSWORD='castfan_test'
.\mvnw.cmd -Dtest=DatabaseMySqlMigrationTests test
```

Stop the disposable database when verification is complete:

```powershell
docker stop castfan-mysql-test
```

## MySQL Release Gate

Before every migration-bearing Render/Aiven deployment, run `DatabaseMySqlMigrationTests` against a new disposable MySQL database. Startup must apply every Flyway migration successfully and Hibernate validation must complete without schema errors. This real-MySQL check is required because H2 compatibility mode cannot reproduce every MySQL behavior.

The GitHub CI workflow runs this gate against MySQL 8.4 for every pull request to `main`. Repeat it whenever a migration is added or the production MySQL version changes.

Production cleanup remains disabled with `spring.flyway.clean-disabled=true`. Do not use Flyway baseline options unless adopting a pre-existing database has been reviewed and documented.
