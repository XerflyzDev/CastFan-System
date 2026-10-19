CREATE TABLE store_event_images (
    store_event_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (store_event_id),
    CONSTRAINT fk_store_event_image_event FOREIGN KEY (store_event_id) REFERENCES store_events (id) ON DELETE CASCADE
);
