import request from './request'

export function getMonitorData() {
  return request.get('/dashboard/monitor')
}

export function getFaultData() {
  return request.get('/dashboard/fault')
}
