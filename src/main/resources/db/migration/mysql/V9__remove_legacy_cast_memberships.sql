ALTER TABLE cast_content DROP FOREIGN KEY fk_content_tier;
ALTER TABLE cast_content DROP CONSTRAINT chk_content_tier;
ALTER TABLE cast_content DROP COLUMN minimum_tier_id;
ALTER TABLE cast_content
    ADD CONSTRAINT chk_content_tier CHECK (
        (audience = 'MEMBERS' AND minimum_store_tier_rank IS NOT NULL)
        OR (audience <> 'MEMBERS' AND minimum_store_tier_rank IS NULL)
    );

DROP TABLE tier_memberships;
DROP TABLE member_tiers;
