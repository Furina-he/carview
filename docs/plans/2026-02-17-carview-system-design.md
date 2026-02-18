# CarView - 大数据驱动的智能车联网系统设计文档

## 1. 项目概述

基于大数据技术栈构建的智能车联网模拟系统，采用 Lambda 架构（实时层 + 批处理层），实现车辆实时监控、轨迹管理、故障预警、离线分析等功能。

**技术栈：** Java 17、Spring Boot、Kafka、Flink、Hadoop(HDFS)、Spark、MySQL、Superset、Vue 3、ECharts

**运行环境：** WSL2 + Docker Compose

## 2. 系统架构

```
数据模拟器(Java)
    |
    v
  Kafka
    |
    |---> Flink（实时处理）---> MySQL（实时结果）---> Spring Boot API ---> Vue 大屏
    |
    +---> HDFS（原始数据落盘）---> Spark（离线批处理）---> MySQL（离线分析结果）---> Superset
```

### 数据流说明

1. 数据模拟器持续生成模拟车辆数据（GPS、速度、转速、油耗、故障码等），写入 Kafka 的 `vehicle-data` topic
2. Flink 消费 Kafka 数据做实时计算（超速检测、电子围栏判断、实时聚合），结果写入 MySQL 实时结果表
3. Kafka 数据同时落盘到 HDFS，供离线分析用
4. Spark 定时从 HDFS 读取原始数据做批处理（日均里程、月度油耗、故障率排名等），结果写回 MySQL
5. Spring Boot 提供 REST API + WebSocket，从 MySQL 查询实时和历史数据
6. Vue 大屏调用 API 展示实时监控，支持 WebSocket 推送实时告警
7. Superset 直连 MySQL，基于 Spark 离线分析结果做可视化报表

## 3. 数据模型

### 3.1 车辆实时数据（Kafka 消息结构）

```json
{
  "vehicleId": "京A12345",
  "vin": "LSVAU2A37N2183456",
  "timestamp": 1700000000000,
  "lng": 116.397128,
  "lat": 39.916527,
  "speed": 85.5,
  "rpm": 3200,
  "fuelConsumption": 8.2,
  "mileage": 52360.5,
  "engineTemp": 92,
  "batteryVoltage": 12.6,
  "faultCode": null,
  "driverBehavior": "NORMAL"
}
```

| 字段 | 说明 | 用途 |
|------|------|------|
| vehicleId / vin | 车辆标识 | 车辆管理、查询 |
| timestamp | 时间戳 | 所有时序分析的基础 |
| lng / lat | 经纬度 | 地图展示、电子围栏、轨迹回放 |
| speed | 速度(km/h) | 超速检测、驾驶行为分析 |
| rpm | 转速 | 异常检测 |
| fuelConsumption | 瞬时油耗(L/100km) | 油耗统计、节能分析 |
| mileage | 总里程(km) | 里程统计 |
| engineTemp | 发动机温度 | 故障预警 |
| batteryVoltage | 电池电压 | 故障预警 |
| faultCode | 故障码(OBD) | 故障诊断模块 |
| driverBehavior | 驾驶行为标签 | NORMAL / RAPID_ACCEL / HARD_BRAKE / SHARP_TURN |

模拟器每秒为每辆车生成一条消息发到 Kafka，模拟 20-50 辆车。

### 3.2 车辆基础信息表（MySQL 静态数据）

```sql
vehicle_info (
  vehicle_id, vin, plate_number, brand, model,
  color, owner_name, owner_phone, register_date
)
```

## 4. 六大功能模块

### 模块 1：实时数据监控大屏

- **技术：** Vue 大屏 + WebSocket + ECharts
- **布局：** 深蓝色科技风全屏 dashboard
  - 中央：中国地图，车辆位置用闪烁点标注，实时更新
  - 左侧：在线车辆数、今日告警数、今日里程总数等 KPI 卡片
  - 右侧：实时速度分布图、油耗趋势折线图
  - 底部：最近告警列表（滚动显示）
- **数据流：** Spring Boot 通过 WebSocket 向前端推送 Flink 处理后的实时数据

### 模块 2：车辆管理与轨迹查询

