<template>
  <div class="data-card" :style="cardStyle">
    <div class="card-title" v-if="title">{{ title }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title?: string
  glow?: boolean
}>()

const cardStyle = computed(() => ({
  boxShadow: props.glow ? '0 0 15px rgba(0, 242, 255, 0.2)' : '0 4px 20px rgba(0, 0, 0, 0.3)'
}))
</script>

<style scoped lang="scss">
.data-card {
  background: rgba(20, 30, 60, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 242, 255, 0.15);
  border-radius: 8px;
  padding: 16px;

  .card-title {
    font-size: 14px;
    color: rgba(230, 247, 255, 0.65);
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(0, 242, 255, 0.1);
  }
}
</style>
