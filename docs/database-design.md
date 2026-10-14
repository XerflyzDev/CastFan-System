# CastFan Database Design

This document is the source of truth for the first CastFan relational schema. The model is designed for Spring Data JPA and must remain compatible with both embedded H2 and Cloud SQL for MySQL.

## Design Principles

- Use `BIGINT` generated identifiers for all primary keys.
- Store enum values as readable strings rather than ordinal numbers.
- Store timestamps in UTC and use `Store.timezone` only for display and schedule input.
- Keep authentication data in `UserAccount`; role-specific data belongs in profile tables.
- Represent many-to-many relationships with explicit junction entities.
- Use status fields instead of deleting records needed for history or auditing.
- Enforce ownership and role checks in the service layer, not only in the UI.

## Entities

### UserAccount

Authentication identity shared by every role.

Key fields: `email`, `passwordHash`, `displayName`, `role`, `status`, timestamps.

Constraints:

- `email` is required, normalized to lowercase, and unique.
- `role` is one of `MEMBER`, `CAST`, or `OWNER`.
- Passwords are stored only as secure hashes.

### MemberProfile

Member-specific public information. Each user can have at most one Member profile.

Constraint: unique `userId`.

### CastProfile

Cast-specific public profile and discoverability settings. Each user can have at most one Cast profile.

Constraints:

- Unique `userId`.
- Unique normalized `stageName`.

### Store

A cafe managed by one Owner account. One Owner may manage multiple stores.

Constraints:

- Unique URL-safe `slug`.
- `ownerUserId` must reference an active `OWNER` account.
- `timezone` uses an IANA identifier such as `Asia/Bangkok`.
- Store removal is represented by `active = false`; historical schedules and events are retained.

### StoreCast

Connects a Cast profile to a store and keeps store-specific employment information.

Constraint: unique pair `(storeId, castProfileId)`.

Rules:

- An inactive assignment remains stored for schedule history and can be reactivated by the Store owner.
- New shifts require both an active assignment and an active Store.

### ShiftSchedule

A shift submitted for a `StoreCast` assignment and optionally reviewed by an Owner.

Rules:

- `endsAt` must be after `startsAt`.
- Only the assigned Cast can submit or cancel the shift.
- Only an Owner of the related store can approve or reject it.
- Approved shifts for the same StoreCast must not overlap.
- `reviewedByUserId`, `reviewedAt`, and `reviewNote` remain nullable until review.

Recommended indexes: `(storeCastId, startsAt)`, `(status, startsAt)`.

### CastFollow

Records a Member following a Cast.

Constraint: unique pair `(memberProfileId, castProfileId)`.

### MemberTier

A Cast-owned membership tier ordered by `rank`.

Constraints:

- Unique pair `(castProfileId, name)`.
- Unique pair `(castProfileId, rank)`.
- `rank` is positive and `monthlyPrice` is not negative.

### TierMembership

Records a Member's subscription to a Cast membership tier.

Constraint: unique pair `(memberProfileId, memberTierId)` for the initial implementation.

The service must ensure that a Member has no more than one active tier for the same Cast.

### CastContent

Content published by a Cast. `minimumTierId` is nullable for public followers-only content.

Rules:

- The selected minimum tier must belong to the same Cast as the content.
- Only the owning Cast can edit, publish, archive, or delete a draft.
- Published content requires `publishedAt`.
- `audience` is `PUBLIC`, `FOLLOWERS`, or `MEMBERS`; member content requires `minimumTierId`.
- Members with a tier rank equal to or higher than the required tier can read the full content.
- Archived content is retained for audit history and is excluded from public profile queries.

Recommended index: `(castProfileId, status, publishedAt)`.

### StoreEvent

A store event created by an Owner.

Rules:

- `endsAt` must be after `startsAt`.
- `createdByUserId` must own the referenced store.
- Public queries return only `PUBLISHED` events.

Recommended index: `(storeId, status, startsAt)`.

### StorePromotion

A time-bounded store promotion created by an Owner.

Rules:

- `endsAt` must be after `startsAt`.
- A non-null promotion `code` is globally unique in the first version.
- `createdByUserId` must own the referenced store.

Recommended index: `(storeId, active, startsAt, endsAt)`.

## Relationship Summary

| Parent | Relationship | Child |
| --- | --- | --- |
| UserAccount | one to zero-or-one | MemberProfile |
| UserAccount | one to zero-or-one | CastProfile |
| UserAccount | one to many | Store |
| Store | one to many | StoreCast |
| CastProfile | one to many | StoreCast |
| StoreCast | one to many | ShiftSchedule |
| MemberProfile | one to many | CastFollow |
| CastProfile | one to many | CastFollow |
| CastProfile | one to many | MemberTier |
| MemberProfile | one to many | TierMembership |
| MemberTier | one to many | TierMembership |
| CastProfile | one to many | CastContent |
| MemberTier | optional one to many | CastContent |
| Store | one to many | StoreEvent |
| Store | one to many | StorePromotion |

## Security Boundaries

- Never accept an owner, Cast, or Member identifier from the client as proof of ownership.
- Resolve the authenticated `UserAccount` on the server and derive its profile identifiers.
- Every update and delete query must include ownership or store authorization checks.
- Return DTOs instead of exposing JPA entities directly.
- Do not serialize `passwordHash` in any response or view model.
- Apply pagination and visibility filters to Cast content, events, and promotions.
- Use transactions for shift approval, follow creation, and membership changes.

## Implementation Order

1. `UserAccount`, role/status enums, and authentication repository.
2. `MemberProfile`, `CastProfile`, and `Store`.
3. `StoreCast` and `ShiftSchedule` with approval rules.
4. `CastFollow` and Member discovery queries.
5. `MemberTier`, `TierMembership`, and access checks.
6. `CastContent`, `StoreEvent`, and `StorePromotion`.
7. Database migrations, integration tests, and production MySQL validation.
