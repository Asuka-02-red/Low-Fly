CREATE TABLE IF NOT EXISTS feedback_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(32) NOT NULL UNIQUE,
    submit_user_id BIGINT NOT NULL,
    submit_user_role VARCHAR(20) NOT NULL,
    contact VARCHAR(100),
    detail TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reply VARCHAR(1000),
    close_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_user_time (submit_user_id, create_time),
    INDEX idx_feedback_status_time (status, create_time)
);
