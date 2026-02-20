# CLAUDE.md

此文件为 Claude Code (claude.ai/code) 提供本仓库的开发指引。

## 项目概述

CarView 是一个车联网 (IoT) 监控平台。通过 Kafka 接收模拟车辆遥测数据，Flink 进行实时流处理，Spark 进行离线批处理，最终在 Vue 3 数据可视化大屏上展示。

## 模块架构

```
carview (父 pom, Java 17)
├── carview-common      — 公共模型、枚举、常量、工具类 (Kafka topic、VehicleTelemetry、AlarmEvent 等)
├── carview-simulator   — 独立 Java 应用，生成模拟车辆遥测数据 → Kafka topic "vehicle-data"
├── carview-flink       — Flink 1.20 流处理作业：超速/故障/围栏检测、实时聚合 → MySQL
├── carview-spark       — Spark 3.5 批处理作业：日里程、驾驶行为评分、故障排名、月度油耗趋势
├── carview-server      — Spring Boot 3.2 REST API + WebSocket 服务 (端口 8080)
└── carview-web         — Vue 3 + TypeScript + Vite 5 前端 (开发端口 5174)
```

**数据流向：** 模拟器 → Kafka (`vehicle-data`) → Flink (实时处理) → MySQL ← Spring Boot API ← Vue 前端。Spark 从 MySQL 读取数据进行批处理分析。

## 构建与运行命令

### 后端 (项目根目录)
```bash
mvn clean package -DskipTests          # 构建所有 Java 模块
mvn clean package -pl carview-server -am  # 仅构建 server 及其依赖

# 启动 Spring Boot 服务
cd carview-server && mvn spring-boot:run

# 启动模拟器 (需要 Kafka 在 localhost:9092)
java -jar carview-simulator/target/carview-simulator-*.jar
# 可覆盖参数: -Dkafka.bootstrap.servers=host:port -Dsimulator.interval.ms=1000 -Dsimulator.vehicle.count=30
```

### 前端 (carview-web/ 目录)
```bash
npm install
npm run dev              # Vite 开发服务器 (端口 5174)
npm run build            # vue-tsc 类型检查 + vite 构建
vue-tsc --noEmit         # 仅类型检查
npm run preview          # 预览生产构建
```

### Docker 基础设施
```bash
docker compose --profile core up -d     # ZooKeeper + Kafka + MySQL
docker compose --profile stream up -d   # Flink JobManager + TaskManager
docker compose --profile batch up -d    # Hadoop NameNode/DataNode + Spark Master/Worker
docker compose --profile bi up -d       # Apache Superset
docker compose --profile full up -d     # 全部启动
```

MySQL 连接信息：用户 `carview` / 密码 `carview123`，数据库 `carview`，端口 3306。表结构通过 `sql/init/001_schema.sql` 自动初始化。

## 前端架构

- **布局：** 全屏深色主题大屏。`App.vue` 提供顶部导航栏（无侧边栏），`<router-view>` 填充剩余空间。
- **路由：** 4 个页面 — `/monitor`（车辆监控）、`/fault`（故障诊断）、`/track`（轨迹回放）、`/assistant`（AI 助手）。默认 `/` 重定向到 `/monitor`。
- **API 层：** `src/api/request.ts` — Axios 实例，`baseURL: '/api'`。响应拦截器自动解包 `response.data`，API 调用直接返回业务数据。Vite 代理 `/api` → `http://localhost:8080`，`/ws` → `ws://localhost:8080`。
- **图表：** 所有图表使用 ECharts 5，通过 `src/hooks/useEcharts.ts` hook 封装。hook 的 options 参数使用 `any` 类型（非 `echarts.EChartsOption`），避免 TypeScript 字面量类型推断问题。
- **地图：** Leaflet + CARTO 深色底图 (`https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png`)。
- **大屏页面** (`monitor/index.vue`、`fault/index.vue`)：CSS Grid 三栏布局 (280px | 1fr | 300px)。每个页面内嵌 mock 数据作为兜底。`mergeWithDefault()` 函数仅在后端字段有实际内容（非空数组、非零值）时覆盖默认数据。
- **公共组件：** `src/components/DataCard.vue` — 带可选标题和发光效果的样式化卡片容器，图表组件使用。
- **轮询：** 两个大屏页面每 10 秒通过 `setInterval` 自动刷新数据。

## 后端架构 (carview-server)

标准 Spring Boot 分层结构，包路径 `com.carview.server`：
- `controller/` — REST 控制器。`DashboardController` 提供 `/api/dashboard/monitor` 和 `/api/dashboard/fault` 接口。
- `service/` — 业务逻辑层。`DashboardService` 从多个 mapper 聚合数据。
- `mapper/` — MyBatis-Plus mapper（已开启自动驼峰映射）。
- `config/` — CORS、MyBatis-Plus 分页、WebSocket 配置。
- `websocket/` — `RealtimeWebSocketHandler` 用于实时推送更新。

## 数据库表结构 (MySQL)

