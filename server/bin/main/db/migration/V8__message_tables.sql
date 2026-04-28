CREATE TABLE IF NOT EXISTS message_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    enterprise_id BIGINT NOT NULL,
    pilot_id BIGINT NOT NULL,
    task_id BIGINT,
    title VARCHAR(120) NOT NULL,
    last_message_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_conversation_enterprise (enterprise_id, update_time),
    INDEX idx_message_conversation_pilot (pilot_id, update_time)
);

CREATE TABLE IF NOT EXISTS message_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_entry_conversation_time (conversation_id, create_time)
);
