<template>
  <div class="chat-page">
    <!-- Header -->
    <div class="chat-header">
      <button class="back-btn" @click="$router.push('/friends')">
        <i class="pi pi-arrow-left"></i>
      </button>
      <img :src="friendInfo.avatarUrl || getDefaultAvatar(friendId)" class="chat-header-avatar" />
      <div class="chat-header-info">
        <div class="chat-header-name">{{ friendInfo.name || '好友' }}</div>
        <div v-if="friendInfo.skills && friendInfo.skills.length" class="chat-header-tags">
          <span v-for="skill in friendInfo.skills.slice(0, 3)" :key="skill" class="skill-tag">{{ skill }}</span>
        </div>
      </div>
    </div>

    <!-- Messages -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="chatStore.loading" class="loading-block">
        <i class="pi pi-spinner pi-spin"></i> 加载中...
      </div>

      <template v-else>
        <div v-if="chatStore.messages.length === 0" class="chat-empty">
          <div style="font-size:40px;margin-bottom:8px;">💬</div>
          <div style="font-weight:700;color:#888;">开始和好友聊天吧</div>
        </div>

        <!-- 时间分组 -->
        <template v-for="(group, dateKey) in groupedMessages" :key="dateKey">
          <div class="date-divider"><span>{{ formatDateKey(dateKey) }}</span></div>
          <div
            v-for="msg in group"
            :key="msg.id"
            class="msg-row"
            :class="msg.fromUserId === currentUserId ? 'msg-row-sent' : 'msg-row-received'"
          >
            <!-- 头像 -->
            <img
              :src="msg.fromUserId === currentUserId ? currentUserAvatar : friendInfo.avatarUrl || getDefaultAvatar(friendId)"
              class="msg-avatar"
            />
            <!-- 消息内容 -->
            <div class="msg-content-wrapper">
              <div class="msg-bubble" :class="msg.fromUserId === currentUserId ? 'msg-sent' : 'msg-received'">
                <div class="msg-text">{{ msg.content }}</div>
              </div>
              <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </template>
      </template>
    </div>

    <!-- Input -->
    <div class="chat-input-bar">
      <button class="input-btn icon-btn">
        <i class="pi pi-plus"></i>
      </button>
      <input
        v-model="inputText"
        class="chat-input"
        placeholder="输入消息..."
        @keydown.enter.prevent="handleSend"
        :disabled="chatStore.sending"
      />
      <button class="input-btn icon-btn">
        <i class="pi pi-smile"></i>
      </button>
      <button class="send-btn" @click="handleSend" :disabled="!inputText.trim() || chatStore.sending">
        <i class="pi pi-send-fill"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { getFriends, getMessages } from '@/api/friend'
import { getProfile } from '@/api/user'

const route = useRoute()
const chatStore = useChatStore()
const authStore = useAuthStore()

const friendId = route.params.friendId
const currentUserId = authStore.user?.userId || authStore.user?.id
const friendInfo = ref({ name: '', avatarUrl: '', skills: [] })
const currentUserAvatar = ref('')
const inputText = ref('')
const messagesContainer = ref(null)

// 计算当前用户头像
const currentUserAvatarUrl = computed(() => {
  return currentUserAvatar.value || getDefaultAvatar(currentUserId)
})

// 消息按日期分组
const groupedMessages = computed(() => {
  const groups = {}
  const sortedMsgs = [...chatStore.messages].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))

  sortedMsgs.forEach(msg => {
    const dateKey = getDateKey(msg.createdAt)
    if (!groups[dateKey]) {
      groups[dateKey] = []
    }
    groups[dateKey].push(msg)
  })

  return groups
})

// 获取日期 key（用于分组）
function getDateKey(dateStr) {
  const d = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  const dDate = d.toDateString()
  if (dDate === today.toDateString()) return 'today'
  if (dDate === yesterday.toDateString()) return 'yesterday'

  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
}

// 格式化日期 key 显示
function formatDateKey(dateKey) {
  if (dateKey === 'today') return '今天'
  if (dateKey === 'yesterday') return '昨天'
  return dateKey
}

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

async function fetchFriendInfo() {
  try {
    // 获取好友列表中的基本信息
    const res = await getFriends()
    const list = Array.isArray(res.data) ? res.data : []
    const found = list.find(f => String(f.userId) === String(friendId))
    if (found) {
      friendInfo.value.name = found.name
      friendInfo.value.avatarUrl = found.avatarUrl
    }

    // 获取好友详细资料（包括技能）
    const profileRes = await getProfile(friendId)
    if (profileRes.data) {
      friendInfo.value.avatarUrl = profileRes.data.avatarUrl || friendInfo.value.avatarUrl
      // 合并技能标签（canSkills 和 wantSkills）
      const allSkills = [
        ...(profileRes.data.canSkills || []),
        ...(profileRes.data.wantSkills || [])
      ].filter((skill, index, self) => self.indexOf(skill) === index) // 去重
      friendInfo.value.skills = allSkills
    }
  } catch { /* ignore */ }
}

