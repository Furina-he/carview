# CarView 前端数据可视化大屏改造 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将现有前端从带侧边栏的管理系统风格，改造为参考图片中的全屏数据可视化大屏风格，重点展示数据变化趋势和实时监控，移除所有 CRUD 管理功能。

**Architecture:** 前端改为两个全屏大屏页面 + 轨迹回放 + 智能助手，移除侧边栏导航改为顶部切换。后端新增统计 API 接口（告警分类统计、车辆品牌分布、日行驶趋势、故障排行等），为大屏图表提供数据源。所有图表通过 WebSocket + 轮询实现实时数据刷新。

**Tech Stack:** Vue 3 + TypeScript + ECharts 5 + Leaflet + Element Plus + Pinia + WebSocket + Spring Boot 3.2 + MyBatis-Plus + MySQL

---

## 总体改造说明

### 参考图片分析

**图片1 - 远程诊断与故障监控平台：**
- 顶部：标题 + 实时时间
- 左上：KPI 数字卡片（故障次数、报警次数、故障车辆数、故障等级分布）- 按月/年/日维度
- 中间：中国地图散点图（车辆/故障分布）
- 右侧：多维度折线图（车系故障趋势）、柱状图（故障分析）
- 左下：饼图（故障等级占比）、横向柱状图（省市故障TOP10）
- 中下：表格（最近故障车辆信息）
- 右下：柱状图（分车系故障数）

**图片2 - 全国车辆监控系统：**
- 顶部：标题 + 实时时间
- 左上：KPI 数字卡片（行驶时长、里程、平均值）
- 中上：KPI（车辆总数、在线车辆、充电车辆、行驶中车辆）
- 中间：中国地图散点图（车辆实时分布）
- 右上：饼图（车型分类占比、用途分类占比）
- 左中：折线图（车辆行驶数量月趋势）
- 右中：折线图（车系行程趋势）
- 左下：柱状图（车辆行程趋势）
- 中下：散点图（充电高峰时间）
- 右下：横向柱状图（车辆报警统计）

### 改造范围

**移除页面：** 车辆管理（CRUD）、电子围栏管理（CRUD）、告警管理（CRUD表格）
**保留改造：** Dashboard 拆分为两个大屏、轨迹回放（保留）、智能助手（保留）
**新增：** 故障监控大屏页面

### 前端路由结构（改造后）

```
/ -> /monitor（默认）
/monitor      - 车辆监控大屏（对应图片2）
/fault        - 故障诊断大屏（对应图片1）
/track        - 轨迹回放（保留，微调样式）
/assistant    - 智能助手（保留）
```

### 页面布局（改造后）

移除侧边栏，改为全屏布局 + 顶部导航条：
```
┌─────────────────────────────────────────────────────────────┐
│  CarView 智能车联网监控系统     [监控] [故障] [轨迹] [助手]  │ ← 顶部条
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                     全屏内容区域                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Task 1: 后端 - 新增大屏统计 API 接口

**Files:**
- Create: `carview-server/src/main/java/com/carview/server/controller/DashboardController.java`
- Create: `carview-server/src/main/java/com/carview/server/service/DashboardService.java`
- Create: `carview-server/src/main/java/com/carview/server/mapper/DashboardMapper.java`

### Step 1: 创建 DashboardMapper

```java
// carview-server/src/main/java/com/carview/server/mapper/DashboardMapper.java
package com.carview.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

    // ========== 车辆监控大屏 ==========

    /** 在线/离线车辆统计 */
    @Select("SELECT online_status AS status, COUNT(*) AS count FROM vehicle_realtime_state GROUP BY online_status")
    List<Map<String, Object>> getOnlineOfflineCount();

    /** 车辆品牌分布（饼图） */
    @Select("SELECT brand AS name, COUNT(*) AS value FROM vehicle_info GROUP BY brand ORDER BY value DESC LIMIT 10")
    List<Map<String, Object>> getVehicleBrandDistribution();

    /** 每月行驶车辆数趋势（近12个月） */
    @Select("SELECT DATE_FORMAT(stat_date, '%Y-%m') AS month, COUNT(DISTINCT vehicle_id) AS count " +
            "FROM offline_daily_stats GROUP BY month ORDER BY month DESC LIMIT 12")
    List<Map<String, Object>> getMonthlyActiveVehicles();

    /** 每月行驶总里程趋势（近12个月） */
    @Select("SELECT DATE_FORMAT(stat_date, '%Y-%m') AS month, ROUND(SUM(daily_mileage), 1) AS totalMileage " +
            "FROM offline_daily_stats GROUP BY month ORDER BY month DESC LIMIT 12")
    List<Map<String, Object>> getMonthlyMileageTrend();

    /** 车辆行驶总时长、总里程汇总 */
    @Select("SELECT COALESCE(SUM(driving_duration), 0) AS totalDuration, " +
            "COALESCE(ROUND(SUM(daily_mileage), 0), 0) AS totalMileage, " +
            "COALESCE(ROUND(AVG(daily_mileage), 0), 0) AS avgMileage, " +
            "COALESCE(ROUND(AVG(driving_duration), 0), 0) AS avgDuration " +
            "FROM offline_daily_stats")
    Map<String, Object> getDrivingSummary();

    /** 平台车辆总数 */
    @Select("SELECT COUNT(*) FROM vehicle_info")
    int getTotalVehicleCount();

    /** 当前在线车辆数 */
    @Select("SELECT COUNT(*) FROM vehicle_realtime_state WHERE online_status = 1")
    int getOnlineVehicleCount();

    /** 行驶中车辆数（速度 > 0） */
    @Select("SELECT COUNT(*) FROM vehicle_realtime_state WHERE online_status = 1 AND speed > 0")
    int getDrivingVehicleCount();

    /** 告警类型统计（横向柱状图） */
    @Select("SELECT alarm_type AS type, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY alarm_type ORDER BY count DESC")
    List<Map<String, Object>> getAlarmTypeStats();

    /** 今日告警数 */
    @Select("SELECT COUNT(*) FROM alarm_event WHERE DATE(event_time) = CURDATE()")
    int getTodayAlarmCount();

    // ========== 故障诊断大屏 ==========

    /** 故障等级分布（饼图） */
    @Select("SELECT alarm_level AS level, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY alarm_level")
    List<Map<String, Object>> getAlarmLevelDistribution();

    /** 每日告警趋势（近30天） */
    @Select("SELECT DATE(event_time) AS date, COUNT(*) AS count FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(event_time) ORDER BY date")
    List<Map<String, Object>> getDailyAlarmTrend();

    /** 每月告警次数（当月/当年/当日） */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM alarm_event WHERE DATE(event_time) = CURDATE()) AS todayCount, " +
            "(SELECT COUNT(*) FROM alarm_event WHERE DATE_FORMAT(event_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')) AS monthCount, " +
            "(SELECT COUNT(*) FROM alarm_event WHERE YEAR(event_time) = YEAR(NOW())) AS yearCount")
    Map<String, Object> getAlarmSummary();

    /** 故障车辆数统计 */
    @Select("SELECT " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE DATE(event_time) = CURDATE()) AS todayFaultVehicles, " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE DATE_FORMAT(event_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')) AS monthFaultVehicles, " +
            "(SELECT COUNT(DISTINCT vehicle_id) FROM alarm_event WHERE YEAR(event_time) = YEAR(NOW())) AS yearFaultVehicles")
    Map<String, Object> getFaultVehicleSummary();

    /** 故障类型排行TOP10 */
    @Select("SELECT alarm_type AS faultName, COUNT(*) AS faultCount FROM alarm_event " +
            "WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY alarm_type ORDER BY faultCount DESC LIMIT 10")
    List<Map<String, Object>> getFaultTypeRanking();

    /** 最近10条故障车辆信息 */
    @Select("SELECT a.vehicle_id AS vehicleId, v.vin, v.model AS vehicleModel, " +
            "a.event_time AS eventTime, a.alarm_type AS faultName, " +
            "a.alarm_level AS level, a.alarm_message AS location " +
            "FROM alarm_event a LEFT JOIN vehicle_info v ON a.vehicle_id = v.vehicle_id " +
            "ORDER BY a.event_time DESC LIMIT 10")
    List<Map<String, Object>> getRecentFaultVehicles();

    /** 各品牌故障次数（近一月） */
    @Select("SELECT v.brand, COUNT(*) AS count FROM alarm_event a " +
            "LEFT JOIN vehicle_info v ON a.vehicle_id = v.vehicle_id " +
            "WHERE a.event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY v.brand ORDER BY count DESC LIMIT 8")
    List<Map<String, Object>> getFaultByBrand();

    /** 速度分布统计（实时） */
    @Select("SELECT " +
            "SUM(CASE WHEN speed BETWEEN 0 AND 30 THEN 1 ELSE 0 END) AS 'range_0_30', " +
            "SUM(CASE WHEN speed BETWEEN 31 AND 60 THEN 1 ELSE 0 END) AS 'range_31_60', " +
            "SUM(CASE WHEN speed BETWEEN 61 AND 90 THEN 1 ELSE 0 END) AS 'range_61_90', " +
            "SUM(CASE WHEN speed BETWEEN 91 AND 120 THEN 1 ELSE 0 END) AS 'range_91_120', " +
            "SUM(CASE WHEN speed > 120 THEN 1 ELSE 0 END) AS 'range_120_plus' " +
            "FROM vehicle_realtime_state WHERE online_status = 1")
    Map<String, Object> getSpeedDistribution();

    /** 车辆实时位置列表 */
    @Select("SELECT vehicle_id AS vehicleId, lng, lat, speed, online_status AS onlineStatus " +
            "FROM vehicle_realtime_state WHERE lng IS NOT NULL AND lat IS NOT NULL")
    List<Map<String, Object>> getVehiclePositions();
}
```

### Step 2: 创建 DashboardService

```java
// carview-server/src/main/java/com/carview/server/service/DashboardService.java
package com.carview.server.service;

