<template>
  <div class="player-control">
    <div class="controls">
      <el-button :icon="playing ? 'VideoPause' : 'VideoPlay'" circle size="small" @click="togglePlay" />
      <el-select v-model="speed" size="small" style="width: 80px">
        <el-option :value="1" label="1x" />
        <el-option :value="2" label="2x" />
        <el-option :value="4" label="4x" />
        <el-option :value="8" label="8x" />
      </el-select>
    </div>
    <el-slider v-model="current" :max="total - 1" :show-tooltip="false" size="small"
               @change="(val: number) => emit('seek', val)" />
    <div class="progress-text">{{ current }} / {{ total }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

const props = defineProps<{ total: number }>()
const emit = defineEmits(['seek'])

const current = ref(0)
const playing = ref(false)
const speed = ref(1)
let timer: number | null = null

function togglePlay() {
  playing.value = !playing.value
  if (playing.value) {
    timer = window.setInterval(() => {
      if (current.value < props.total - 1) {
        current.value++
        emit('seek', current.value)
      } else {
        playing.value = false
        if (timer) clearInterval(timer)
      }
    }, 1000 / speed.value)
  } else if (timer) {
    clearInterval(timer)
  }
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped lang="scss">
.player-control { margin-top: 12px; }
.controls { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.progress-text { font-size: 11px; color: rgba(230, 247, 255, 0.45); text-align: right; margin-top: 4px; }
</style>
