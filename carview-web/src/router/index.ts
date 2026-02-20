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
    path: '/fence',
    name: 'Fence',
    component: () => import('@/views/fence/index.vue'),
    meta: { title: '电子围栏', icon: 'Position' }
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
