CREATE TABLE IF NOT EXISTS report_daily_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    task_count INT NOT NULL DEFAULT 0,
    order_count INT NOT NULL DEFAULT 0,
    payment_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    alert_count INT NOT NULL DEFAULT 0,
    training_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_report_date (stat_date)
);
