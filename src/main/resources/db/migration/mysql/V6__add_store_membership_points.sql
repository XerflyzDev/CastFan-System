ALTER TABLE member_profiles
    ADD COLUMN points_balance BIGINT NOT NULL DEFAULT 0;

ALTER TABLE member_profiles
    ADD CONSTRAINT chk_member_points_balance CHECK (points_balance >= 0);

CREATE TABLE member_point_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_profile_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    purchase_amount DECIMAL(12, 2),
    points_delta BIGINT NOT NULL,
    receipt_reference VARCHAR(120),
    receipt_reference_key VARCHAR(160),
    reason VARCHAR(500),
    original_transaction_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_point_receipt_key UNIQUE (receipt_reference_key),
    CONSTRAINT uk_member_point_original UNIQUE (original_transaction_id),
    CONSTRAINT chk_member_point_type CHECK (type IN ('EARN', 'CORRECTION', 'REVERSAL')),
    CONSTRAINT chk_member_point_fields CHECK (
        (type = 'EARN'
            AND purchase_amount IS NOT NULL
            AND purchase_amount > 0
            AND points_delta >= 0
            AND receipt_reference IS NOT NULL
            AND receipt_reference_key IS NOT NULL
            AND reason IS NULL
            AND original_transaction_id IS NULL)
        OR (type = 'CORRECTION'
            AND purchase_amount IS NOT NULL
            AND purchase_amount > 0
            AND receipt_reference IS NULL
            AND receipt_reference_key IS NULL
            AND reason IS NOT NULL
            AND original_transaction_id IS NOT NULL)
        OR (type = 'REVERSAL'
            AND purchase_amount IS NULL
            AND points_delta <= 0
            AND receipt_reference IS NULL
            AND receipt_reference_key IS NULL
            AND reason IS NOT NULL
            AND original_transaction_id IS NOT NULL)
    ),
    CONSTRAINT fk_member_point_member FOREIGN KEY (member_profile_id) REFERENCES member_profiles (id),
    CONSTRAINT fk_member_point_actor FOREIGN KEY (created_by_user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_member_point_original FOREIGN KEY (original_transaction_id) REFERENCES member_point_transactions (id)
);

CREATE INDEX idx_member_point_member_created
    ON member_point_transactions (member_profile_id, created_at);
CREATE INDEX idx_member_point_original
    ON member_point_transactions (original_transaction_id);

ALTER TABLE cast_content
    ADD COLUMN minimum_store_tier_rank INTEGER;

ALTER TABLE cast_content
    ADD CONSTRAINT chk_content_store_tier_rank CHECK (
        minimum_store_tier_rank IS NULL OR minimum_store_tier_rank BETWEEN 1 AND 3
    );
