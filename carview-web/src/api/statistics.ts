import request from './request'

export function getRealtimeStatistics() {
  return request.get('/statistics/realtime')
}

export function getTrack(vehicleId: string, startTime: string, endTime: string) {
  return request.get(`/vehicles/${vehicleId}/track`, {
    params: { startTime, endTime }
  })
}
