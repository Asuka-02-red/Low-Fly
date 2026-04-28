INSERT INTO user_account (username, password_hash, phone, email, role, real_name, company_name)
VALUES
('pilot_demo', '{noop}demo123', '13800138000', 'pilot@example.com', 'PILOT', '张飞手', NULL),
('enterprise_demo', '{noop}demo123', '13800138001', 'enterprise@example.com', 'ENTERPRISE', '李企业', '低空运维科技'),
('institution_demo', '{noop}demo123', '13800138002', 'org@example.com', 'INSTITUTION', '王机构', '飞行培训中心'),
('admin', '{noop}Admin123456', '13800138003', 'admin@example.com', 'ADMIN', '管理员', '平台运营中心')
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO task (enterprise_id, task_type, title, description, location, deadline, latitude, longitude, budget, status)
VALUES
(2, 'INSPECTION', '长江沿线巡检', '桥梁与沿江设施巡检任务', '重庆江北区', '2026-05-01 18:00:00', 29.5637600, 106.5504600, 3000.00, 'PUBLISHED'),
(2, 'MAPPING', '园区测绘', '园区三维建模测绘任务', '重庆渝北区', '2026-05-03 12:00:00', 29.7235800, 106.6388200, 5600.00, 'REVIEWING')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO course (title, institution_name, seat_total, seat_available, price, status)
VALUES
('民航法规基础课', '重庆飞行培训中心', 60, 18, 699.00, 'OPEN'),
('实操飞行强化营', '重庆飞行培训中心', 30, 6, 1299.00, 'OPEN')
ON DUPLICATE KEY UPDATE title = VALUES(title);

INSERT INTO no_fly_zone (name, zone_type, center_lat, center_lng, radius, description)
VALUES
('江北机场净空区', 'FORBIDDEN', 29.7192000, 106.6418000, 10000, '机场周边 10 公里内禁止飞行'),
('两江新区限飞区', 'RESTRICTED', 29.6415000, 106.5667000, 5000, '大型活动期间限制飞行')
ON DUPLICATE KEY UPDATE name = VALUES(name);