import com.carview.server.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    /** 车辆监控大屏数据 */
    public Map<String, Object> getMonitorData() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalVehicles", dashboardMapper.getTotalVehicleCount());
        result.put("onlineVehicles", dashboardMapper.getOnlineVehicleCount());
        result.put("drivingVehicles", dashboardMapper.getDrivingVehicleCount());
        result.put("todayAlarms", dashboardMapper.getTodayAlarmCount());
        result.put("drivingSummary", dashboardMapper.getDrivingSummary());
        result.put("brandDistribution", dashboardMapper.getVehicleBrandDistribution());
        result.put("monthlyActiveVehicles", dashboardMapper.getMonthlyActiveVehicles());
        result.put("monthlyMileageTrend", dashboardMapper.getMonthlyMileageTrend());
        result.put("alarmTypeStats", dashboardMapper.getAlarmTypeStats());
        result.put("speedDistribution", dashboardMapper.getSpeedDistribution());
        result.put("vehiclePositions", dashboardMapper.getVehiclePositions());
        return result;
    }

    /** 故障诊断大屏数据 */
    public Map<String, Object> getFaultData() {
        Map<String, Object> result = new HashMap<>();
        result.put("alarmSummary", dashboardMapper.getAlarmSummary());
        result.put("faultVehicleSummary", dashboardMapper.getFaultVehicleSummary());
        result.put("alarmLevelDistribution", dashboardMapper.getAlarmLevelDistribution());
        result.put("dailyAlarmTrend", dashboardMapper.getDailyAlarmTrend());
        result.put("faultTypeRanking", dashboardMapper.getFaultTypeRanking());
        result.put("recentFaultVehicles", dashboardMapper.getRecentFaultVehicles());
        result.put("faultByBrand", dashboardMapper.getFaultByBrand());
        result.put("vehiclePositions", dashboardMapper.getVehiclePositions());
        return result;
    }
}
```

### Step 3: 创建 DashboardController

```java
// carview-server/src/main/java/com/carview/server/controller/DashboardController.java
package com.carview.server.controller;

import com.carview.server.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monitor")
    public Map<String, Object> getMonitorData() {
        return dashboardService.getMonitorData();
    }

    @GetMapping("/fault")
    public Map<String, Object> getFaultData() {
        return dashboardService.getFaultData();
    }
}
```

### Step 4: Commit

```bash
git add carview-server/src/main/java/com/carview/server/mapper/DashboardMapper.java \
        carview-server/src/main/java/com/carview/server/service/DashboardService.java \
        carview-server/src/main/java/com/carview/server/controller/DashboardController.java
git commit -m "feat: add dashboard statistics API for data visualization screens"
```

---

## Task 2: 前端 - 新增大屏 API 接口 + 改造路由和布局

**Files:**
- Create: `carview-web/src/api/dashboard.ts`
- Modify: `carview-web/src/router/index.ts`
- Modify: `carview-web/src/App.vue`
- Modify: `carview-web/src/styles/global.scss`

### Step 1: 创建前端 API

```typescript
// carview-web/src/api/dashboard.ts
import request from './request'

export function getMonitorData() {
  return request.get('/dashboard/monitor')
}

