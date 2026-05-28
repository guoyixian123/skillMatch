import request from '@/utils/request'

// --- 好友 ---

export function getFriends() {
  return request.get('/friends')
}

export function getUnreadMessageCount() {
  return request.get('/friends/unread-count')
}

export function removeFriend(friendId) {
  return request.delete(`/friends/${friendId}`)
}

// --- 聊天消息 ---

export function getMessages(friendId, params) {
  return request.get(`/chat/messages/${friendId}`, { params })
}

export function sendMessage(data) {
  return request.post('/chat/messages', data)
}

export function markAsRead(friendId) {
  return request.put(`/chat/messages/${friendId}/read`)
}
