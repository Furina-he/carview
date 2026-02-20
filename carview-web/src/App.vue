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
import { ref, onMounted, onUnmounted } from 'vue'
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
