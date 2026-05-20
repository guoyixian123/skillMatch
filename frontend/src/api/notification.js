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
  return request.put(`/notification/requests/${requestId}/accept`)
}

export function declineRequest(requestId) {
  return request.put(`/notification/requests/${requestId}/decline`)
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}