export function getFaultData() {
  return request.get('/dashboard/fault')
}
```

### Step 2: 改造路由

移除 vehicle、fence、alarm 管理路由，改为 monitor 和 fault 两个大屏路由。

```typescript
// carview-web/src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/monitor'
  },
  {
    path: '/monitor',
    name: 'Monitor',
    component: () => import('@/views/monitor/index.vue'),
    meta: { title: '车辆监控', icon: 'Monitor' }
  },
  {
    path: '/fault',
    name: 'Fault',
    component: () => import('@/views/fault/index.vue'),
    meta: { title: '故障诊断', icon: 'Warning' }
  },
  {
    path: '/track',
    name: 'Track',
    component: () => import('@/views/track/index.vue'),
    meta: { title: '轨迹回放', icon: 'MapLocation' }
  },
  {
    path: '/assistant',
    name: 'Assistant',
    component: () => import('@/views/assistant/index.vue'),
    meta: { title: '智能助手', icon: 'ChatDotRound' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### Step 3: 改造 App.vue 布局

移除侧边栏，改为全屏布局 + 顶部导航条（深色科技风）。

```vue
<!-- carview-web/src/App.vue -->
<template>
  <div class="app-fullscreen">
    <header class="app-topbar">
      <div class="topbar-left">
        <span class="topbar-logo">CarView</span>
        <span class="topbar-title">智能车联网监控系统</span>
      </div>
      <nav class="topbar-nav">
        <router-link v-for="route in menuRoutes" :key="route.path" :to="route.path"
                     class="nav-item" active-class="active">
          <el-icon><component :is="route.meta?.icon" /></el-icon>
          <span>{{ route.meta?.title }}</span>
        </router-link>
      </nav>
      <div class="topbar-right">
        <span class="datetime">{{ currentTime }}</span>
      </div>
    </header>
    <main class="app-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menuRoutes = router.getRoutes().filter(r => r.meta?.title)

const currentTime = ref('')
let timer: number | null = null

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

onMounted(() => {
  updateTime()
  timer = window.setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
```

### Step 4: 更新 global.scss

移除 `.app-sidebar` 相关样式，新增 `.app-fullscreen` 和 `.app-topbar` 样式。

```scss
// 替换 .app-layout 及 .app-sidebar 相关样式为：

.app-fullscreen {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
}

.app-topbar {
  height: 48px;
  background: linear-gradient(90deg, rgba(10, 17, 40, 0.95), rgba(20, 40, 80, 0.95));
  border-bottom: 1px solid rgba(0, 242, 255, 0.2);
  display: flex;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 100;

  .topbar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .topbar-logo {
      font-size: 18px;
      font-weight: bold;
      color: $accent-blue;
      letter-spacing: 2px;
    }

    .topbar-title {
      font-size: 14px;
      color: $text-secondary;
      padding-left: 12px;
      border-left: 1px solid rgba(0, 242, 255, 0.2);
    }
  }

  .topbar-nav {
    flex: 1;
    display: flex;
    justify-content: center;
    gap: 4px;

    .nav-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 16px;
      border-radius: 4px;
      font-size: 13px;
      color: $text-secondary;
      text-decoration: none;
      transition: all 0.2s;

      &:hover {
        background: rgba(0, 242, 255, 0.08);
        color: $accent-blue;
      }

      &.active {
        background: rgba(0, 242, 255, 0.15);
        color: $accent-blue;
      }
    }
  }

  .topbar-right {
    .datetime {
      font-size: 13px;
      color: $text-secondary;
      font-variant-numeric: tabular-nums;
    }
  }
}

.app-content {
  flex: 1;
  overflow: hidden;
}
```

### Step 5: Commit

```bash
git add carview-web/src/api/dashboard.ts carview-web/src/router/index.ts \
        carview-web/src/App.vue carview-web/src/styles/global.scss
git commit -m "feat: restructure frontend layout to fullscreen dashboard with top navigation"
```

---

## Task 3: 前端 - 车辆监控大屏页面（对应图片2）

这是最大的一个 Task，创建完整的车辆监控大屏。

**Files:**
- Create: `carview-web/src/views/monitor/index.vue`
- Create: `carview-web/src/views/monitor/components/KpiCards.vue`
- Create: `carview-web/src/views/monitor/components/VehicleMap.vue`
- Create: `carview-web/src/views/monitor/components/BrandPieChart.vue`
- Create: `carview-web/src/views/monitor/components/MonthlyVehicleChart.vue`
- Create: `carview-web/src/views/monitor/components/MileageTrendChart.vue`
- Create: `carview-web/src/views/monitor/components/SpeedDistChart.vue`
- Create: `carview-web/src/views/monitor/components/AlarmTypeChart.vue`

### 页面布局

```
┌──────────────┬──────────────────────────┬──────────────────┐
│  KPI 数字卡片 │  平台概览 KPI（4个数字）   │  品牌饼图        │
│  (行驶时长    │                          │  车辆用途饼图     │
│   总里程     │                          │                  │
│   平均值)    │                          │                  │
├──────────────┤  中国地图（车辆实时分布）   ├──────────────────┤
│  车辆行驶数量 │                          │  车系行程趋势     │
│  月趋势折线图 │                          │  折线图           │
├──────────────┤                          ├──────────────────┤
│  行程趋势    │                          │  告警类型统计     │
│  柱状图      │                          │  横向柱状图       │
└──────────────┴──────────────────────────┴──────────────────┘
```

### Step 1: 创建 monitor/index.vue 主页面

```vue
<!-- carview-web/src/views/monitor/index.vue -->
<template>
  <div class="monitor-screen">
    <!-- 左列 -->
    <div class="col col-left">
      <KpiCards :data="monitorData" />
      <MonthlyVehicleChart :data="monitorData.monthlyActiveVehicles" />
      <SpeedDistChart :data="monitorData.speedDistribution" />
    </div>
    <!-- 中列 -->
    <div class="col col-center">
      <div class="center-kpi">
        <div class="kpi-item">
          <div class="kpi-num">{{ monitorData.totalVehicles || 0 }}</div>
          <div class="kpi-label">平台车辆总数</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-green">{{ monitorData.onlineVehicles || 0 }}</div>
          <div class="kpi-label">平台在线车辆</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-yellow">{{ monitorData.drivingVehicles || 0 }}</div>
          <div class="kpi-label">平台行驶中车辆</div>
        </div>
        <div class="kpi-item">
          <div class="kpi-num accent-red">{{ monitorData.todayAlarms || 0 }}</div>
          <div class="kpi-label">今日告警数</div>
        </div>
      </div>
      <VehicleMap :positions="monitorData.vehiclePositions" />
    </div>
    <!-- 右列 -->
    <div class="col col-right">
      <BrandPieChart :data="monitorData.brandDistribution" />
      <MileageTrendChart :data="monitorData.monthlyMileageTrend" />
      <AlarmTypeChart :data="monitorData.alarmTypeStats" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getMonitorData } from '@/api/dashboard'
import KpiCards from './components/KpiCards.vue'
import VehicleMap from './components/VehicleMap.vue'
import BrandPieChart from './components/BrandPieChart.vue'
import MonthlyVehicleChart from './components/MonthlyVehicleChart.vue'
import MileageTrendChart from './components/MileageTrendChart.vue'
import SpeedDistChart from './components/SpeedDistChart.vue'
import AlarmTypeChart from './components/AlarmTypeChart.vue'

const monitorData = ref<any>({})
let refreshTimer: number | null = null

async function fetchData() {
  try {
    monitorData.value = await getMonitorData()
  } catch (e) {
    console.error('获取监控数据失败', e)
  }
}

onMounted(() => {
  fetchData()
  refreshTimer = window.setInterval(fetchData, 10000) // 10秒刷新
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped lang="scss">
.monitor-screen {
  height: 100%;
  display: grid;
  grid-template-columns: 280px 1fr 300px;
  gap: 12px;
  padding: 12px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.col-center {
  .center-kpi {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }
}

.kpi-item {
  background: rgba(20, 30, 60, 0.7);
  border: 1px solid rgba(0, 242, 255, 0.15);
  border-radius: 8px;
  padding: 12px;
  text-align: center;

  .kpi-num {
    font-size: 28px;
    font-weight: bold;
    color: #00f2ff;
    font-variant-numeric: tabular-nums;
    &.accent-green { color: #70ff00; }
    &.accent-yellow { color: #faad14; }
    &.accent-red { color: #ff4d4f; }
  }
  .kpi-label {
    font-size: 12px;
    color: rgba(230, 247, 255, 0.65);
    margin-top: 4px;
  }
}
</style>
```

### Step 2: 创建 KpiCards.vue（左上大数字卡片）

展示行驶时长总数、平均时长、总里程、平均里程四个大数字，类似图片2左上角。

```vue
<!-- carview-web/src/views/monitor/components/KpiCards.vue -->
<template>
  <div class="kpi-cards">
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num">{{ formatNum(summary.totalDuration) }}</div>
        <div class="big-label">行驶时长总数 (h)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-green">{{ formatNum(summary.avgDuration) }}</div>
        <div class="big-label">行驶时长平均数 (h)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-yellow">{{ formatNum(summary.totalMileage) }}</div>
        <div class="big-label">行驶总里程 (km)</div>
      </div>
    </DataCard>
    <DataCard glow>
      <div class="big-kpi">
        <div class="big-num accent-purple">{{ formatNum(summary.avgMileage) }}</div>
        <div class="big-label">行驶平均里程 (km)</div>
      </div>
    </DataCard>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data: any }>()

const summary = computed(() => props.data?.drivingSummary || {})

function formatNum(n: any): string {
  const num = Number(n) || 0
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}
</script>

<style scoped lang="scss">
.kpi-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.big-kpi {
  text-align: center;
  padding: 8px 0;
  .big-num {
    font-size: 28px;
    font-weight: bold;
    color: #00f2ff;
    line-height: 1.3;
    font-variant-numeric: tabular-nums;
    &.accent-green { color: #70ff00; }
    &.accent-yellow { color: #faad14; }
    &.accent-purple { color: #b37feb; }
  }
  .big-label {
    font-size: 11px;
    color: rgba(230, 247, 255, 0.55);
    margin-top: 4px;
  }
}
</style>
```

### Step 3: 创建 VehicleMap.vue（中央地图）

复用 Leaflet 暗色地图，接收后端车辆位置数据渲染散点。

```vue
<!-- carview-web/src/views/monitor/components/VehicleMap.vue -->
<template>
  <DataCard title="车辆实时分布" class="map-card">
    <div ref="mapRef" class="map-view"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ positions?: any[] }>()

const mapRef = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const markerLayer = shallowRef<L.LayerGroup | null>(null)

function getColor(speed: number): string {
  if (speed > 120) return '#ff4d4f'
  if (speed > 80) return '#faad14'
  return '#70ff00'
}

function renderMarkers(data: any[]) {
  if (!map.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  data.forEach(v => {
    if (!v.lng || !v.lat) return
    const color = getColor(v.speed || 0)
    L.circleMarker([v.lat, v.lng], {
      radius: 5, fillColor: color, color: color,
      weight: 1.5, opacity: 0.9, fillOpacity: 0.7
    }).bindTooltip(`${v.vehicleId}<br/>速度: ${(v.speed || 0).toFixed(0)} km/h`, {
      className: 'dark-tooltip'
    }).addTo(markerLayer.value!)
  })
}

watch(() => props.positions, (val) => {
  if (val && val.length > 0) renderMarkers(val)
}, { deep: true })

onMounted(() => {
  if (!mapRef.value) return
  delete (L.Icon.Default.prototype as any)._getIconUrl
  L.Icon.Default.mergeOptions({ iconRetinaUrl: '', iconUrl: '', shadowUrl: '' })

  map.value = L.map(mapRef.value, {
    center: [34.0, 112.0], zoom: 5,
    zoomControl: false, attributionControl: false
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19
  }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
  markerLayer.value = L.layerGroup().addTo(map.value)

  if (props.positions && props.positions.length > 0) {
    renderMarkers(props.positions)
  }
})

onUnmounted(() => { map.value?.remove() })
</script>

<style scoped lang="scss">
.map-card { flex: 1; display: flex; flex-direction: column; }
.map-view { flex: 1; border-radius: 6px; overflow: hidden; background: #0d1b2a; min-height: 0; }
</style>

<style>
.dark-tooltip {
  background: rgba(10, 17, 40, 0.9) !important;
  border: 1px solid rgba(0, 242, 255, 0.3) !important;
  color: #e6f7ff !important;
  font-size: 12px !important;
  padding: 4px 8px !important;
  border-radius: 4px !important;
}
</style>
```

### Step 4: 创建 BrandPieChart.vue（品牌分布饼图）

```vue
<!-- carview-web/src/views/monitor/components/BrandPieChart.vue -->
<template>
  <DataCard title="车辆品牌分布" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const colors = ['#00f2ff', '#70ff00', '#faad14', '#ff4d4f', '#b37feb', '#36cfc9', '#ff85c0', '#597ef7', '#ffc53d', '#95de64']

function buildOption(data: any[]) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    color: colors,
    legend: {
      orient: 'vertical', right: 10, top: 'center',
      textStyle: { color: 'rgba(230,247,255,0.65)', fontSize: 11 }
    },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      center: ['35%', '50%'],
      label: { show: false },
      data: data || [],
      itemStyle: { borderColor: '#0a1128', borderWidth: 2 }
    }]
  }
}

onMounted(() => {
  init(buildOption(props.data || []))
})

watch(() => props.data, (val) => {
  if (val) setOption(buildOption(val))
}, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 5: 创建 MonthlyVehicleChart.vue（月活跃车辆趋势折线图）

```vue
<!-- carview-web/src/views/monitor/components/MonthlyVehicleChart.vue -->
<template>
  <DataCard title="车辆行驶数量趋势" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(rawData: any[]) {
  const sorted = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 45 },
    xAxis: {
      type: 'category',
      data: sorted.map(d => d.month?.substring(5) || ''),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'line', data: sorted.map(d => d.count || 0),
      smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#00f2ff', width: 2 },
      itemStyle: { color: '#00f2ff' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0,242,255,0.25)' },
            { offset: 1, color: 'rgba(0,242,255,0.02)' }
          ]
        }
      }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 6: 创建 MileageTrendChart.vue（里程趋势折线图）

