-- CarView 车联网系统数据库初始化脚本
-- 使用 UTC 时间存储，前端按本地时区展示

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection = utf8mb4;

CREATE DATABASE IF NOT EXISTS carview DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE carview;

-- ============================================================
-- 1. 车辆基础信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    vin VARCHAR(17) NOT NULL COMMENT '车辆识别码',
    plate_number VARCHAR(20) NOT NULL COMMENT '车牌号（冗余，同vehicle_id）',
    brand VARCHAR(50) COMMENT '品牌',
    model VARCHAR(50) COMMENT '型号',
    color VARCHAR(20) COMMENT '颜色',
    owner_name VARCHAR(50) COMMENT '车主姓名',
    owner_phone VARCHAR(20) COMMENT '车主电话',
    register_date DATE COMMENT '注册日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vin (vin),
    UNIQUE KEY uk_vehicle_id (vehicle_id)
) ENGINE=InnoDB COMMENT='车辆基础信息表';

-- ============================================================
-- 2. 车辆轨迹数据表（GPS 数据）
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle_track (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    event_time TIMESTAMP NOT NULL COMMENT '数据时间',
    lng DOUBLE NOT NULL COMMENT '经度',
    lat DOUBLE NOT NULL COMMENT '纬度',
    speed DOUBLE COMMENT '速度(km/h)',
    rpm INT COMMENT '转速',
    fuel_consumption DOUBLE COMMENT '瞬时油耗(L/100km)',
    mileage DOUBLE COMMENT '总里程(km)',
    engine_temp INT COMMENT '发动机温度',
    battery_voltage DOUBLE COMMENT '电池电压',
    fault_code VARCHAR(20) COMMENT '故障码',
    driver_behavior VARCHAR(20) DEFAULT 'NORMAL' COMMENT '驾驶行为',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_time (vehicle_id, event_time),
    INDEX idx_event_time (event_time)
) ENGINE=InnoDB COMMENT='车辆轨迹数据表';

-- ============================================================
-- 3. 车辆实时状态表（每车一行，Flink 更新）
-- ============================================================
CREATE TABLE IF NOT EXISTS vehicle_realtime_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    last_time TIMESTAMP COMMENT '最后上报时间',
    lng DOUBLE COMMENT '最新经度',
    lat DOUBLE COMMENT '最新纬度',
    speed DOUBLE COMMENT '当前速度',
    rpm INT COMMENT '当前转速',
    fuel_consumption DOUBLE COMMENT '当前油耗',
    mileage DOUBLE COMMENT '总里程',
    engine_temp INT COMMENT '发动机温度',
    battery_voltage DOUBLE COMMENT '电池电压',
    fault_code VARCHAR(20) COMMENT '故障码',
    driver_behavior VARCHAR(20) DEFAULT 'NORMAL' COMMENT '驾驶行为',
    online_status TINYINT DEFAULT 1 COMMENT '在线状态 1-在线 0-离线',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vehicle_id (vehicle_id)
) ENGINE=InnoDB COMMENT='车辆实时状态表';

-- ============================================================
-- 4. 告警事件表
-- ============================================================
CREATE TABLE IF NOT EXISTS alarm_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    alarm_type VARCHAR(50) NOT NULL COMMENT '告警类型: OVERSPEED/FENCE_OUT/FAULT/ENGINE_OVERHEAT/LOW_BATTERY',
    alarm_level TINYINT NOT NULL COMMENT '告警级别: 1-严重 2-警告 3-提示',
    alarm_status TINYINT DEFAULT 1 COMMENT '状态: 1-活跃 2-已确认 3-已解决',
    alarm_message VARCHAR(500) COMMENT '告警描述',
    alarm_value VARCHAR(100) COMMENT '触发值',
    lng DOUBLE COMMENT '告警时经度',
    lat DOUBLE COMMENT '告警时纬度',
    event_time TIMESTAMP NOT NULL COMMENT '告警时间',
    ack_time TIMESTAMP NULL COMMENT '确认时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_level_status_time (alarm_level, alarm_status, event_time),
    INDEX idx_vehicle_time (vehicle_id, event_time)
) ENGINE=InnoDB COMMENT='告警事件表';