async function fetchCurrentUserAvatar() {
  try {
    const profileRes = await getProfile(currentUserId)
    if (profileRes.data) {
      currentUserAvatar.value = profileRes.data.avatarUrl
    }
  } catch { /* ignore */ }
}

watch(() => chatStore.messages.length, () => { scrollToBottom() })

onMounted(async () => {
  await Promise.all([
    fetchFriendInfo(),
    fetchCurrentUserAvatar(),
    chatStore.fetchMessages(friendId)
  ])
  scrollToBottom()
  chatStore.startPolling(friendId)
})

onUnmounted(() => { chatStore.stopPolling(); chatStore.clear() })
</script>

<style scoped>
.chat-page {
  display: flex; flex-direction: column;
  height: calc(100vh - 60px); background: var(--bg-primary);
}
@media (max-width: 768px) {
  .chat-page { height: calc(100dvh - 60px - 56px); }
}

/* 顶部导航 */
.chat-header {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: #fff;
  border-bottom: 1px solid #e5e7eb; flex-shrink: 0;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  background: #f3f4f6; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: background 0.2s;
}
.back-btn:hover { background: #e5e7eb; }
.chat-header-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  object-fit: cover; flex-shrink: 0;
}
.chat-header-info {
  flex: 1; min-width: 0;
}
.chat-header-name {
  font-weight: 600; font-size: 16px; color: #1f2937;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.chat-header-tags {
  display: flex; gap: 4px; margin-top: 2px; flex-wrap: wrap;
}
.skill-tag {
  font-size: 11px; padding: 2px 6px; background: #e0f2fe;
  color: #0369a1; border-radius: 4px; font-weight: 500;
}

/* 消息区域 */
.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px;
  background: #f9fafb;
}
.chat-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; flex: 1; }

.date-divider {
  text-align: center; margin: 12px 0;
}
.date-divider::before,
.date-divider::after {
  content: ''; display: inline-block; width: 30%; height: 1px;
  background: #e5e7eb; vertical-align: middle; margin: 0 8px;
}
.date-divider span {
  font-size: 12px; color: #9ca3af; padding: 0 8px;
}

/* 消息行 */
.msg-row {
  display: flex; gap: 8px; margin-bottom: 16px;
  max-width: 85%;
}
.msg-row-sent { flex-direction: row-reverse; margin-left: auto; }
.msg-row-received { margin-right: auto; }

.msg-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  object-fit: cover; flex-shrink: 0;
}

.msg-content-wrapper {
  display: flex; flex-direction: column; max-width: 100%;
}
.msg-row-sent .msg-content-wrapper { align-items: flex-end; }
.msg-row-received .msg-content-wrapper { align-items: flex-start; }

.msg-bubble {
  padding: 10px 14px; border-radius: 12px;
  font-size: 14px; line-height: 1.5; word-break: break-word;
  max-width: 280px;
}
.msg-sent {
  background: #3b82f6; color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-received {
  background: #fff; color: #1f2937;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
}
.msg-text { white-space: pre-wrap; }

.msg-time {
  font-size: 11px; color: #9ca3af;
  margin-top: 4px; padding: 0 4px;
}

/* 输入区域 */
.chat-input-bar {
  display: flex; gap: 8px; padding: 12px 16px;
  background: #fff; border-top: 1px solid #e5e7eb; flex-shrink: 0;
}
.input-btn {
  width: 36px; height: 36px; border-radius: 50%;
  background: #f3f4f6; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: background 0.2s;
  color: #6b7280;
}
.input-btn:hover { background: #e5e7eb; color: #4b5563; }
.input-btn i { font-size: 18px; }

.chat-input {
  flex: 1; padding: 10px 14px;
  border: 1px solid #e5e7eb; border-radius: 20px;
  font-size: 14px; outline: none;
  background: #f9fafb;
}
.chat-input:focus { border-color: #3b82f6; background: #fff; }
.chat-input:disabled { opacity: 0.6; cursor: not-allowed; }

.send-btn {
  width: 36px; height: 36px; border-radius: 50%;
  background: #3b82f6; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
  color: #fff;
}
.send-btn:hover:not(:disabled) { background: #2563eb; transform: scale(1.05); }
.send-btn:disabled { background: #d1d5db; cursor: not-allowed; }
.send-btn i { font-size: 16px; margin-left: 2px; }

.loading-block {
  display: flex; align-items: center; justify-content: center;
  gap: 8px; padding: 40px; color: #9ca3af;
}
</style>