`carview` 数据库中的核心表（均使用 UTF-8mb4，UTC 时间戳）：
- `vehicle_info` — 30 辆种子车辆，车牌号如 京A12345~京F56789
- `vehicle_track` — GPS 遥测历史数据（按 vehicle_id + event_time 建索引）
- `vehicle_realtime_state` — 每辆车一行，由 Flink 更新（在线状态、最新位置）
- `alarm_event` — 告警类型：OVERSPEED、FENCE_OUT、FAULT、ENGINE_OVERHEAT、LOW_BATTERY；等级：1-严重、2-警告、3-信息
- `fence_rule` — RECTANGLE 或 CIRCLE 类型电子围栏
- `realtime_agg_10s` — Flink 10 秒窗口聚合数据
- `offline_daily_stats`、`offline_monthly_fuel`、`offline_fault_rank` — Spark 批处理结果

## 运行环境

- **操作系统：** Windows 11 Home China (10.0.22631)
- **WSL：** Ubuntu-24.04 (WSL 2)
- **Docker：** Engine v29.2.1 + Compose v5.0.2，安装在 WSL 内部（非 Docker Desktop）
- **Docker 用户：** WSL 用户 `furina`，需执行 `sudo usermod -aG docker furina` 获取非 root 访问权限
- **IDE：** IntelliJ IDEA，通过 WSL 集成连接 Docker（Settings > Docker > WSL > Ubuntu-24.04）
- **Java：** 17（Flink 1.20 和 Spring Boot 3.2 要求）
- **网络环境：** 中国大陆，Docker Hub 需在 `/etc/docker/daemon.json` 中配置镜像加速器

### Docker 服务列表 (docker-compose.yml)

所有服务运行在 Docker 网络 `carview-net` 中，通过 profile 控制启动范围。

| Profile | 容器名 | 镜像 | 端口 | 备注 |
|---------|--------|------|------|------|
| core | carview-zookeeper | zookeeper:3.9 | 2181 | 4LW 白名单：srvr,ruok,mntr；健康检查走 Admin Server :8080 |
| core | carview-kafka | apache/kafka:3.7.0 | 9092 | KRaft 模式（不依赖 ZooKeeper），以 root 用户运行解决 volume 权限问题 |
| core | carview-mysql | mysql:8.0.31 | 3306 | 用户：carview / carview123，启动时自动执行 sql/init/ 建表 |
| stream | carview-flink-jm | flink:1.20-scala_2.12-java17 | 8081 | JobManager，process.size: 1024m，mem_limit: 1280m |
| stream | carview-flink-tm | flink:1.20-scala_2.12-java17 | - | TaskManager，4 个任务槽 |
| batch | carview-hadoop-nn | apache/hadoop:3 | 9870, 9000 | HDFS NameNode |
| batch | carview-hadoop-dn | apache/hadoop:3 | - | HDFS DataNode |
| batch | carview-spark-master | apache/spark:3.5.0 | 18080, 7077 | Spark Master，通过 spark-class 命令启动 |
| batch | carview-spark-worker | apache/spark:3.5.0 | - | Spark Worker，1g 内存，2 核 |
| bi | carview-superset | apache/superset:latest | 8088 | BI 可视化面板 |

### Docker 踩坑记录（已解决）

- **Bitnami 镜像下架：** `bitnami/kafka` 和 `bitnami/spark` 已于 2025 年 8 月从 Docker Hub 移除。需改用 `apache/kafka` 和 `apache/spark`，环境变量和启动命令不同。
- **Kafka KRaft 模式：** `apache/kafka` 默认使用 KRaft 模式，不支持 ZooKeeper 模式。必须配置 `KAFKA_PROCESS_ROLES`、`KAFKA_CONTROLLER_QUORUM_VOTERS` 等参数。
- **Kafka volume 权限：** `apache/kafka` 以 `appuser` (uid 1000) 运行，但 Docker volume 挂载目录归 root 所有，写入失败。解决方案：docker-compose 中设置 `user: root`。
- **ZooKeeper 3.9 健康检查：** 默认仅启用 `srvr` 四字命令，`ruok` 未启用。需添加 `ZOO_4LW_COMMANDS_WHITELIST` 并使用 Admin Server HTTP 接口进行健康检查。
- **Flink 1.20 内存：** JobManager `process.size: 512m` 过小（off-heap 128m + JVM overhead 最低 192m 超出总量）。最低需要 1024m。
- **Docker 镜像加速：** `docker.1ms.run` 已失效。需在 `/etc/docker/daemon.json` 中配置多个镜像源实现容错。

## 关键约定

- **语言：** 所有 UI 文本使用中文。代码标识符和注释中英文混用。
- **Kafka topic：** `vehicle-data` 是唯一的遥测数据 topic。消费者组：`carview-flink-realtime`、`carview-hdfs-sink`。
- **告警等级：** 1 = 严重（红色）、2 = 警告（黄色）、3 = 信息（青色）。
- **前端主题：** 深蓝科技风格（`rgba(20, 30, 60, 0.7)` 背景，`#00f2ff` 青色强调色）。禁止使用浅色主题。
- **ECharts 配色：** 监控大屏使用青蓝色系；故障大屏使用红色系。