-- ============================================================
-- 5. 电子围栏规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS fence_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fence_name VARCHAR(100) NOT NULL COMMENT '围栏名称',
    fence_type VARCHAR(20) NOT NULL COMMENT '围栏类型: RECTANGLE/CIRCLE',
    -- 矩形围栏: 存储左下角和右上角坐标
    min_lng DOUBLE COMMENT '最小经度（矩形左下角）',
    min_lat DOUBLE COMMENT '最小纬度（矩形左下角）',
    max_lng DOUBLE COMMENT '最大经度（矩形右上角）',
    max_lat DOUBLE COMMENT '最大纬度（矩形右上角）',
    -- 圆形围栏: 存储圆心和半径
    center_lng DOUBLE COMMENT '圆心经度',
    center_lat DOUBLE COMMENT '圆心纬度',
    radius DOUBLE COMMENT '半径(米)',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 1-启用 0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='电子围栏规则表';

-- ============================================================
-- 6. Flink 实时聚合结果表（10秒窗口）
-- ============================================================
CREATE TABLE IF NOT EXISTS realtime_agg_10s (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    window_start TIMESTAMP NOT NULL COMMENT '窗口开始时间',
    window_end TIMESTAMP NOT NULL COMMENT '窗口结束时间',
    online_count INT COMMENT '在线车辆数',
    avg_speed DOUBLE COMMENT '平均速度',
    total_mileage_increment DOUBLE COMMENT '总里程增量',
    avg_fuel_consumption DOUBLE COMMENT '平均油耗',
    alarm_count INT DEFAULT 0 COMMENT '窗口内告警数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_window (window_start, window_end)
) ENGINE=InnoDB COMMENT='Flink实时聚合结果表';

-- ============================================================
-- 7. Spark 离线分析 - 日统计表
-- ============================================================
CREATE TABLE IF NOT EXISTS offline_daily_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    stat_date DATE NOT NULL COMMENT '统计日期',
    daily_mileage DOUBLE COMMENT '日行驶里程(km)',
    daily_fuel DOUBLE COMMENT '日油耗(L)',
    avg_speed DOUBLE COMMENT '日均速度(km/h)',
    max_speed DOUBLE COMMENT '最高速度(km/h)',
    driving_duration INT COMMENT '行驶时长(秒)',
    alarm_count INT DEFAULT 0 COMMENT '告警次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vehicle_date (vehicle_id, stat_date),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB COMMENT='Spark日统计结果表';

-- ============================================================
-- 8. Spark 离线分析 - 月度油耗趋势表
-- ============================================================
CREATE TABLE IF NOT EXISTS offline_monthly_fuel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    stat_month VARCHAR(7) NOT NULL COMMENT '统计月份 YYYY-MM',
    avg_fuel_consumption DOUBLE COMMENT '月均油耗(L/100km)',
    total_fuel DOUBLE COMMENT '月总油耗(L)',
    total_mileage DOUBLE COMMENT '月总里程(km)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vehicle_month (vehicle_id, stat_month)
) ENGINE=InnoDB COMMENT='Spark月度油耗趋势表';

-- ============================================================
-- 9. Spark 离线分析 - 故障率排名表
-- ============================================================
CREATE TABLE IF NOT EXISTS offline_fault_rank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id VARCHAR(20) NOT NULL COMMENT '车牌号',
    stat_period VARCHAR(20) NOT NULL COMMENT '统计周期',
    fault_count INT COMMENT '故障次数',
    fault_rate DOUBLE COMMENT '故障率',
    common_fault_code VARCHAR(20) COMMENT '最常见故障码',
    ranking INT COMMENT '排名',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period_rank (stat_period, ranking)
) ENGINE=InnoDB COMMENT='Spark故障率排名表';

