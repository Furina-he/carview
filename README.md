# CarView - 智能车联网监控平台

基于大数据技术栈的车联网 IoT 监控平台。通过 Kafka 接收模拟车辆遥测数据，Flink 进行实时流处理，Spark 进行离线批处理，Spring Boot 提供 REST API，Vue 3 数据可视化大屏展示。

## 目录

- [系统架构](#系统架构)
- [模块说明](#模块说明)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署指南](#详细部署指南)
- [前端页面](#前端页面)
- [数据库设计](#数据库设计)
- [API 接口](#api-接口)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

## 系统架构

```
┌──────────────┐     ┌───────────┐     ┌──────────────┐     ┌───────────┐     ┌──────────────┐
│  模拟器       │────▶│  Kafka    │────▶│  Flink       │────▶│  MySQL    │◀────│  Spring Boot │
│  Simulator   │     │  3.7.0    │     │  1.20        │     │  8.0      │     │  3.2 API     │
└──────────────┘     └───────────┘     └──────────────┘     └─────┬─────┘     └──────┬───────┘
                                                                  │                   │
                                                                  ▼                   ▼
                                                           ┌──────────────┐    ┌──────────────┐
                                                           │  Spark 3.5   │    │  Vue 3 前端   │
                                                           │  批处理分析    │    │  数据可视化    │
                                                           └──────────────┘    └──────────────┘
```

**数据流向：** 模拟器 → Kafka (`vehicle-data` topic) → Flink (实时处理) → MySQL ← Spring Boot API ← Vue 前端。Spark 从 MySQL 读取历史数据进行批处理分析。

## 模块说明

```
carview/                          # 父 POM，Java 17
├── carview-common/               # 公共模型、枚举、常量、工具类
├── carview-simulator/            # 独立 Java 应用，生成模拟车辆遥测数据
├── carview-flink/                # Flink 流处理：超速/故障/围栏检测、实时聚合
├── carview-spark/                # Spark 批处理：日里程、驾驶行为评分、故障排名、月度油耗趋势
├── carview-server/               # Spring Boot REST API + WebSocket 服务
├── carview-web/                  # Vue 3 + TypeScript 前端大屏
├── docker/                       # Docker 附加配置（Hadoop 等）
├── sql/init/                     # MySQL 建表及种子数据脚本
└── docker-compose.yml            # 全部基础设施容器编排
```

| 模块 | 技术栈 | 说明 |
|------|--------|------|
| carview-common | Java 17, Lombok, Jackson | 共享模型（VehicleTelemetry, AlarmEvent, FenceRule 等）、Kafka topic 常量、GeoUtils 工具 |
| carview-simulator | Java 17, Kafka Client 3.7 | 模拟 30 辆车的 GPS、速度、油耗、故障等遥测数据，按固定间隔发送至 Kafka |
| carview-flink | Flink 1.20, Kafka Connector, JDBC Connector | 消费 Kafka 数据，执行超速检测、故障检测、围栏越界检测、10秒窗口聚合，结果写入 MySQL |
| carview-spark | Spark 3.5, Spark SQL | 从 MySQL 读取历史数据，计算日里程、驾驶行为评分、故障排名、月度油耗趋势 |
| carview-server | Spring Boot 3.2, MyBatis-Plus 3.5, WebSocket | REST API 服务，为前端提供监控大屏、故障诊断、轨迹回放、围栏管理等接口 |
| carview-web | Vue 3.4, TypeScript, Vite 5, ECharts 5, Leaflet, Element Plus | 深色主题数据可视化大屏，包含 5 个功能页面 |

## 环境要求

### 必需软件

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Flink 1.20 和 Spring Boot 3.2 要求 Java 17 |
| Maven | 3.8+ | 构建全部 Java 模块 |
| Node.js | 18+ | 前端构建和开发 |
| npm | 9+ | 前端包管理 |
| Docker Engine | 24+ | 运行基础设施容器 |
| Docker Compose | v2.20+ | 容器编排（需支持 profiles） |

### 推荐运行环境

本项目在以下环境中开发和测试：

- **操作系统：** Windows 11 + WSL 2 (Ubuntu 24.04)
- **Docker：** 安装在 WSL 内部（非 Docker Desktop），Engine v29.2.1 + Compose v5.0.2
- **IDE：** IntelliJ IDEA（通过 WSL 集成连接 Docker）
- **内存建议：** 全部服务运行至少需要 8GB 可用内存

### Docker 在 WSL 中的配置

如果使用 WSL 运行 Docker，需确保当前用户有 Docker 权限：

```bash
sudo usermod -aG docker $USER
# 重新登录 WSL 使权限生效
```

### Docker 镜像加速（中国大陆用户）

在中国大陆网络环境下，拉取 Docker Hub 镜像可能较慢，建议在 `/etc/docker/daemon.json` 中配置镜像加速器：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerhub.timeweb.cloud",
    "https://mirror.ccs.tencentyun.com"
  ]
}
```

配置后重启 Docker：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/Furina-he/carview.git
cd carview
```

### 2. 启动基础设施（ZooKeeper + Kafka + MySQL）

```bash
docker compose --profile core up -d
```

等待所有容器健康运行（约 30-60 秒）：

```bash
docker compose --profile core ps
```

确认 `carview-kafka`、`carview-mysql` 状态为 `healthy`。

### 3. 构建后端

```bash
mvn clean package -DskipTests
```

### 4. 启动 Spring Boot 服务

```bash
cd carview-server
mvn spring-boot:run
```

服务启动后监听 `http://localhost:8080`。

### 5. 启动前端

打开新终端：

```bash
cd carview-web
npm install
npm run dev
```

访问 `http://localhost:5173`（如端口被占用则为 5174）。

### 6.（可选）启动模拟器生成测试数据

打开新终端：

```bash
java -jar carview-simulator/target/carview-simulator-1.0-SNAPSHOT.jar
```

模拟器将持续生成 30 辆车的遥测数据并发送至 Kafka。

## 详细部署指南

### Docker 服务分组

所有容器通过 Docker Compose profiles 分组管理，可按需启动：

```bash
# 核心服务：ZooKeeper + Kafka + MySQL
docker compose --profile core up -d

# 流处理：Flink JobManager + TaskManager
docker compose --profile stream up -d

# 批处理：Hadoop NameNode/DataNode + Spark Master/Worker
docker compose --profile batch up -d

# BI 可视化：Apache Superset
docker compose --profile bi up -d

# 一键启动全部
docker compose --profile full up -d
```

### 服务端口列表

| 服务 | 容器名 | 端口 | 用途 |
|------|--------|------|------|
| ZooKeeper | carview-zookeeper | 2181 | ZooKeeper 客户端连接 |
| Kafka | carview-kafka | 9092 | Kafka Broker（KRaft 模式） |
| MySQL | carview-mysql | 3306 | 数据库连接 |
| Flink JobManager | carview-flink-jm | 8081 | Flink Web UI |
| Hadoop NameNode | carview-hadoop-nn | 9870 / 9000 | HDFS Web UI / HDFS RPC |
| Spark Master | carview-spark-master | 18080 / 7077 | Spark Web UI / Spark RPC |
| Superset | carview-superset | 8088 | BI 可视化面板 |
| Spring Boot | - | 8080 | REST API + WebSocket |
| Vue 前端 | - | 5173 | Vite 开发服务器 |

### MySQL 连接信息

| 参数 | 值 |
|------|-----|
| 地址 | localhost:3306 |
| 数据库名 | carview |
| 用户名 | carview |
| 密码 | carview123 |
| 字符集 | utf8mb4 |
| 时区 | UTC |

数据库表结构和种子数据通过 `sql/init/001_schema.sql` 在 MySQL 容器首次启动时自动初始化。

### 构建各模块

```bash
# 构建全部 Java 模块
mvn clean package -DskipTests

# 仅构建 server 及其依赖
mvn clean package -pl carview-server -am -DskipTests

# 仅构建 simulator
mvn clean package -pl carview-simulator -am -DskipTests

# 前端构建（含类型检查）
cd carview-web
npm run build

# 仅前端类型检查
npx vue-tsc --noEmit
```

### 模拟器参数配置

模拟器支持通过 JVM 系统参数覆盖默认配置：

```bash
java -jar carview-simulator/target/carview-simulator-1.0-SNAPSHOT.jar \
  -Dkafka.bootstrap.servers=localhost:9092 \
  -Dsimulator.interval.ms=1000 \
  -Dsimulator.vehicle.count=30
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| kafka.bootstrap.servers | localhost:9092 | Kafka 连接地址 |
| simulator.interval.ms | 1000 | 数据发送间隔（毫秒） |
| simulator.vehicle.count | 30 | 模拟车辆数量 |

## 前端页面

前端采用全屏深色科技风格主题，包含 5 个功能页面：

### 车辆监控 (`/monitor`)

实时监控大屏，CSS Grid 三栏布局。展示：
- 关键 KPI 指标卡片（在线车辆数、平均速度、总里程、告警数）
- Leaflet 地图实时车辆分布
- ECharts 图表：品牌分布饼图、告警类型统计、速度分布、里程趋势、月度车辆统计

### 故障诊断 (`/fault`)

故障分析大屏，CSS Grid 三栏布局。展示：
- 故障 KPI 指标卡片
- 故障地理分布地图
- 告警级别饼图、每日告警趋势、品牌故障统计、故障类型排名、最近告警列表

### 轨迹回放 (`/track`)

左侧查询面板 + 右侧地图的 Flex 布局。支持：
- 按车牌号和时间范围查询历史轨迹
- Leaflet 地图绘制轨迹线
- 播放控制器逐点回放

### 电子围栏 (`/fence`)

左侧围栏列表 + 右侧地图的 Flex 布局。支持：
- 围栏列表增删改查
- 矩形围栏（`L.rectangle`）和圆形围栏（`L.circle`）地图可视化
- 启用/禁用状态区分显示（青色/灰色）
- 点击围栏高亮定位

### 智能助手 (`/assistant`)

预设问答式 AI 助手界面。

### 主题和配色

| 项目 | 值 |
|------|-----|
| 背景色 | `rgba(20, 30, 60, 0.7)` |
| 强调色 | `#00f2ff`（青色） |
| 文字主色 | `#e6f7ff` |
| 文字次色 | `rgba(230, 247, 255, 0.65)` |
| 地图底图 | CARTO Dark Matter |
| 监控图表色系 | 青蓝色系 |
| 故障图表色系 | 红色系 |

## 数据库设计

共 9 张核心表：

| 表名 | 说明 | 数据来源 |
|------|------|---------|
| vehicle_info | 车辆基础信息（30 辆种子车） | SQL 初始化 |
| vehicle_track | GPS 遥测历史记录 | Flink 写入 |
| vehicle_realtime_state | 每车最新状态（一车一行） | Flink 更新 |
| alarm_event | 告警事件 | Flink 写入 |
| fence_rule | 电子围栏规则 | 前端管理 |
| realtime_agg_10s | 10 秒窗口聚合数据 | Flink 写入 |
| offline_daily_stats | 日统计结果 | Spark 写入 |
| offline_monthly_fuel | 月度油耗趋势 | Spark 写入 |
| offline_fault_rank | 故障排名 | Spark 写入 |

### 告警类型和级别

| 告警类型 | 说明 |
|---------|------|
| OVERSPEED | 超速告警 |
| FENCE_OUT | 围栏越界 |
| FAULT | 车辆故障 |
| ENGINE_OVERHEAT | 发动机过热 |
| LOW_BATTERY | 电池低压 |

| 级别 | 含义 | 颜色 |
|------|------|------|
| 1 | 严重 | 红色 |
| 2 | 警告 | 黄色 |
| 3 | 信息 | 青色 |

## API 接口

Spring Boot 服务运行在 `http://localhost:8080`，前端通过 Vite 代理 `/api` 访问。

### 监控大屏

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/dashboard/monitor | 获取车辆监控大屏聚合数据 |
| GET | /api/dashboard/fault | 获取故障诊断大屏聚合数据 |

### 车辆管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/vehicles | 获取车辆列表 |
| GET | /api/vehicles/{id}/track | 查询车辆历史轨迹 |

### 告警管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/alarms | 获取告警列表 |

### 电子围栏

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/fences | 获取全部围栏规则 |
| POST | /api/fences | 创建围栏 |
| PUT | /api/fences/{id} | 更新围栏 |
| DELETE | /api/fences/{id} | 删除围栏 |

### 统计数据

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/statistics/realtime | 获取实时统计数据 |

### WebSocket

- 连接地址：`ws://localhost:8080/ws/realtime`
- 用途：推送实时车辆状态更新

## 配置说明

### 后端配置

配置文件位于 `carview-server/src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  kafka:
    bootstrap-servers: localhost:9092
  datasource:
    url: jdbc:mysql://localhost:3306/carview?useSSL=false&serverTimezone=UTC
    username: carview
    password: carview123
```

### 前端配置

Vite 配置文件 `carview-web/vite.config.ts` 中定义了代理规则：

- `/api` → `http://localhost:8080`（REST API 代理）
- `/ws` → `ws://localhost:8080`（WebSocket 代理）

### Docker 环境变量

通过 `docker/.env` 文件管理：

```env
MYSQL_ROOT_PASSWORD=carview123
MYSQL_USER=carview
MYSQL_PASSWORD=carview123
MYSQL_DATABASE=carview
SUPERSET_SECRET_KEY=carview-superset-secret-key-2024
```

### Kafka Topic

| Topic | 分区数 | 说明 |
|-------|--------|------|
| vehicle-data | 3 | 车辆遥测数据（唯一 topic） |

消费者组：
- `carview-flink-realtime` — Flink 实时处理
- `carview-hdfs-sink` — HDFS 数据归档
- `carview-server` — Spring Boot 服务端消费

## 常见问题

### Kafka 启动失败，提示权限问题

`apache/kafka` 镜像以 `appuser` (uid 1000) 运行，Docker volume 目录归 root 所有导致写入失败。`docker-compose.yml` 中已设置 `user: root` 解决此问题。

### Flink JobManager 启动后 OOM

Flink 1.20 的 JobManager `process.size` 不能低于 1024m（off-heap 128m + JVM overhead 最低 192m 会超出总量）。`docker-compose.yml` 中已配置为 1024m。

### ZooKeeper 健康检查失败

ZooKeeper 3.9 默认仅启用 `srvr` 四字命令，`ruok` 未启用。`docker-compose.yml` 中已通过 `ZOO_4LW_COMMANDS_WHITELIST` 白名单和 Admin Server HTTP 接口解决。

### 前端地图不显示

确认网络可访问 CARTO 底图服务 `https://{s}.basemaps.cartocdn.com`。如在内网环境，需配置代理或更换底图源。

### MySQL 中文乱码

`docker-compose.yml` 中已设置 `--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`，SQL 初始化脚本开头也设置了 `SET NAMES utf8mb4`。

### 端口冲突

如果默认端口被占用：
- Spring Boot 端口：修改 `application.yml` 中的 `server.port`
- 前端端口：Vite 会自动尝试下一个端口（5173 → 5174）
- MySQL 3306 冲突：修改 `docker-compose.yml` 中的端口映射
- Kafka 9092 冲突：修改 `docker-compose.yml` 中的 `EXTERNAL` 监听器端口

### 停止全部服务

```bash
# 停止并移除容器（保留数据卷）
docker compose --profile full down

# 停止并移除容器和数据卷（清除全部数据）
docker compose --profile full down -v
```
