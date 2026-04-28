CREATE TABLE IF NOT EXISTS admin_setting (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_notification_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    trigger_desc VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO admin_setting (setting_key, setting_value)
VALUES
('basic.stationName', '低空驿站一站式数字化服务平台'),
('basic.serviceHotline', '400-820-2026'),
('basic.defaultRegion', '深圳南山'),
('basic.mobileDashboardEnabled', 'true'),
('security.passwordValidityDays', '90'),
('security.loginRetryLimit', '5'),
('security.ipWhitelist', '127.0.0.1/32,10.0.0.0/8'),
('security.mfaRequired', 'true')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);

INSERT INTO admin_notification_rule (rule_key, name, channel, enabled, trigger_desc)
VALUES
('rule-login-risk', '登录风险告警', '站内信', 1, '连续登录失败超过阈值时触发'),
('rule-project-delay', '项目延期提醒', '短信', 1, '项目连续 2 天无进展更新时触发'),
('rule-feedback-escalation', '工单升级通知', '邮件', 1, '反馈工单 24 小时未关闭时触发')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    channel = VALUES(channel),
    enabled = VALUES(enabled),
    trigger_desc = VALUES(trigger_desc);

INSERT INTO report_daily_summary (stat_date, task_count, order_count, payment_amount, alert_count, training_count)
VALUES
('2026-04-17', 8, 4, 92000.00, 3, 14),
('2026-04-18', 10, 5, 108000.00, 2, 18),
('2026-04-19', 11, 6, 126000.00, 2, 22),
('2026-04-20', 9, 5, 118000.00, 4, 20),
('2026-04-21', 12, 7, 146000.00, 2, 26),
('2026-04-22', 13, 8, 168000.00, 1, 29)
ON DUPLICATE KEY UPDATE
    task_count = VALUES(task_count),
    order_count = VALUES(order_count),
    payment_amount = VALUES(payment_amount),
    alert_count = VALUES(alert_count),
    training_count = VALUES(training_count);
