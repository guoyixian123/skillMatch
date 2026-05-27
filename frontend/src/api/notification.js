import request from '@/utils/request'

export function getReceivedRequests(params) {
  return request.get('/notification/requests/received', { params })
}

export function getSentRequests(params) {
  return request.get('/notification/requests/sent', { params })
}

export function createRequest(data) {
  return request.post('/notification/requests', data)
}

export function acceptRequest(requestId) {
  return request.post(`/notification/requests/${requestId}/accept`)
}

export function declineRequest(requestId) {
  return request.post(`/notification/requests/${requestId}/decline`)
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

// --- 点赞通知 ---

export function getLikeNotifications(params) {
  return request.get('/notifications', { params })
}

export function getLikeUnreadCount() {
  return request.get('/notifications/unread-count')
}

export function markLikeRead(id) {
  return request.put(`/notifications/${id}/read`)
}

export function markLikeAllRead() {
  return request.put('/notifications/read-all')
}
