import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useRealtimeStore = defineStore('realtime', () => {
  const onlineCount = ref(0)
  const avgSpeed = ref(0)
  const totalMileage = ref(0)
  const avgFuel = ref(0)
  const alarmCount = ref(0)
  const vehiclePositions = ref<Map<string, { lng: number; lat: number; speed: number }>>(new Map())

  function updateFromWebSocket(data: any) {
    if (data.onlineCount !== undefined) onlineCount.value = data.onlineCount
    if (data.avgSpeed !== undefined) avgSpeed.value = Math.round(data.avgSpeed * 10) / 10
    if (data.totalMileageIncrement !== undefined) totalMileage.value += data.totalMileageIncrement
    if (data.avgFuelConsumption !== undefined) avgFuel.value = Math.round(data.avgFuelConsumption * 10) / 10
    if (data.alarmCount !== undefined) alarmCount.value += data.alarmCount
  }

  function updateVehiclePosition(vehicleId: string, lng: number, lat: number, speed: number) {
    vehiclePositions.value.set(vehicleId, { lng, lat, speed })
  }

  return { onlineCount, avgSpeed, totalMileage, avgFuel, alarmCount, vehiclePositions, updateFromWebSocket, updateVehiclePosition }
})
