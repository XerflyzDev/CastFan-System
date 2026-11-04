CREATE INDEX idx_store_promotion_status_range
    ON store_promotions (store_id, status, starts_at, ends_at);

ALTER TABLE store_promotions
    DROP INDEX idx_store_promotion_active_range;

ALTER TABLE store_promotions
    DROP COLUMN active;