-- ============================================================
-- 10. 初始车辆数据（30辆测试车）
-- ============================================================
INSERT INTO vehicle_info (vehicle_id, vin, plate_number, brand, model, color, owner_name, owner_phone, register_date) VALUES
('京A12345', 'LSVAU2A37N2183001', '京A12345', '一汽大众', '迈腾', '黑色', '张三', '13800138001', '2023-01-15'),
('京A23456', 'LSVAU2A37N2183002', '京A23456', '上汽大众', '帕萨特', '白色', '李四', '13800138002', '2023-02-20'),
('京A34567', 'LSVAU2A37N2183003', '京A34567', '丰田', '凯美瑞', '银色', '王五', '13800138003', '2023-03-10'),
('京A45678', 'LSVAU2A37N2183004', '京A45678', '本田', '雅阁', '红色', '赵六', '13800138004', '2023-04-05'),
('京A56789', 'LSVAU2A37N2183005', '京A56789', '日产', '天籁', '蓝色', '孙七', '13800138005', '2023-05-18'),
('京B12345', 'LSVAU2A37N2183006', '京B12345', '比亚迪', '汉', '黑色', '周八', '13800138006', '2023-06-22'),
('京B23456', 'LSVAU2A37N2183007', '京B23456', '吉利', '星瑞', '白色', '吴九', '13800138007', '2023-07-30'),
('京B34567', 'LSVAU2A37N2183008', '京B34567', '长安', 'UNI-V', '灰色', '郑十', '13800138008', '2023-08-14'),
('京B45678', 'LSVAU2A37N2183009', '京B45678', '奇瑞', '艾瑞泽8', '红色', '陈一', '13800138009', '2023-09-05'),
('京B56789', 'LSVAU2A37N2183010', '京B56789', '长城', '哈弗H6', '白色', '林二', '13800138010', '2023-10-20'),
('京C12345', 'LSVAU2A37N2183011', '京C12345', '宝马', '3系', '蓝色', '黄三', '13800138011', '2023-11-11'),
('京C23456', 'LSVAU2A37N2183012', '京C23456', '奔驰', 'C级', '黑色', '刘四', '13800138012', '2023-12-01'),
('京C34567', 'LSVAU2A37N2183013', '京C34567', '奥迪', 'A4L', '白色', '杨五', '13800138013', '2024-01-15'),
('京C45678', 'LSVAU2A37N2183014', '京C45678', '沃尔沃', 'S60', '银色', '马六', '13800138014', '2024-02-20'),
('京C56789', 'LSVAU2A37N2183015', '京C56789', '凯迪拉克', 'CT5', '红色', '朱七', '13800138015', '2024-03-10'),
('京D12345', 'LSVAU2A37N2183016', '京D12345', '特斯拉', 'Model 3', '白色', '胡八', '13800138016', '2024-04-05'),
('京D23456', 'LSVAU2A37N2183017', '京D23456', '蔚来', 'ET5', '蓝色', '何九', '13800138017', '2024-05-18'),
('京D34567', 'LSVAU2A37N2183018', '京D34567', '小鹏', 'P7', '灰色', '罗十', '13800138018', '2024-06-22'),
('京D45678', 'LSVAU2A37N2183019', '京D45678', '理想', 'L7', '绿色', '梁一', '13800138019', '2024-07-30'),
('京D56789', 'LSVAU2A37N2183020', '京D56789', '极氪', '001', '蓝色', '宋二', '13800138020', '2024-08-14'),
('京E12345', 'LSVAU2A37N2183021', '京E12345', '一汽丰田', 'RAV4', '白色', '唐三', '13800138021', '2024-09-05'),
('京E23456', 'LSVAU2A37N2183022', '京E23456', '广汽本田', 'CR-V', '黑色', '韩四', '13800138022', '2024-10-20'),
('京E34567', 'LSVAU2A37N2183023', '京E34567', '东风日产', '奇骏', '银色', '冯五', '13800138023', '2024-11-11'),
('京E45678', 'LSVAU2A37N2183024', '京E45678', '上汽通用', '别克君威', '灰色', '董六', '13800138024', '2024-12-01'),
('京E56789', 'LSVAU2A37N2183025', '京E56789', '马自达', '阿特兹', '红色', '萧七', '13800138025', '2025-01-15'),
('京F12345', 'LSVAU2A37N2183026', '京F12345', '斯柯达', '明锐', '白色', '程八', '13800138026', '2025-02-20'),
('京F23456', 'LSVAU2A37N2183027', '京F23456', '现代', '伊兰特', '蓝色', '曹九', '13800138027', '2025-03-10'),
('京F34567', 'LSVAU2A37N2183028', '京F34567', '起亚', 'K5', '黑色', '袁十', '13800138028', '2025-04-05'),
('京F45678', 'LSVAU2A37N2183029', '京F45678', '福特', '蒙迪欧', '白色', '邓一', '13800138029', '2025-05-18'),
('京F56789', 'LSVAU2A37N2183030', '京F56789', '雪佛兰', '迈锐宝', '银色', '许二', '13800138030', '2025-06-22');

-- 插入默认电子围栏（北京四环内）
INSERT INTO fence_rule (fence_name, fence_type, min_lng, min_lat, max_lng, max_lat, enabled) VALUES
('北京四环区域', 'RECTANGLE', 116.28, 39.83, 116.50, 39.99, 1);

-- 插入默认电子围栏（天安门广场圆形围栏）
INSERT INTO fence_rule (fence_name, fence_type, center_lng, center_lat, radius, enabled) VALUES
('天安门广场区域', 'CIRCLE', 116.397128, 39.916527, 5000, 1);
