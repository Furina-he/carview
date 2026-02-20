import request from './request'

export interface FenceRule {
  id?: number
  fenceName: string
  fenceType: 'RECTANGLE' | 'CIRCLE'
  minLng?: number
  minLat?: number
  maxLng?: number
  maxLat?: number
  centerLng?: number
  centerLat?: number
  radius?: number
  enabled: number
  createdAt?: string
  updatedAt?: string
}

export function getFenceList() {
  return request.get('/fences')
}

export function createFence(data: FenceRule) {
  return request.post('/fences', data)
}

export function updateFence(id: number, data: FenceRule) {
  return request.put(`/fences/${id}`, data)
}

export function deleteFence(id: number) {
  return request.delete(`/fences/${id}`)
}
