SET @schema := DATABASE();

SET @idx_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'course_enrollment'
      AND INDEX_NAME = 'idx_course_enrollment_course_user_status'
);
SET @sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_course_enrollment_course_user_status ON course_enrollment (course_id, user_id, status)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE course
SET publish_user_id = COALESCE(publish_user_id, 2),
    course_type = COALESCE(NULLIF(course_type, ''), 'ARTICLE'),
    summary = COALESCE(NULLIF(summary, ''), CONCAT(title, ' 学习概览')),
    content = COALESCE(NULLIF(content, ''), CONCAT(title, '：支持企业团队文章学习与线下培训报名。')),
    browse_count = COALESCE(browse_count, 0),
    enroll_count = COALESCE(enroll_count, 0),
    seat_total = COALESCE(seat_total, 0),
    seat_available = COALESCE(seat_available, 0),
    status = COALESCE(NULLIF(status, ''), 'DRAFT');
