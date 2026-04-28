SET @schema := DATABASE();

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'publish_user_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN publish_user_id BIGINT NULL AFTER institution_name', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'course_type'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN course_type VARCHAR(20) NOT NULL DEFAULT ''ARTICLE'' AFTER publish_user_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'summary'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN summary VARCHAR(255) NULL AFTER course_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'content'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN content TEXT NULL AFTER summary', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'browse_count'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN browse_count INT NOT NULL DEFAULT 0 AFTER status', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course'
      AND COLUMN_NAME = 'enroll_count'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE course ADD COLUMN enroll_count INT NOT NULL DEFAULT 0 AFTER browse_count', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS course_enrollment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    enrollment_no VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_course_enrollment_course_user (course_id, user_id),
    INDEX idx_course_enrollment_user_time (user_id, create_time)
);

UPDATE course
SET publish_user_id = COALESCE(publish_user_id, 2),
    course_type = COALESCE(NULLIF(course_type, ''), 'ARTICLE'),
    summary = COALESCE(summary, CONCAT(title, ' 学习概览')),
    content = COALESCE(content, CONCAT(title, '：支持企业团队文章学习与线下培训报名。')),
    browse_count = COALESCE(browse_count, 0),
    enroll_count = COALESCE(enroll_count, 0);
