-- Multi-store foundation. Existing CastFan data is preserved as the first store.

ALTER TABLE user_accounts ADD COLUMN login_identifier VARCHAR(254);
ALTER TABLE user_accounts ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE user_accounts SET login_identifier = LOWER(email) WHERE login_identifier IS NULL;
ALTER TABLE user_accounts MODIFY email VARCHAR(254) NULL;
ALTER TABLE user_accounts MODIFY login_identifier VARCHAR(254) NOT NULL;
ALTER TABLE user_accounts ADD CONSTRAINT uk_user_accounts_login_identifier UNIQUE (login_identifier);

UPDATE stores
SET slug = 'castfan'
WHERE id = (SELECT legacy.id FROM (SELECT MIN(id) AS id FROM stores) legacy)
  AND NOT EXISTS (SELECT 1 FROM (SELECT slug FROM stores) existing WHERE existing.slug = 'castfan');

ALTER TABLE stores
    ADD CONSTRAINT uk_stores_owner UNIQUE (owner_user_account_id);

CREATE TABLE store_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    member_profile_id BIGINT NOT NULL,
    points_balance BIGINT NOT NULL DEFAULT 0,
    cast_update_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    event_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_membership_store_member UNIQUE (store_id, member_profile_id),
    CONSTRAINT uk_store_membership_id_store UNIQUE (id, store_id),
    CONSTRAINT chk_store_membership_points CHECK (points_balance >= 0),
    CONSTRAINT chk_store_membership_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_store_membership_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_membership_member FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id)
);

INSERT INTO store_memberships (
    store_id, member_profile_id, points_balance, cast_update_notifications,
    event_notifications, status, joined_at, updated_at
)
SELECT legacy_store.id, profile.id, profile.points_balance,
       profile.cast_update_notifications, profile.event_notifications,
       'ACTIVE', profile.created_at, profile.updated_at
FROM member_profiles profile
CROSS JOIN (SELECT MIN(id) AS id FROM stores) legacy_store;

ALTER TABLE member_point_transactions ADD COLUMN store_id BIGINT;
ALTER TABLE member_point_transactions ADD COLUMN store_membership_id BIGINT;

UPDATE member_point_transactions
SET store_id = (SELECT MIN(store_id) FROM store_memberships
                WHERE store_memberships.member_profile_id = member_point_transactions.member_profile_id),
    store_membership_id = (SELECT MIN(id) FROM store_memberships
                           WHERE store_memberships.member_profile_id = member_point_transactions.member_profile_id);

ALTER TABLE member_point_transactions DROP INDEX uk_member_point_receipt_key;
ALTER TABLE member_point_transactions MODIFY store_id BIGINT NOT NULL;
ALTER TABLE member_point_transactions MODIFY store_membership_id BIGINT NOT NULL;
ALTER TABLE member_point_transactions ADD CONSTRAINT uk_member_point_store_receipt UNIQUE (store_id, receipt_reference_key);
ALTER TABLE member_point_transactions ADD CONSTRAINT fk_member_point_store FOREIGN KEY (store_id) REFERENCES stores (id);
ALTER TABLE member_point_transactions ADD CONSTRAINT fk_member_point_membership_store
    FOREIGN KEY (store_membership_id, store_id) REFERENCES store_memberships (id, store_id);
CREATE INDEX idx_member_point_membership_created
    ON member_point_transactions (store_membership_id, created_at);

ALTER TABLE cast_follows ADD COLUMN store_id BIGINT;
ALTER TABLE cast_follows ADD COLUMN store_membership_id BIGINT;

UPDATE cast_follows
SET store_id = (SELECT MIN(store_id) FROM store_cast
                WHERE store_cast.cast_profile_id = cast_follows.cast_profile_id);
UPDATE cast_follows
SET store_membership_id = (SELECT MIN(id) FROM store_memberships
    WHERE store_memberships.member_profile_id = cast_follows.member_profile_id
      AND store_memberships.store_id = cast_follows.store_id);

ALTER TABLE cast_follows DROP INDEX uk_cast_follow_member_cast;
ALTER TABLE cast_follows MODIFY store_id BIGINT NOT NULL;
ALTER TABLE cast_follows MODIFY store_membership_id BIGINT NOT NULL;
ALTER TABLE cast_follows ADD CONSTRAINT uk_cast_follow_membership_cast UNIQUE (store_membership_id, cast_profile_id);
ALTER TABLE cast_follows ADD CONSTRAINT fk_cast_follow_membership_store
    FOREIGN KEY (store_membership_id, store_id) REFERENCES store_memberships (id, store_id);
ALTER TABLE cast_follows ADD CONSTRAINT fk_cast_follow_store FOREIGN KEY (store_id) REFERENCES stores (id);

ALTER TABLE cast_profiles ADD COLUMN store_id BIGINT;
UPDATE cast_profiles
SET store_id = (SELECT MIN(store_id) FROM store_cast WHERE store_cast.cast_profile_id = cast_profiles.id);
ALTER TABLE cast_profiles DROP INDEX uk_cast_profile_stage_name;
ALTER TABLE cast_profiles MODIFY store_id BIGINT NOT NULL;
ALTER TABLE cast_profiles ADD CONSTRAINT uk_cast_profile_id_store UNIQUE (id, store_id);
ALTER TABLE cast_profiles ADD CONSTRAINT uk_cast_profile_store_stage UNIQUE (store_id, stage_name_key);
ALTER TABLE cast_profiles ADD CONSTRAINT fk_cast_profile_store FOREIGN KEY (store_id) REFERENCES stores (id);

-- A Cast account belongs to exactly one store. These composite foreign keys make
-- cross-store roster, follow, and content references fail at the database boundary.
ALTER TABLE store_cast ADD CONSTRAINT fk_store_cast_profile_store
    FOREIGN KEY (cast_profile_id, store_id) REFERENCES cast_profiles (id, store_id);
ALTER TABLE cast_follows ADD CONSTRAINT fk_cast_follow_profile_store
    FOREIGN KEY (cast_profile_id, store_id) REFERENCES cast_profiles (id, store_id);

ALTER TABLE cast_content ADD COLUMN store_id BIGINT;
UPDATE cast_content
SET store_id = (SELECT store_id FROM cast_profiles WHERE cast_profiles.id = cast_content.cast_profile_id);
ALTER TABLE cast_content MODIFY store_id BIGINT NOT NULL;
ALTER TABLE cast_content ADD CONSTRAINT fk_cast_content_store FOREIGN KEY (store_id) REFERENCES stores (id);
ALTER TABLE cast_content ADD CONSTRAINT fk_cast_content_profile_store
    FOREIGN KEY (cast_profile_id, store_id) REFERENCES cast_profiles (id, store_id);
CREATE INDEX idx_content_store_status_published
    ON cast_content (store_id, status, published_at);
