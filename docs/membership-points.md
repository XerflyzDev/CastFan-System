# Membership Points And Tier Workflow

This document defines CastFan's confirmed per-store membership program. The points balance, immutable ledger, tier derivation, Owner workflow, Member summary/history, and store-tier content access are isolated for each store membership.

## Confirmed Requirements

- A Member can use one global account across stores, but receives a separate membership in each store.
- Every newly registered Member starts with `0` points and no tier (`NONE`).
- The cafe checks and settles the bill outside CastFan.
- Only an authenticated `OWNER` records points in CastFan after the bill has been checked.
- The conversion rate is `1 THB = 1 point`.
- Points do not expire.
- Tier status belongs to the store membership, not to an individual Cast.
- Members do not choose or subscribe to a tier themselves.
- Members cannot create, buy, select, or subscribe to membership tiers.
- A separate membership number is not required.

## Tier Thresholds

| Tier | Point range |
| --- | ---: |
| None | 0 |
| Tier 1 | 1-4,999 |
| Tier 2 | 5,000-19,999 |
| Tier 3 | 20,000 or more |

The system derives the current tier from the Member's point balance. A tier must not be selected or edited independently from that balance.

## Recommended Point-Earning Flow

1. The Owner signs in and opens Membership Management.
2. The Owner searches for and selects the Member account.
3. The Owner enters the paid amount in THB and a receipt or transaction reference.
4. The system validates that the amount is positive and that the receipt reference has not already been used.
5. The system previews the points to add, the resulting balance, and any tier change.
6. The Owner confirms the entry.
7. In one database transaction, the system creates an immutable point-ledger entry, updates the Member's point balance, and derives the new tier.
8. The system shows a success result containing the Member, points added, new balance, and current tier.
9. The Member can view their balance, current tier, progress to the next tier, and point history.

## Recommended Correction And Refund Flow

Point history should remain auditable. An Owner must not edit or delete a completed ledger entry.

1. The Owner opens the Member's point history and selects the original entry.
2. The Owner chooses **Reverse** or **Correct** and provides a required reason.
3. The system previews the negative or replacement point adjustment and resulting tier.
4. After confirmation, the system appends a linked adjustment entry instead of changing the original record.
5. The balance must never become negative.
6. The system recalculates the tier immediately, including a downgrade when the corrected balance crosses a threshold.

## Implemented Core Data Model

### MemberProfile

The legacy `MemberProfile.pointsBalance` value is migrated into the first store membership. Current balances and notification preferences belong to `StoreMembership`, where `pointsBalance` is a required `BIGINT`, defaults to `0`, and cannot be negative. The current tier is derived from that store-specific balance rather than independently selected by a Member.

### MemberPointTransaction

An immutable ledger entry for every point change.

Implemented fields:

- `id`
- `storeMembershipId` and `storeId`
- `type`: `EARN`, `CORRECTION`, or `REVERSAL`
- `purchaseAmount`: nullable for manual corrections
- `pointsDelta`: positive or negative integer
- `receiptReference`: required for earned purchase points and unique within that store
- `reason`: required for corrections and reversals
- `originalTransactionId`: nullable self-reference to the original entry
- `createdByUserId`: the authenticated Owner
- `createdAt`

### StoreMembershipTier

The core uses the fixed `StoreMembershipTier` definition `NONE`, `TIER_1`, `TIER_2`, and `TIER_3`, with ranks and thresholds controlled in code. This prevents clients from editing or assigning tiers independently from the balance.

### CastContent

`minimumStoreTierRank` is required when the audience is `MEMBERS` and must be absent for public or follower-only content. `CastContent.forStoreTier(...)` and `updateStoreTierDetails(...)` provide the entity contract for the replacement workflow. The legacy field remains temporarily so existing screens compile, but legacy writes also populate the store-tier rank and it is no longer sufficient by itself at the database boundary.

## Service And View Contract

`MembershipService` exposes the UI-independent contract used by the dependent Owner and Member work:

- `previewEarnPoints(ownerLogin, memberProfileId, paidAmount, receiptReference)` resolves the Owner's store and validates the request without mutation.
- `recordPurchasePoints(...)` appends an `EARN` entry and updates the locked balance atomically.
- `previewCorrection(...)`, `correctPurchasePoints(...)`, and `reversePurchasePoints(...)` preserve the original entry and return the resulting balance and tier.
- `memberPoints(memberLogin, storeSlug)` returns only the authenticated Member's summary and history for that store.
- `memberPointsForOwner(ownerLogin, memberProfileId)` resolves the Owner's store and returns only that store membership.
- `hasStoreTierAccess(memberLogin, storeId, minimumRank)` is the backend access check for store-tier content.

The preview result includes the current balance/tier, point delta, and resulting balance/tier. A confirmed result includes the immutable transaction view, resulting balance, and resulting tier. Controllers pass the authenticated principal email; they never accept an Owner ID as proof of authority.

An original earned entry may receive one correction or reversal. A second adjustment is rejected so concurrent requests cannot apply the same purchase twice.

## Security And Integrity Rules

- Only an authenticated active Owner may add, reverse, or correct points.
- Resolve the acting Owner from the authenticated principal; never accept an Owner ID as proof of authority.
- Every point change must create a ledger entry containing actor and timestamp.
- Use one transaction to append the ledger entry and update the balance.
- Reject duplicate receipt references within the same store to prevent awarding the same purchase twice.
- Reject every cross-store Member, Cast, content, follow, and ledger reference in both service logic and database constraints.
- Reject changes that would produce a negative balance.
- Member access is read-only and limited to their own balance and history.
- Do not store payment-card data or complete bill details in CastFan.

## Acceptance Criteria

- A new Member has `0` points and tier `NONE`.
- An Owner recording a 4,999 THB purchase results in 4,999 points and Tier 1.
- A Member reaches Tier 2 at exactly 5,000 points.
- A Member reaches Tier 3 at exactly 20,000 points.
- Members cannot add points or select a tier.
- Cast cannot add points, create tiers, or assign Members to tiers.
- Reusing a receipt reference is rejected.
- A correction keeps the original ledger entry and records the Owner and reason.
- Points remain unchanged over time unless an Owner records another transaction.
- Tier-restricted Cast content uses the Member's store-wide tier.

## Confirmed Implementation Details

- Award whole-number points by rounding the paid THB amount down; for example, `199.75 THB` awards `199` points.
- Require a receipt reference for purchase entries.
- Apply tier downgrades immediately after a correction or reversal reduces the balance.
- Show the latest point transactions to Members but keep Owner-only notes private.

