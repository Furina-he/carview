import { ref, onMounted, onUnmounted } from 'vue'
import { useRealtimeStore } from '@/store/realtime'

export function useWebSocket(url: string = 'ws://localhost:8080/ws/realtime') {
  const connected = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: number | null = null
  const realtimeStore = useRealtimeStore()

  function connect() {
    ws = new WebSocket(url)

    ws.onopen = () => {
      connected.value = true
      console.log('WebSocket 已连接')
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        realtimeStore.updateFromWebSocket(data)
      } catch (e) {
        console.error('解析 WebSocket 消息失败', e)
      }
    }

    ws.onclose = () => {
      connected.value = false
      console.log('WebSocket 断开，3秒后重连...')
      reconnectTimer = window.setTimeout(connect, 3000)
    }

    ws.onerror = (error) => {
      console.error('WebSocket 错误', error)
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.close()
      ws = null
    }
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return { connected }
}
