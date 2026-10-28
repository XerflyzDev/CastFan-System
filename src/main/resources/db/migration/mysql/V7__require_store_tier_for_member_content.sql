UPDATE cast_content
SET minimum_store_tier_rank = (
    SELECT CASE
        WHEN member_tiers.rank_number >= 3 THEN 3
        ELSE member_tiers.rank_number
    END
    FROM member_tiers
    WHERE member_tiers.id = cast_content.minimum_tier_id
)
WHERE audience = 'MEMBERS'
  AND minimum_store_tier_rank IS NULL
  AND minimum_tier_id IS NOT NULL;

ALTER TABLE cast_content
    DROP CONSTRAINT chk_content_tier;

ALTER TABLE cast_content
    ADD CONSTRAINT chk_content_tier CHECK (
        (audience = 'MEMBERS' AND minimum_store_tier_rank IS NOT NULL)
        OR (audience <> 'MEMBERS'
            AND minimum_store_tier_rank IS NULL
            AND minimum_tier_id IS NULL)
    );
