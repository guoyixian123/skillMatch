<template>
  <div class="chat-page">
    <!-- Header -->
    <div class="chat-header">
      <button class="brutal-btn outline small back-btn" @click="$router.push('/friends')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <img
        :src="friendInfo.avatarUrl || getDefaultAvatar(friendId)"
        class="brutal-avatar chat-header-avatar"
      />
      <div class="chat-header-name">{{ friendInfo.name || '好友' }}</div>
    </div>

    <!-- Messages -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="chatStore.loading" class="loading-block">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>

      <template v-else>
        <div v-if="chatStore.messages.length === 0" class="chat-empty">
          <div style="font-size:40px;margin-bottom:8px;">💬</div>
          <div style="font-weight:700;color:#888;">开始和好友聊天吧</div>
        </div>

        <div
          v-for="msg in chatStore.messages"
          :key="msg.id"
          class="msg-row"
          :class="msg.fromUserId === currentUserId ? 'msg-row-sent' : 'msg-row-received'"
        >
          <div class="msg-bubble" :class="msg.fromUserId === currentUserId ? 'msg-sent' : 'msg-received'">
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
      </template>
    </div>

    <!-- Input -->
    <div class="chat-input-bar">
      <input
        v-model="inputText"
        class="brutal-input chat-input"
        placeholder="输入消息..."
        @keydown.enter.prevent="handleSend"
        :disabled="chatStore.sending"
      />
      <button
        class="brutal-btn primary send-btn"
        @click="handleSend"
        :disabled="!inputText.trim() || chatStore.sending"
      >
        <el-icon><Promotion /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { ArrowLeft, Loading, Promotion } from '@element-plus/icons-vue'
import { getFriends } from '@/api/friend'

const route = useRoute()
const chatStore = useChatStore()
const authStore = useAuthStore()

const friendId = route.params.friendId
const currentUserId = authStore.user?.userId || authStore.user?.id
const friendInfo = ref({ name: '', avatarUrl: '' })
const inputText = ref('')
const messagesContainer = ref(null)

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  try {
    await chatStore.sendMessageAction(friendId, text)
    scrollToBottom()
  } catch { /* handled */ }
}

watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})

onMounted(async () => {
  // Load friend info
  try {
    const res = await getFriends()
    const list = Array.isArray(res.data) ? res.data : []
    const found = list.find(f => f.userId === friendId)
    if (found) {
      friendInfo.value = { name: found.name, avatarUrl: found.avatarUrl }
    }
  } catch { /* ignore */ }

  await chatStore.fetchMessages(friendId)
  scrollToBottom()
  chatStore.startPolling(friendId)
})

onUnmounted(() => {
  chatStore.stopPolling()
  chatStore.clear()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px); /* subtract navbar */
  background: var(--bg-primary);
}
@media (max-width: 768px) {
  .chat-page {
    height: calc(100dvh - 60px - 56px); /* subtract navbar + mobile nav */
  }
}

/* Header */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 3px solid #1A1A1A;
  flex-shrink: 0;
}
.back-btn {
  padding: 6px 10px !important;
}
.chat-header-avatar {
  width: 36px;
  height: 36px;
}
.chat-header-name {
  font-weight: 900;
  font-size: 16px;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.msg-row {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}
.msg-row-sent {
  align-self: flex-end;
  align-items: flex-end;
}
.msg-row-received {
  align-self: flex-start;
  align-items: flex-start;
}

.msg-bubble {
  padding: 12px 16px;
  border: 2px solid #1A1A1A;
  box-shadow: 3px 3px 0 #1A1A1A;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}
.msg-sent {
  background: var(--color-yellow);
}
.msg-received {
  background: #fff;
}

.msg-time {
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
  padding: 0 4px;
}

/* Input bar */
.chat-input-bar {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  background: #fff;
  border-top: 3px solid #1A1A1A;
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  padding: 10px 14px !important;
  font-size: 14px !important;
}
.send-btn {
  padding: 10px 16px !important;
  flex-shrink: 0;
}
</style>
