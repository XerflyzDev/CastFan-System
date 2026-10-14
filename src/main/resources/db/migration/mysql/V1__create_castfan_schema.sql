CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_email UNIQUE (email),
    CONSTRAINT chk_user_accounts_role CHECK (role IN ('MEMBER', 'CAST', 'OWNER')),
    CONSTRAINT chk_user_accounts_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE member_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    bio VARCHAR(500),
    avatar_url VARCHAR(2048),
    cast_update_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    event_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_profile_user UNIQUE (user_account_id),
    CONSTRAINT fk_member_profile_user FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
);

CREATE TABLE cast_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT NOT NULL,
    stage_name VARCHAR(80) NOT NULL,
    stage_name_key VARCHAR(100) NOT NULL,
    bio VARCHAR(1000),
    avatar_url VARCHAR(2048),
    discoverable BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cast_profile_user UNIQUE (user_account_id),
    CONSTRAINT uk_cast_profile_stage_name UNIQUE (stage_name_key),
    CONSTRAINT fk_cast_profile_user FOREIGN KEY (user_account_id) REFERENCES user_accounts (id)
);

CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_account_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    address VARCHAR(500),
    timezone VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stores_slug UNIQUE (slug),
    CONSTRAINT fk_store_owner FOREIGN KEY (owner_user_account_id) REFERENCES user_accounts (id)
);

CREATE TABLE store_cast (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    cast_profile_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_cast_assignment UNIQUE (store_id, cast_profile_id),
    CONSTRAINT fk_store_cast_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_cast_profile FOREIGN KEY (cast_profile_id) REFERENCES cast_profiles (id)
);

CREATE TABLE shift_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_cast_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_by_user_id BIGINT,
    reviewed_at DATETIME(6),
    review_note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_shift_range CHECK (ends_at > starts_at),
    CONSTRAINT chk_shift_status CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_shift_store_cast FOREIGN KEY (store_cast_id) REFERENCES store_cast (id),
    CONSTRAINT fk_shift_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES user_accounts (id)
);
CREATE INDEX idx_shift_assignment_start ON shift_schedules (store_cast_id, starts_at);
CREATE INDEX idx_shift_status_start ON shift_schedules (status, starts_at);

CREATE TABLE cast_follows (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_profile_id BIGINT NOT NULL,
    cast_profile_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cast_follow_member_cast UNIQUE (member_profile_id, cast_profile_id),
    CONSTRAINT fk_cast_follow_member FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id),
    CONSTRAINT fk_cast_follow_cast FOREIGN KEY (cast_profile_id) REFERENCES cast_profiles (id)
);

CREATE TABLE member_tiers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cast_profile_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    name_key VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    rank_number INTEGER NOT NULL,
    monthly_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_tier_name UNIQUE (cast_profile_id, name_key),
    CONSTRAINT uk_member_tier_rank UNIQUE (cast_profile_id, rank_number),
    CONSTRAINT chk_member_tier_values CHECK (rank_number > 0 AND monthly_price >= 0),
    CONSTRAINT fk_member_tier_cast FOREIGN KEY (cast_profile_id) REFERENCES cast_profiles (id)
);

CREATE TABLE tier_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_profile_id BIGINT NOT NULL,
    member_tier_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tier_membership_member_tier UNIQUE (member_profile_id, member_tier_id),
    CONSTRAINT chk_membership_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT fk_membership_member FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id),
    CONSTRAINT fk_membership_tier FOREIGN KEY (member_tier_id) REFERENCES member_tiers (id)
);

CREATE TABLE cast_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cast_profile_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(5000) NOT NULL,
    image_url VARCHAR(2048),
    audience VARCHAR(20) NOT NULL,
    minimum_tier_id BIGINT,
    status VARCHAR(20) NOT NULL,
    published_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_content_audience CHECK (audience IN ('PUBLIC', 'FOLLOWERS', 'MEMBERS')),
    CONSTRAINT chk_content_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_content_tier CHECK (
        (audience = 'MEMBERS' AND minimum_tier_id IS NOT NULL)
        OR (audience <> 'MEMBERS' AND minimum_tier_id IS NULL)
    ),
    CONSTRAINT fk_content_cast FOREIGN KEY (cast_profile_id) REFERENCES cast_profiles (id),
    CONSTRAINT fk_content_tier FOREIGN KEY (minimum_tier_id) REFERENCES member_tiers (id)
);
CREATE INDEX idx_content_cast_status_published ON cast_content (cast_profile_id, status, published_at);

CREATE TABLE store_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    image_url VARCHAR(2048),
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_store_event_range CHECK (ends_at > starts_at),
    CONSTRAINT chk_store_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT fk_store_event_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_event_creator FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (id)
);
CREATE INDEX idx_store_event_status_start ON store_events (store_id, status, starts_at);

CREATE TABLE store_promotions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    code VARCHAR(64),
    banner_url VARCHAR(2048),
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_promotion_code UNIQUE (code),
    CONSTRAINT chk_store_promotion_range CHECK (ends_at > starts_at),
    CONSTRAINT fk_store_promotion_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_promotion_creator FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (id)
);
CREATE INDEX idx_store_promotion_active_range ON store_promotions (store_id, active, starts_at, ends_at);
