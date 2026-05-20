CREATE TABLE IF NOT EXISTS post_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(80) NOT NULL,
    description TEXT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    report_result VARCHAR(20) NULL,
    post_action VARCHAR(20) NULL,
    create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    review_time DATETIME(6) NULL,
    PRIMARY KEY (id)
);

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE post_reports ADD CONSTRAINT fk_post_reports_post FOREIGN KEY (post_id) REFERENCES posts(id)',
        'SELECT 1'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'post_reports'
      AND CONSTRAINT_NAME = 'fk_post_reports_post'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE post_reports ADD CONSTRAINT fk_post_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)',
        'SELECT 1'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = @schema_name
      AND TABLE_NAME = 'post_reports'
      AND CONSTRAINT_NAME = 'fk_post_reports_reporter'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_post_reports_post_id ON post_reports(post_id)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'post_reports'
      AND INDEX_NAME = 'idx_post_reports_post_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_post_reports_reporter_id ON post_reports(reporter_id)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'post_reports'
      AND INDEX_NAME = 'idx_post_reports_reporter_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_post_reports_review_status_create_time ON post_reports(review_status, create_time)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'post_reports'
      AND INDEX_NAME = 'idx_post_reports_review_status_create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_posts_status_create_time ON posts(status, create_time)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'posts'
      AND INDEX_NAME = 'idx_posts_status_create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_posts_user_status_create_time ON posts(user_id, status, create_time)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'posts'
      AND INDEX_NAME = 'idx_posts_user_status_create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
