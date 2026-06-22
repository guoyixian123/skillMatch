import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMessages, sendMessage, markAsRead, getUnreadMessageCount } from '@/api/friend'
import ws from '@/utils/ws'

export const useChatStore = defineStore('chat', () => {
  const currentFriendId = ref(null)
  const messages = ref([])
  const totalUnread = ref(0)
  const loading = ref(false)
  const sending = ref(false)
  const wsConnected = ref(false)
  let pollTimer = null
  let unsubChat = null       // WebSocket 取消函数
  let unsubOpen = null
  let unsubClose = null

  // ========== WebSocket 实时消息 ==========

  /** 绑定 WebSocket 事件（App.vue 登录后调用） */
  function bindWebSocket() {
    // 监听聊天消息
    unsubChat = ws.on('chat', (msg) => {
      handleIncomingMessage(msg)
    })

    // 监听连接状态
    unsubOpen = ws.on('open', () => {
      wsConnected.value = true
      // 连接成功后立即拉取未读数
      fetchUnreadCount()
      // 如果当前正在聊天，停止轮询（WebSocket 已接管）
      if (currentFriendId.value) {
        stopPolling()
      }
    })

    unsubClose = ws.on('close', () => {
      wsConnected.value = false
      // 断开后如果正在聊天，恢复轮询兜底
      if (currentFriendId.value) {
        startPolling(currentFriendId.value)
      }
    })

    wsConnected.value = ws.connected
  }

  /** 解绑 WebSocket 事件（退出登录时调用） */
  function unbindWebSocket() {
    unsubChat?.()
    unsubOpen?.()
    unsubClose?.()
    unsubChat = null
    unsubOpen = null
    unsubClose = null
    wsConnected.value = false
  }

  /** 处理 WebSocket 推送的聊天消息 */
  function handleIncomingMessage(msg) {
    const fromUserId = msg.fromUserId
    if (!fromUserId) return

    // 如果当前正在和这个好友聊天，直接追加消息并标记已读
    if (currentFriendId.value === fromUserId) {
      const exists = messages.value.some(m => m.id === msg.messageId)
      if (!exists) {
        messages.value.push({
          id: msg.messageId,
          fromUserId: msg.fromUserId,
          content: msg.content,
          createdAt: msg.createdAt,
        })
        // 标记已读，不刷新未读数（已经在聊天了，不需要红点）
        markAsRead(fromUserId).catch(() => {})
      }
    } else {
      // 不在当前聊天页，才更新未读角标
      fetchUnreadCount()
    }
  }

  // ========== HTTP 接口 ==========

  async function fetchMessages(friendId) {
    currentFriendId.value = friendId
    loading.value = true
    try {
      const res = await getMessages(friendId, { limit: 50 })
      messages.value = res.data || []
      await markAsRead(friendId)
      fetchUnreadCount()
    } finally {
      loading.value = false
    }
  }

  async function sendMessageAction(friendId, content) {
    sending.value = true
    try {
      const res = await sendMessage({ toUserId: friendId, content })
      messages.value.push(res.data)
      return res.data
    } finally {
      sending.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadMessageCount()
      totalUnread.value = (typeof res.data === 'number') ? res.data : 0
    } catch { /* ignore */ }
  }

  // ========== 轮询（WebSocket 断开时的兜底） ==========

  function startPolling(friendId, interval = 5000) {
    stopPolling()
    // 如果 WebSocket 已连接，不启动轮询
    if (wsConnected.value) return

    pollTimer = setInterval(async () => {
      if (!messages.value.length) return
      const lastId = messages.value[messages.value.length - 1].id
      try {
        const res = await getMessages(friendId, { afterId: lastId, limit: 50 })
        const newMsgs = res.data || []
        if (newMsgs.length) {
          messages.value.push(...newMsgs)
          await markAsRead(friendId)
        }
      } catch { /* ignore */ }
    }, interval)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function clear() {
    currentFriendId.value = null
    messages.value = []
    stopPolling()
  }

  return {
    currentFriendId, messages, totalUnread, loading, sending, wsConnected,
    fetchMessages, sendMessageAction, fetchUnreadCount,
    startPolling, stopPolling, clear,
    bindWebSocket, unbindWebSocket,
  }
})
