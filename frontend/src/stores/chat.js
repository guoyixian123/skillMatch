import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMessages, sendMessage, markAsRead, getUnreadMessageCount } from '@/api/friend'

export const useChatStore = defineStore('chat', () => {
  const currentFriendId = ref(null)
  const messages = ref([])
  const totalUnread = ref(0)
  const loading = ref(false)
  const sending = ref(false)
  let pollTimer = null

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

  function startPolling(friendId, interval = 3000) {
    stopPolling()
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
    currentFriendId, messages, totalUnread, loading, sending,
    fetchMessages, sendMessageAction, fetchUnreadCount,
    startPolling, stopPolling, clear,
  }
})