- **技术：** Vue 页面 + Spring Boot CRUD API + MySQL
- **功能：**
  - 车辆列表页：增删改查车辆基础信息
  - 轨迹回放页：选择车辆 + 时间范围，查历史 GPS 数据，ECharts 地图画轨迹线，支持播放/暂停/倍速
  - 电子围栏设置：在地图上画矩形/圆形区域，存入 MySQL，Flink 实时判断越界

### 模块 3：Flink 实时流处理

Flink 做三件事：
1. **超速检测** — 速度 > 120km/h 时生成告警，写入 MySQL 告警表
2. **电子围栏判断** — 判断 GPS 是否在围栏区域外，越界则告警
3. **实时聚合** — 每 10 秒滑动窗口统计：平均速度、在线车辆数、总里程增量，写入 MySQL 实时统计表

### 模块 4：Spark 离线批处理 + Superset

**Spark 定时任务（每天/每小时）：**
- 从 HDFS 读取原始数据
- 计算：每辆车日均里程、月度油耗趋势、故障率排名、驾驶行为评分
- 结果写回 MySQL 离线分析结果表

**Superset：**
- 直连 MySQL，基于离线分析结果表做可视化报表
- 配置 dashboard：油耗排行、故障统计、车辆活跃度等

### 模块 5：故障诊断与预警

- **技术：** Flink 规则引擎 + Spring Boot API
- **规则：** faultCode != null、engineTemp > 110、batteryVoltage < 11
- **告警分级：** 一级（严重）、二级（警告）、三级（提示）
- **展示：** 前端告警列表和统计图表

### 模块 6：智能助手

- **技术：** 纯前端 Vue 组件
- **实现：** 右下角悬浮对话框，前端维护 JSON 问答库，关键词匹配返回答案

## 5. Docker 部署架构

所有中间件通过 docker-compose 启动，业务代码本地开发运行。

```
docker-compose.yml
├── zookeeper        (端口 2181)
├── kafka            (端口 9092)
├── flink-jobmanager (端口 8081 - Flink Web UI)
├── flink-taskmanager
├── hadoop-namenode  (端口 9870 - HDFS Web UI)
├── hadoop-datanode
├── spark-master     (端口 8080 - Spark Web UI)
├── spark-worker
├── mysql            (端口 3306)
└── superset         (端口 8088 - Superset Web UI)
```

**本地运行（不放 Docker）：**
- Spring Boot 后端
- Vue 前端（npm run dev）
- Flink Job（打 jar 包提交到 Flink 集群）
- Spark Job（打 jar 包提交到 Spark 集群）
- 数据模拟器（Java 程序本地跑）

**内存分配（16GB 方案）：**

| 容器 | 限制内存 |
|------|---------|
| Kafka + Zookeeper | 1GB |
| Flink (job + task) | 1.5GB |
| Hadoop (name + data) | 1.5GB |
| Spark (master + worker) | 1GB |
| MySQL | 512MB |
| Superset | 512MB |
| 容器合计 | ~6GB |

WSL2 分配 8GB，容器占 6GB，剩余给系统和本地程序。Windows 侧保留 8GB 跑 IDE 和浏览器。

## 6. 项目工程结构

```
carview/
├── docker-compose.yml
├── docker/
│   ├── hadoop/
│   ├── spark/
│   └── superset/
│
├── carview-common/                   # 公共模块：实体类、常量、工具类
├── carview-simulator/                # 数据模拟器：生成车辆数据 -> Kafka
├── carview-flink/                    # Flink 实时处理任务
├── carview-spark/                    # Spark 离线批处理任务
├── carview-server/                   # Spring Boot 后端
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── websocket/
│   └── config/
│
├── carview-web/                      # Vue 3 前端
│   ├── views/
│   │   ├── dashboard/                # 实时监控大屏
│   │   ├── vehicle/                  # 车辆管理
│   │   ├── track/                    # 轨迹回放
│   │   ├── fence/                    # 电子围栏
│   │   ├── alarm/                    # 告警管理
│   │   └── assistant/                # 智能助手
│   └── components/
│
├── docs/plans/
└── sql/init.sql
```

Maven 多模块项目：common、simulator、flink、spark、server 五个 Java 子模块，web 为独立 Vue 项目。
