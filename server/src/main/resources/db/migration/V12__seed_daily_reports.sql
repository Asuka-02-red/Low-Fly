INSERT INTO report_daily_summary (stat_date, task_count, order_count, payment_amount, alert_count, training_count)
VALUES
(CURDATE() - INTERVAL 6 DAY, 18, 5, 12800.00, 2, 3),
(CURDATE() - INTERVAL 5 DAY, 22, 8, 21500.00, 1, 5),
(CURDATE() - INTERVAL 4 DAY, 19, 6, 15600.00, 3, 4),
(CURDATE() - INTERVAL 3 DAY, 25, 12, 34200.00, 0, 6),
(CURDATE() - INTERVAL 2 DAY, 21, 9, 19800.00, 2, 5),
(CURDATE() - INTERVAL 1 DAY, 20, 7, 16700.00, 1, 4)
ON DUPLICATE KEY UPDATE
    task_count = VALUES(task_count),
    order_count = VALUES(order_count),
    payment_amount = VALUES(payment_amount),
    alert_count = VALUES(alert_count),
    training_count = VALUES(training_count);
