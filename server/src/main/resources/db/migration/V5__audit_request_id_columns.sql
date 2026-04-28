SET @schema := DATABASE();

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'audit_event'
      AND COLUMN_NAME = 'request_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE audit_event ADD COLUMN request_id VARCHAR(64) NULL AFTER id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'audit_event'
      AND COLUMN_NAME = 'actor_user_id'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE audit_event ADD COLUMN actor_user_id BIGINT NULL AFTER request_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'audit_event'
      AND COLUMN_NAME = 'actor_role'
);
SET @sql := IF(@exists = 0, 'ALTER TABLE audit_event ADD COLUMN actor_role VARCHAR(32) NULL AFTER actor_user_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'audit_event'
      AND INDEX_NAME = 'idx_audit_request_id'
);
SET @sql := IF(@exists = 0, 'CREATE INDEX idx_audit_request_id ON audit_event (request_id)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

