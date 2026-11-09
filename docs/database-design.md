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
- An Owner-authorized Member-to-Cast promotion updates the account role, retains the existing Member profile, creates one Cast profile, and creates the system-store roster assignment in one transaction.
- Passwords are stored only as secure hashes.

### AccountAvatar

An optional profile photo shared by every account role and stored separately so authentication queries do not load image data.

Key fields: `userAccountId`, `contentType`, `imageData`, `updatedAt`.

Constraints:

- `userAccountId` is both the primary key and a foreign key to `UserAccount`.
- Only validated JPG, PNG, or WebP files up to 2 MB are accepted.
- Only the authenticated account can replace or remove its photo; public retrieval is limited to active accounts.

### MemberProfile

Member-specific public information. Each user can have at most one Member profile.

Key fields: `bio`, `avatarUrl`, `castUpdateNotifications`, `eventNotifications`, `pointsBalance`.

Constraints: unique `userId`; `pointsBalance >= 0`. Notification preferences default to enabled and the point balance defaults to zero for new Member profiles.

The store-wide points core is implemented as follows:

- Store accumulated purchase points on the Member profile and record every change in an auditable point ledger.
- New Members start with zero points and no tier.
- A separate membership number is not required.
- Only the authenticated Owner records points after checking the bill outside CastFan; no bill, payment, or point-of-sale record is managed by CastFan.
- Award points at `1 THB = 1 point`; points do not expire.
- Derive store-wide tiers as Tier 1 at `1-4,999`, Tier 2 at `5,000-19,999`, and Tier 3 at `20,000+` points.
- Store-wide point balance is the only source of membership tier status.
- The workflow and service contract are specified in [`membership-points.md`](membership-points.md).

### MemberPointTransaction

An append-only ledger entry for every store-wide point change.

Key fields: `memberProfileId`, `createdByUserId`, `type`, `purchaseAmount`, `pointsDelta`, `receiptReference`, `receiptReferenceKey`, `reason`, `originalTransactionId`, `createdAt`.

Rules:

- `type` is `EARN`, `CORRECTION`, or `REVERSAL`.
- An `EARN` entry requires a positive purchase amount and a globally unique normalized receipt reference for the one-cafe release.
- A correction or reversal requires a reason and a link to the original `EARN` entry.
- An original earned entry can be linked to at most one correction or reversal.
- Completed entries are never updated or deleted by the membership service.
- The authenticated active Owner account is stored as the actor.
- A pessimistic lock protects the Member balance while the ledger entry and balance update are committed in one transaction.

### CastProfile

Public profile settings for a Cast. Each active Cast profile is public, and each user can have at most one Cast profile. The legacy `discoverable` column is retained for migration compatibility and is normalized to `TRUE`.

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

### CastContent

Content published by a Cast. `minimumStoreTierRank` is the required access field for store-tier content.

Rules:

- Only the owning Cast can edit, publish, archive, or delete a draft.
- Published content requires `publishedAt`.
- `audience` is `PUBLIC`, `FOLLOWERS`, or `MEMBERS`; member content requires `minimumStoreTierRank`.
- Members with a tier rank equal to or higher than the required tier can read the full content.
- `minimumStoreTierRank`, when present, is between `1` and `3`.
- Archived content is retained for audit history and is excluded from public profile queries.

Recommended index: `(castProfileId, status, publishedAt)`.

### CastContentImage

An optional uploaded image stored separately from Cast content.

Rules:

- `castContentId` is both the primary key and a foreign key to `CastContent`.
- Only validated JPG, PNG, or WebP files up to 2 MB are accepted.
- The owning Cast can manage the image; public reads follow the content audience and publication rules.

### StoreEvent

A store event created by an Owner.

Rules:

- `endsAt` must be after `startsAt`.
- `createdByUserId` must own the referenced store.
- Public queries return only `PUBLISHED` events.

Recommended index: `(storeId, status, startsAt)`.

### StoreEventImage

An optional uploaded image stored separately so event list queries do not load binary data.

Rules:

- `storeEventId` is both the primary key and a foreign key to `StoreEvent`.
- Only validated JPG, PNG, or WebP files up to 2 MB are accepted.
- Owners can read images only for events belonging to their stores.
- Public image access requires a `PUBLISHED` event and an active Store.

### StorePromotion

A time-bounded store promotion created by an Owner.

Key fields: `title`, `description`, optional globally unique `code`, optional `bannerUrl`, `startsAt`, `endsAt`, and `status`.

Rules:

- `endsAt` must be after `startsAt`.
- A non-null promotion `code` is globally unique in the first version.
- `createdByUserId` must own the referenced store.
- Public queries return only `ACTIVE` promotions whose time range includes the current instant and whose Store is active.

Recommended index: `(storeId, status, startsAt, endsAt)`.

### StorePromotionImage

An optional uploaded promotion image stored separately from campaign metadata.

Rules:

- `storePromotionId` is both the primary key and a foreign key to `StorePromotion`.
- Only validated JPG, PNG, or WebP files up to 2 MB are accepted.
- The Store owner can manage the image; public reads require a currently active promotion and active Store.

## Relationship Summary

| Parent | Relationship | Child |
| --- | --- | --- |
| UserAccount | one to zero-or-one | MemberProfile |
| UserAccount | one to zero-or-one | CastProfile |
| UserAccount | one to zero-or-one | AccountAvatar |
| UserAccount | one to many | Store |
| UserAccount | one to many | MemberPointTransaction as actor |
| Store | one to many | StoreCast |
| CastProfile | one to many | StoreCast |
| StoreCast | one to many | ShiftSchedule |
| MemberProfile | one to many | CastFollow |
| MemberProfile | one to many | MemberPointTransaction |
| CastProfile | one to many | CastFollow |
| CastProfile | one to many | CastContent |
| CastContent | one to zero-or-one | CastContentImage |
| Store | one to many | StoreEvent |
| StoreEvent | one to zero-or-one | StoreEventImage |
| Store | one to many | StorePromotion |
| StorePromotion | one to zero-or-one | StorePromotionImage |

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
5. `MemberPointTransaction`, store-tier derivation, and access checks.
6. `CastContent`, `StoreEvent`, and `StorePromotion`.
7. Flyway production migration and automated H2 MySQL-mode schema validation. Completed through schema version 9.
8. Run the migration and Hibernate validation against a disposable MySQL database before the next Cloud SQL deployment.
