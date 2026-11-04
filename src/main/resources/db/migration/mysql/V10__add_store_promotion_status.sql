ALTER TABLE store_promotions
    ADD COLUMN status VARCHAR(20) NULL AFTER active;

UPDATE store_promotions
SET status = CASE
    WHEN active = TRUE THEN 'ACTIVE'
    ELSE 'DRAFT'
END
WHERE status IS NULL;

ALTER TABLE store_promotions
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

ALTER TABLE store_promotions
    ADD CONSTRAINT chk_store_promotion_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));
