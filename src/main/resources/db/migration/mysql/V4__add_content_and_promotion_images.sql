CREATE TABLE cast_content_images (
    cast_content_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (cast_content_id),
    CONSTRAINT fk_cast_content_image_content FOREIGN KEY (cast_content_id) REFERENCES cast_content (id) ON DELETE CASCADE
);

CREATE TABLE store_promotion_images (
    store_promotion_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (store_promotion_id),
    CONSTRAINT fk_store_promotion_image_promotion FOREIGN KEY (store_promotion_id) REFERENCES store_promotions (id) ON DELETE CASCADE
);
