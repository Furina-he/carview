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
