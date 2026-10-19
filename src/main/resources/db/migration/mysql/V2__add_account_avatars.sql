CREATE TABLE account_avatars (
    user_account_id BIGINT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_account_id),
    CONSTRAINT fk_account_avatar_user FOREIGN KEY (user_account_id) REFERENCES user_accounts (id) ON DELETE CASCADE
);