```vue
<!-- carview-web/src/views/monitor/components/MileageTrendChart.vue -->
<template>
  <DataCard title="月度里程趋势" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(rawData: any[]) {
  const sorted = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 50 },
    xAxis: {
      type: 'category',
      data: sorted.map(d => d.month?.substring(5) || ''),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'line', data: sorted.map(d => d.totalMileage || 0),
      smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: '#faad14', width: 2 },
      itemStyle: { color: '#faad14' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(250,173,20,0.25)' },
            { offset: 1, color: 'rgba(250,173,20,0.02)' }
          ]
        }
      }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 7: 创建 SpeedDistChart.vue（速度分布柱状图）

```vue
<!-- carview-web/src/views/monitor/components/SpeedDistChart.vue -->
<template>
  <DataCard title="实时速度分布" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(d: any) {
  const data = d || {}
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 35 },
    xAxis: {
      type: 'category',
      data: ['0-30', '31-60', '61-90', '91-120', '>120'],
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'bar',
      data: [
        data.range_0_30 || 0, data.range_31_60 || 0,
        data.range_61_90 || 0, data.range_91_120 || 0,
        data.range_120_plus || 0
      ],
      itemStyle: {
        color: (params: any) => {
          const colors = ['#70ff00', '#00f2ff', '#faad14', '#ff85c0', '#ff4d4f']
          return colors[params.dataIndex] || '#00f2ff'
        },
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '50%'
    }]
  }
}

