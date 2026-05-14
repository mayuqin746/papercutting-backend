CREATE TABLE user_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_follow (follower_id, following_id),
    KEY idx_user_follows_follower (follower_id),
    KEY idx_user_follows_following (following_id),
    CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users(id),
    CONSTRAINT fk_user_follows_following FOREIGN KEY (following_id) REFERENCES users(id)
);
