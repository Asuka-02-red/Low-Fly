CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_order_id BIGINT NOT NULL,
    trade_no VARCHAR(32) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    callback_payload TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_biz_status (biz_order_id, status)
);

CREATE TABLE IF NOT EXISTS message_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    recipient_role VARCHAR(20) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(255) NOT NULL,
    send_status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(50) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_payload TEXT,
    replayable TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_biz_type_biz_id (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS feature_flag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flag_key VARCHAR(50) NOT NULL UNIQUE,
    flag_value VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ab_experiment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    experiment_key VARCHAR(50) NOT NULL UNIQUE,
    variant_a VARCHAR(50) NOT NULL,
    variant_b VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_name VARCHAR(20) NOT NULL,
    channel_name VARCHAR(20) NOT NULL,
    force_update TINYINT NOT NULL DEFAULT 0,
    release_note VARCHAR(255) NOT NULL,
    download_url VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