onMounted(() => { init(buildOption(props.data)) })
watch(() => props.data, (val) => { setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 8: 创建 AlarmTypeChart.vue（告警类型横向柱状图）

```vue
<!-- carview-web/src/views/monitor/components/AlarmTypeChart.vue -->
<template>
  <DataCard title="车辆报警统计" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const typeMap: Record<string, string> = {
  OVERSPEED: '超速', FENCE_OUT: '越界', FAULT: '故障',
  ENGINE_OVERHEAT: '过热', LOW_BATTERY: '低电压'
}

function buildOption(rawData: any[]) {
  const data = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 8, right: 30, bottom: 8, left: 60 },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(d => typeMap[d.type] || d.type),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    series: [{
      type: 'bar',
      data: data.map(d => d.count || 0),
      itemStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: 'rgba(0,242,255,0.3)' },
            { offset: 1, color: '#00f2ff' }
          ]
        },
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: '55%',
      label: { show: true, position: 'right', color: 'rgba(230,247,255,0.75)', fontSize: 11 }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 9: Commit

```bash
git add carview-web/src/views/monitor/
git commit -m "feat: add vehicle monitoring dashboard screen with KPI cards, map, and charts"
```

---

## Task 4: 前端 - 故障诊断大屏页面（对应图片1）

**Files:**
- Create: `carview-web/src/views/fault/index.vue`
- Create: `carview-web/src/views/fault/components/FaultKpiCards.vue`
- Create: `carview-web/src/views/fault/components/FaultMap.vue`
- Create: `carview-web/src/views/fault/components/AlarmLevelPie.vue`
- Create: `carview-web/src/views/fault/components/DailyAlarmTrend.vue`
- Create: `carview-web/src/views/fault/components/FaultTypeRanking.vue`
- Create: `carview-web/src/views/fault/components/RecentFaultTable.vue`
- Create: `carview-web/src/views/fault/components/FaultByBrandChart.vue`

### 页面布局

```
┌──────────────────┬──────────────────────────┬──────────────────┐
│  故障KPI数字      │  全国故障车辆分布（地图）   │  每日告警趋势     │
│  (当月/当年/当日) │                          │  折线图           │
│                  │                          │                  │
├──────────────────┤                          ├──────────────────┤
│  故障等级饼图     │                          │  各品牌故障数     │
│                  │                          │  柱状图           │
├──────────────────┼──────────────────────────┼──────────────────┤
│  故障类型排行     │  最近10条故障车辆信息表格  │  告警等级分布饼图  │
│  横向柱状图       │                          │  (小饼图)         │
└──────────────────┴──────────────────────────┴──────────────────┘
```

### Step 1: 创建 fault/index.vue 主页面

```vue
<!-- carview-web/src/views/fault/index.vue -->
<template>
  <div class="fault-screen">
    <!-- 左列 -->
    <div class="col col-left">
      <FaultKpiCards :alarmSummary="faultData.alarmSummary"
                     :faultVehicleSummary="faultData.faultVehicleSummary" />
      <AlarmLevelPie :data="faultData.alarmLevelDistribution" />
      <FaultTypeRanking :data="faultData.faultTypeRanking" />
    </div>
    <!-- 中列 -->
    <div class="col col-center">
      <FaultMap :positions="faultData.vehiclePositions" />
      <RecentFaultTable :data="faultData.recentFaultVehicles" />
    </div>
    <!-- 右列 -->
    <div class="col col-right">
      <DailyAlarmTrend :data="faultData.dailyAlarmTrend" />
      <FaultByBrandChart :data="faultData.faultByBrand" />
      <AlarmLevelPie :data="faultData.alarmLevelDistribution" title="故障等级占比(近一月)" variant="doughnut" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getFaultData } from '@/api/dashboard'
import FaultKpiCards from './components/FaultKpiCards.vue'
import FaultMap from './components/FaultMap.vue'
import AlarmLevelPie from './components/AlarmLevelPie.vue'
import DailyAlarmTrend from './components/DailyAlarmTrend.vue'
import FaultTypeRanking from './components/FaultTypeRanking.vue'
import RecentFaultTable from './components/RecentFaultTable.vue'
import FaultByBrandChart from './components/FaultByBrandChart.vue'

const faultData = ref<any>({})
let refreshTimer: number | null = null

async function fetchData() {
  try {
    faultData.value = await getFaultData()
  } catch (e) {
    console.error('获取故障数据失败', e)
  }
}

onMounted(() => {
  fetchData()
  refreshTimer = window.setInterval(fetchData, 10000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped lang="scss">
.fault-screen {
  height: 100%;
  display: grid;
  grid-template-columns: 280px 1fr 300px;
  grid-template-rows: 1fr;
  gap: 12px;
  padding: 12px;
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.col-center {
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 12px;
}
</style>
```

### Step 2: 创建 FaultKpiCards.vue（故障/报警 KPI 数字卡片）

展示类似图片1左上：故障次数（当月/年/日）、报警次数（当月/年/日）、故障车辆数。

```vue
<!-- carview-web/src/views/fault/components/FaultKpiCards.vue -->
<template>
  <DataCard class="fault-kpi">
    <div class="kpi-row">
      <div class="kpi-block">
        <div class="kpi-title">全国故障次数</div>
        <div class="kpi-nums">
          <div class="kpi-cell">
            <span class="num">{{ alarm.monthCount || 0 }}</span>
            <span class="dim">当月</span>
          </div>
          <div class="kpi-cell">
            <span class="num">{{ alarm.yearCount || 0 }}</span>
            <span class="dim">当年</span>
          </div>
          <div class="kpi-cell">
            <span class="num accent-red">{{ alarm.todayCount || 0 }}</span>
            <span class="dim">当日</span>
          </div>
        </div>
      </div>
      <div class="kpi-block">
        <div class="kpi-title">故障车辆数</div>
        <div class="kpi-nums">
          <div class="kpi-cell">
            <span class="num">{{ fv.monthFaultVehicles || 0 }}</span>
            <span class="dim">当月</span>
          </div>
          <div class="kpi-cell">
            <span class="num">{{ fv.yearFaultVehicles || 0 }}</span>
            <span class="dim">当年</span>
          </div>
          <div class="kpi-cell">
            <span class="num accent-yellow">{{ fv.todayFaultVehicles || 0 }}</span>
            <span class="dim">当日</span>
          </div>
        </div>
      </div>
    </div>
  </DataCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ alarmSummary?: any; faultVehicleSummary?: any }>()
const alarm = computed(() => props.alarmSummary || {})
const fv = computed(() => props.faultVehicleSummary || {})
</script>

<style scoped lang="scss">
.fault-kpi { padding: 12px; }
.kpi-row { display: flex; flex-direction: column; gap: 14px; }
.kpi-block {
  .kpi-title {
    font-size: 12px;
    color: rgba(230, 247, 255, 0.55);
    margin-bottom: 8px;
  }
}
.kpi-nums {
  display: flex;
  gap: 12px;
  .kpi-cell {
    flex: 1;
    text-align: center;
    .num {
      display: block;
      font-size: 24px;
      font-weight: bold;
      color: #00f2ff;
      font-variant-numeric: tabular-nums;
      &.accent-red { color: #ff4d4f; }
      &.accent-yellow { color: #faad14; }
    }
    .dim {
      font-size: 11px;
      color: rgba(230, 247, 255, 0.45);
    }
  }
}
</style>
```

### Step 3: 创建 FaultMap.vue（故障车辆分布地图）

复用 Leaflet 暗色地图，展示所有有告警的车辆位置。红色点表示故障车辆。

```vue
<!-- carview-web/src/views/fault/components/FaultMap.vue -->
<template>
  <DataCard title="全国故障车辆分布" class="map-card">
    <div ref="mapRef" class="map-view"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, shallowRef, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ positions?: any[] }>()
const mapRef = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const markerLayer = shallowRef<L.LayerGroup | null>(null)

function renderMarkers(data: any[]) {
  if (!map.value || !markerLayer.value) return
  markerLayer.value.clearLayers()
  data.forEach(v => {
    if (!v.lng || !v.lat) return
    L.circleMarker([v.lat, v.lng], {
      radius: 5, fillColor: '#ff6b6b', color: '#ff4d4f',
      weight: 1.5, opacity: 0.8, fillOpacity: 0.6
    }).bindTooltip(`${v.vehicleId}`, {
      className: 'dark-tooltip'
    }).addTo(markerLayer.value!)
  })
}

watch(() => props.positions, (val) => {
  if (val && val.length > 0) renderMarkers(val)
}, { deep: true })

onMounted(() => {
  if (!mapRef.value) return
  delete (L.Icon.Default.prototype as any)._getIconUrl
  L.Icon.Default.mergeOptions({ iconRetinaUrl: '', iconUrl: '', shadowUrl: '' })
  map.value = L.map(mapRef.value, {
    center: [34.0, 112.0], zoom: 5,
    zoomControl: false, attributionControl: false
  })
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', { maxZoom: 19 }).addTo(map.value)
  L.control.zoom({ position: 'topright' }).addTo(map.value)
  markerLayer.value = L.layerGroup().addTo(map.value)
  if (props.positions?.length) renderMarkers(props.positions)
})

onUnmounted(() => { map.value?.remove() })
</script>

<style scoped lang="scss">
.map-card { flex: 1; display: flex; flex-direction: column; }
.map-view { flex: 1; border-radius: 6px; overflow: hidden; background: #0d1b2a; min-height: 0; }
</style>

<style>
.dark-tooltip {
  background: rgba(10, 17, 40, 0.9) !important;
  border: 1px solid rgba(0, 242, 255, 0.3) !important;
  color: #e6f7ff !important;
  font-size: 12px !important;
}
</style>
```

### Step 4: 创建 AlarmLevelPie.vue（告警等级饼图）

```vue
<!-- carview-web/src/views/fault/components/AlarmLevelPie.vue -->
<template>
  <DataCard :title="title || '故障等级分布'" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[]; title?: string; variant?: string }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const levelNames: Record<number, string> = { 1: '一级(严重)', 2: '二级(警告)', 3: '三级(提示)' }
const levelColors: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#00f2ff' }

function buildOption(rawData: any[]) {
  const data = (rawData || []).map(d => ({
    name: levelNames[d.level] || `级别${d.level}`,
    value: d.count || 0,
    itemStyle: { color: levelColors[d.level] || '#00f2ff' }
  }))
  const radius = props.variant === 'doughnut' ? ['45%', '70%'] : ['0%', '65%']
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 5, left: 'center',
      textStyle: { color: 'rgba(230,247,255,0.65)', fontSize: 11 }
    },
    series: [{
      type: 'pie', radius,
      label: { show: true, color: 'rgba(230,247,255,0.65)', fontSize: 11, formatter: '{d}%' },
      data, itemStyle: { borderColor: '#0a1128', borderWidth: 2 }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 5: 创建 DailyAlarmTrend.vue（每日告警趋势折线图）

```vue
<!-- carview-web/src/views/fault/components/DailyAlarmTrend.vue -->
<template>
  <DataCard title="告警趋势(近30天)" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

function buildOption(rawData: any[]) {
  const data = rawData || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 24, left: 40 },
    xAxis: {
      type: 'category',
      data: data.map(d => (d.date || '').substring(5)),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 9, rotate: 30 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'line', data: data.map(d => d.count || 0),
      smooth: true, symbol: 'none',
      lineStyle: { color: '#ff4d4f', width: 2 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(255,77,79,0.3)' },
            { offset: 1, color: 'rgba(255,77,79,0.02)' }
          ]
        }
      }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 6: 创建 FaultTypeRanking.vue（故障类型排行横向柱状图）

```vue
<!-- carview-web/src/views/fault/components/FaultTypeRanking.vue -->
<template>
  <DataCard title="故障类型排行" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const typeMap: Record<string, string> = {
  OVERSPEED: '超速', FENCE_OUT: '围栏越界', FAULT: '故障码',
  ENGINE_OVERHEAT: '发动机过热', LOW_BATTERY: '电池低压'
}

function buildOption(rawData: any[]) {
  const data = [...(rawData || [])].reverse()
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 8, right: 40, bottom: 8, left: 80 },
    xAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(d => typeMap[d.faultName] || d.faultName),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 11 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    series: [{
      type: 'bar', data: data.map(d => d.faultCount || 0),
      itemStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: 'rgba(255,77,79,0.3)' },
            { offset: 1, color: '#ff4d4f' }
          ]
        },
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: '55%',
      label: { show: true, position: 'right', color: 'rgba(230,247,255,0.75)', fontSize: 11 }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 7: 创建 RecentFaultTable.vue（最近故障车辆信息表格）

```vue
<!-- carview-web/src/views/fault/components/RecentFaultTable.vue -->
<template>
  <DataCard title="最近故障车辆信息" class="table-card">
    <el-table :data="rows" size="small" stripe
              :header-cell-style="{ background: 'rgba(0,242,255,0.05)', color: 'rgba(230,247,255,0.65)' }"
              :cell-style="{ color: 'rgba(230,247,255,0.85)' }">
      <el-table-column type="index" label="序号" width="50" />
      <el-table-column prop="vehicleId" label="车牌号" width="95" />
      <el-table-column prop="vin" label="VIN" width="170" />
      <el-table-column prop="vehicleModel" label="车型" width="80" />
      <el-table-column prop="eventTime" label="故障发生时间" width="160" />
      <el-table-column prop="faultName" label="故障名称" width="100">
        <template #default="{ row }">
          {{ typeMap[row.faultName] || row.faultName }}
        </template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="70">
        <template #default="{ row }">
          <el-tag :type="levelType[row.level]" size="small">{{ levelText[row.level] }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </DataCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const rows = computed(() => props.data || [])

const typeMap: Record<string, string> = {
  OVERSPEED: '超速', FENCE_OUT: '围栏越界', FAULT: '故障码',
  ENGINE_OVERHEAT: '发动机过热', LOW_BATTERY: '电池低压'
}
const levelText: Record<number, string> = { 1: '严重', 2: '警告', 3: '提示' }
const levelType: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
</script>

<style scoped lang="scss">
.table-card {
  max-height: 280px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
```

### Step 8: 创建 FaultByBrandChart.vue（各品牌故障数柱状图）

```vue
<!-- carview-web/src/views/fault/components/FaultByBrandChart.vue -->
<template>
  <DataCard title="各品牌故障数(近一月)" class="chart-card">
    <div ref="chartRef" class="chart-container"></div>
  </DataCard>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useEcharts } from '@/hooks/useEcharts'
import DataCard from '@/components/DataCard.vue'

const props = defineProps<{ data?: any[] }>()
const chartRef = ref<HTMLElement | null>(null)
const { init, setOption } = useEcharts(() => chartRef.value)

const colors = ['#00f2ff', '#70ff00', '#faad14', '#ff4d4f', '#b37feb', '#36cfc9', '#ff85c0', '#597ef7']

function buildOption(rawData: any[]) {
  const data = rawData || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 10, right: 10, bottom: 30, left: 40 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.brand || ''),
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10, rotate: 20 },
      axisLine: { lineStyle: { color: 'rgba(0,242,255,0.15)' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(230,247,255,0.65)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(0,242,255,0.08)' } }
    },
    series: [{
      type: 'bar',
      data: data.map((d, i) => ({
        value: d.count || 0,
        itemStyle: { color: colors[i % colors.length] }
      })),
      barWidth: '55%',
      itemStyle: { borderRadius: [4, 4, 0, 0] }
    }]
  }
}

onMounted(() => { init(buildOption(props.data || [])) })
watch(() => props.data, (val) => { if (val) setOption(buildOption(val)) }, { deep: true })
</script>

<style scoped lang="scss">
.chart-card { flex: 1; display: flex; flex-direction: column; }
.chart-container { flex: 1; min-height: 0; }
</style>
```

### Step 9: Commit

```bash
git add carview-web/src/views/fault/
git commit -m "feat: add fault diagnosis dashboard screen with KPIs, map, charts, and fault table"
```

---

## Task 5: 前端 - 清理旧页面 + WebSocket 集成改进

**Files:**
- Delete: `carview-web/src/views/dashboard/` (entire directory)
- Delete: `carview-web/src/views/vehicle/` (entire directory)
- Delete: `carview-web/src/views/fence/` (entire directory)
- Delete: `carview-web/src/views/alarm/` (entire directory)
- Delete: `carview-web/src/api/vehicle.ts`
- Delete: `carview-web/src/api/alarm.ts`
- Delete: `carview-web/src/api/fence.ts`
- Modify: `carview-web/src/store/realtime.ts` - 扩展 store 以支持两个大屏的实时数据
- Modify: `carview-web/src/hooks/useWebSocket.ts` - 改进 WebSocket 支持多种消息类型

### Step 1: 删除旧管理页面

删除以下目录和文件：
- `carview-web/src/views/dashboard/` (旧 dashboard)
- `carview-web/src/views/vehicle/` (车辆 CRUD)
- `carview-web/src/views/fence/` (围栏 CRUD)
- `carview-web/src/views/alarm/` (告警管理 CRUD)
- `carview-web/src/api/vehicle.ts`
- `carview-web/src/api/alarm.ts`
- `carview-web/src/api/fence.ts`

### Step 2: 更新 realtime store

```typescript
// carview-web/src/store/realtime.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useRealtimeStore = defineStore('realtime', () => {
  const onlineCount = ref(0)
  const avgSpeed = ref(0)
  const totalMileage = ref(0)
  const avgFuel = ref(0)
  const alarmCount = ref(0)
  const latestAlarms = ref<any[]>([])

  function updateFromWebSocket(data: any) {
    if (data.onlineCount !== undefined) onlineCount.value = data.onlineCount
    if (data.avgSpeed !== undefined) avgSpeed.value = Math.round(data.avgSpeed * 10) / 10
    if (data.totalMileageIncrement !== undefined) totalMileage.value += data.totalMileageIncrement
    if (data.avgFuelConsumption !== undefined) avgFuel.value = Math.round(data.avgFuelConsumption * 10) / 10
    if (data.alarmCount !== undefined) alarmCount.value += data.alarmCount
    if (data.latestAlarms) latestAlarms.value = data.latestAlarms
  }

  return { onlineCount, avgSpeed, totalMileage, avgFuel, alarmCount, latestAlarms, updateFromWebSocket }
})
```

### Step 3: Commit

```bash
git rm -r carview-web/src/views/dashboard/ carview-web/src/views/vehicle/ \
         carview-web/src/views/fence/ carview-web/src/views/alarm/ \
         carview-web/src/api/vehicle.ts carview-web/src/api/alarm.ts \
         carview-web/src/api/fence.ts
git add carview-web/src/store/realtime.ts
git commit -m "refactor: remove CRUD management pages, keep data visualization focus"
```

---

## Task 6: 前端 - 轨迹回放页面样式调整 + 智能助手样式微调

**Files:**
- Modify: `carview-web/src/views/track/index.vue` - 去除 padding，适应全屏
- Modify: `carview-web/src/views/assistant/index.vue` - 去除 padding，适应全屏

### Step 1: 调整轨迹回放页面

将 `.track-page` 的 padding 改为适应新的全屏布局：

```scss
// 修改 .track-page
.track-page {
  display: flex;
  height: 100%;
  padding: 12px;
  gap: 12px;
}
```

### Step 2: 调整智能助手页面

```scss
// 修改 .assistant-page
.assistant-page { padding: 12px; height: 100%; }
```

### Step 3: Commit

```bash
git add carview-web/src/views/track/index.vue carview-web/src/views/assistant/index.vue
git commit -m "style: adjust track and assistant pages for fullscreen layout"
```

---

## Task 7: 前端 - 数据 Mock 兜底处理

当后端 API 未连接或无数据时，确保大屏不会显示空白。在 API 层或页面层添加 mock 数据兜底。

**Files:**
- Modify: `carview-web/src/views/monitor/index.vue` - 添加 mock 数据兜底
- Modify: `carview-web/src/views/fault/index.vue` - 添加 mock 数据兜底

### Step 1: 在 monitor/index.vue 添加 mock 数据

当 API 请求失败时，使用 mock 数据填充页面，确保展示效果：

```typescript
const defaultMonitorData = {
  totalVehicles: 30,
  onlineVehicles: 22,
  drivingVehicles: 15,
  todayAlarms: 8,
  drivingSummary: { totalDuration: 152400, avgDuration: 10000, totalMileage: 722200, avgMileage: 722 },
  brandDistribution: [
    { name: '一汽大众', value: 4 }, { name: '比亚迪', value: 3 },
    { name: '丰田', value: 3 }, { name: '特斯拉', value: 2 },
    { name: '宝马', value: 2 }, { name: '其他', value: 16 }
  ],
  monthlyActiveVehicles: Array.from({ length: 12 }, (_, i) => ({
    month: `2025-${String(i + 1).padStart(2, '0')}`, count: 15 + Math.floor(Math.random() * 15)
  })),
  monthlyMileageTrend: Array.from({ length: 12 }, (_, i) => ({
    month: `2025-${String(i + 1).padStart(2, '0')}`, totalMileage: 5000 + Math.floor(Math.random() * 10000)
  })),
  alarmTypeStats: [
    { type: 'OVERSPEED', count: 45 }, { type: 'FENCE_OUT', count: 23 },
    { type: 'FAULT', count: 18 }, { type: 'ENGINE_OVERHEAT', count: 12 },
    { type: 'LOW_BATTERY', count: 8 }
  ],
  speedDistribution: { range_0_30: 5, range_31_60: 8, range_61_90: 12, range_91_120: 6, range_120_plus: 2 },
  vehiclePositions: [
    { vehicleId: '京A12345', lng: 116.40, lat: 39.92, speed: 65 },
    { vehicleId: '京A23456', lng: 121.48, lat: 31.24, speed: 110 },
    { vehicleId: '京C12345', lng: 113.28, lat: 23.14, speed: 72 },
    { vehicleId: '京D12345', lng: 114.08, lat: 22.56, speed: 130 },
    { vehicleId: '京E12345', lng: 104.08, lat: 30.68, speed: 35 },
    // ... 更多城市散点
  ]
}

async function fetchData() {
  try {
    const res = await getMonitorData()
    monitorData.value = res
  } catch (e) {
    console.warn('使用 mock 数据', e)
    monitorData.value = defaultMonitorData
  }
}
```

### Step 2: 在 fault/index.vue 添加类似的 mock 数据兜底

（结构类似，包含 alarmSummary、faultVehicleSummary、alarmLevelDistribution 等 mock 数据）

### Step 3: Commit

```bash
git add carview-web/src/views/monitor/index.vue carview-web/src/views/fault/index.vue
git commit -m "feat: add mock data fallback for dashboard screens when API unavailable"
```

---

## 文件变更总览

### 后端新增（3个文件）
| 文件 | 说明 |
|------|------|
| `carview-server/.../mapper/DashboardMapper.java` | 大屏统计 SQL 查询 |
| `carview-server/.../service/DashboardService.java` | 大屏数据聚合服务 |
| `carview-server/.../controller/DashboardController.java` | 大屏 REST API |

### 前端新增（16个文件）
| 文件 | 说明 |
|------|------|
| `carview-web/src/api/dashboard.ts` | 大屏 API 接口 |
| `carview-web/src/views/monitor/index.vue` | 车辆监控大屏主页 |
| `carview-web/src/views/monitor/components/KpiCards.vue` | 大数字 KPI 卡片 |
| `carview-web/src/views/monitor/components/VehicleMap.vue` | 车辆实时分布地图 |
| `carview-web/src/views/monitor/components/BrandPieChart.vue` | 品牌分布饼图 |
| `carview-web/src/views/monitor/components/MonthlyVehicleChart.vue` | 月活跃趋势折线图 |
| `carview-web/src/views/monitor/components/MileageTrendChart.vue` | 月里程趋势折线图 |
| `carview-web/src/views/monitor/components/SpeedDistChart.vue` | 速度分布柱状图 |
| `carview-web/src/views/monitor/components/AlarmTypeChart.vue` | 告警类型横向柱状图 |
| `carview-web/src/views/fault/index.vue` | 故障诊断大屏主页 |
| `carview-web/src/views/fault/components/FaultKpiCards.vue` | 故障 KPI 卡片 |
| `carview-web/src/views/fault/components/FaultMap.vue` | 故障车辆分布地图 |
| `carview-web/src/views/fault/components/AlarmLevelPie.vue` | 告警等级饼图 |
| `carview-web/src/views/fault/components/DailyAlarmTrend.vue` | 每日告警趋势折线图 |
| `carview-web/src/views/fault/components/FaultTypeRanking.vue` | 故障类型排行柱状图 |
| `carview-web/src/views/fault/components/RecentFaultTable.vue` | 最近故障表格 |
| `carview-web/src/views/fault/components/FaultByBrandChart.vue` | 品牌故障数柱状图 |

### 前端修改（4个文件）
| 文件 | 说明 |
|------|------|
| `carview-web/src/router/index.ts` | 路由改为 /monitor /fault /track /assistant |
| `carview-web/src/App.vue` | 布局改为全屏 + 顶部导航 |
| `carview-web/src/styles/global.scss` | 移除侧边栏样式，新增顶部导航样式 |
| `carview-web/src/store/realtime.ts` | 简化 store |

### 前端删除
| 文件/目录 | 说明 |
|-----------|------|
| `carview-web/src/views/dashboard/` | 旧 dashboard（已被 monitor 替代） |
| `carview-web/src/views/vehicle/` | 车辆 CRUD 管理页面 |
| `carview-web/src/views/fence/` | 围栏 CRUD 管理页面 |
| `carview-web/src/views/alarm/` | 告警 CRUD 管理页面 |
| `carview-web/src/api/vehicle.ts` | 车辆 CRUD API |
| `carview-web/src/api/alarm.ts` | 告警 CRUD API |
| `carview-web/src/api/fence.ts` | 围栏 CRUD API |
