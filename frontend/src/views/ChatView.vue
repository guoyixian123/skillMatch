<template>
  <div class="chat-page">
    <!-- Header -->
    <div class="chat-header">
      <button class="geo-btn outline small back-btn" @click="$router.push('/friends')">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <img
        :src="friendInfo.avatarUrl || getDefaultAvatar(friendId)"
        class="geo-avatar chat-header-avatar"
      />
      <div class="chat-header-name">{{ friendInfo.name || '好友' }}</div>
      <div class="ws-status" :class="{ online: chatStore.wsConnected }">
        {{ chatStore.wsConnected ? '⚡ 实时' : '🔄 轮询' }}
      </div>
    </div>

    <!-- Messages -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="chatStore.loading" class="loading-block">
        <el-icon class="is-loading"><Loading /></el-icon> 加载中...
      </div>

      <template v-else>
        <div v-if="chatStore.messages.length === 0" class="chat-empty">
          <div class="empty-icon-circle">💬</div>
          <div style="font-weight:600;color:var(--color-muted-fg);">开始和好友聊天吧</div>
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
        class="geo-input chat-input"
        placeholder="输入消息..."
        @keydown.enter.prevent="handleSend"
        :disabled="chatStore.sending"
      />
      <button
        class="geo-btn primary send-btn"
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
  height: calc(100vh - 64px);
  background: var(--color-bg);
}
@media (max-width: 768px) {
  .chat-page {
    height: calc(100dvh - 64px - 56px);
  }
}

/* Header */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 253, 245, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 2px solid var(--color-border);
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
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 16px;
  color: var(--color-fg);
}
.ws-status {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: #FEE2E2;
  color: #DC2626;
  transition: all 0.3s;
}
.ws-status.online {
  background: #D1FAE5;
  color: #059669;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scrollbar-width: thin;
  scrollbar-color: var(--color-border) var(--color-muted);
}
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-track {
  background: var(--color-muted);
}
.chat-messages::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 9999px;
}
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
}
.empty-icon-circle {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: var(--color-muted);
  border-radius: 9999px;
  border: 2px solid var(--color-border);
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
  border: 2px solid var(--color-fg);
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}
.msg-sent {
  background: var(--color-accent);
  color: #fff;
  border-radius: var(--radius-lg) var(--radius-lg) var(--radius-sm) var(--radius-lg);
  box-shadow: 3px 3px 0 var(--color-fg);
}
.msg-received {
  background: #fff;
  border-radius: var(--radius-lg) var(--radius-lg) var(--radius-lg) var(--radius-sm);
  box-shadow: 3px 3px 0 var(--color-border);
}

.msg-time {
  font-size: 11px;
  color: var(--color-muted-fg);
  margin-top: 4px;
  padding: 0 4px;
}

/* Input bar */
.chat-input-bar {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 253, 245, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 2px solid var(--color-border);
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
