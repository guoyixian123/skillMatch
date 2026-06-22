<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><ChatLineRound /></el-icon> 好友</h1>
      <p class="page-subtitle">聊天</p>
    </header>

    <div class="search-bar" style="margin-bottom:20px;">
      <input
        v-model="keyword"
        class="geo-input"
        placeholder="搜索好友..."
        style="font-size:14px;padding:10px 14px;"
      />
    </div>

    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <template v-else>
      <div v-if="filteredFriends.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><ChatLineRound /></el-icon></div>
        <div style="font-weight:700;font-size:18px;">{{ keyword ? '没有找到好友' : '暂无好友' }}</div>
        <div style="color:var(--color-muted-fg);margin-top:8px;">发送技能交换请求，对方同意后成为好友</div>
      </div>

      <div v-else class="friend-list">
        <div
          v-for="f in filteredFriends"
          :key="f.userId"
          class="friend-card geo-card"
          @click="$router.push(`/chat/${f.userId}`)"
        >
          <div class="friend-row">
            <el-badge :value="f.unreadCount" :hidden="!f.unreadCount" class="friend-avatar-badge">
              <img
                :src="f.avatarUrl || getDefaultAvatar(f.userId || f.name)"
                class="geo-avatar avatar-clickable"
                @click.stop="$router.push(`/user/${f.userId}`)"
              />
            </el-badge>
            <div class="friend-info">
              <div class="friend-name">{{ f.name }}</div>
              <div class="friend-last-msg">{{ f.lastMessage || '开始聊天...' }}</div>
            </div>
            <div class="friend-time">{{ formatTime(f.lastMessageTime) }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getDefaultAvatar } from '@/utils/avatar'
import { ChatLineRound, Loading } from '@element-plus/icons-vue'
import { getFriends } from '@/api/friend'
import { useChatStore } from '@/stores/chat'
import ws from '@/utils/ws'

const chatStore = useChatStore()
const loading = ref(true)
const friends = ref([])
const keyword = ref('')
let unsubFriendUpdate = null
let unsubChat = null

const filteredFriends = computed(() => {
  let list = friends.value
  // 按最新消息时间排序，有新消息的好友排前面
  list = [...list].sort((a, b) => {
    const ta = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0
    const tb = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0
    return tb - ta
  })
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(f => f.name?.toLowerCase().includes(kw))
  }
  return list
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

async function fetchFriends() {
  try {
    const res = await getFriends()
    friends.value = Array.isArray(res.data) ? res.data : []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

/** 收到聊天消息时，实时更新好友列表中的最后消息 */
function handleChatMessage(msg) {
  const fromUserId = msg.fromUserId
  if (!fromUserId) return

  // 找到对应好友，更新最后消息
  const friend = friends.value.find(f => f.userId === fromUserId)
  if (friend) {
    friend.lastMessage = msg.content
    friend.lastMessageTime = msg.createdAt
    // 如果不在当前聊天页，未读数 +1
    if (chatStore.currentFriendId !== fromUserId) {
      friend.unreadCount = (friend.unreadCount || 0) + 1
    }
  } else {
    // 可能是新好友，刷新列表
    fetchFriends()
  }
}

onMounted(() => {
  fetchFriends()

  // 监听好友关系变更（新增好友）
  unsubFriendUpdate = ws.on('friend_update', () => {
    console.log('[Friends] 收到好友列表更新，刷新中...')
    fetchFriends()
  })

  // 监听聊天消息，实时更新最后消息
  unsubChat = ws.on('chat', handleChatMessage)
})

onUnmounted(() => {
  unsubFriendUpdate?.()
  unsubChat?.()
})
</script>

<style scoped>
.friend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.friend-card {
  padding: 16px;
  cursor: pointer;
  transition: transform 0.3s var(--ease-bounce), box-shadow 0.3s var(--ease-bounce), background 0.3s;
}
.friend-card:hover {
  background: #F5F3FF;
  transform: translateX(4px);
}
.friend-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.friend-avatar-badge { flex-shrink: 0; }
.friend-info {
  flex: 1;
  min-width: 0;
}
.friend-name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 4px;
  color: var(--color-fg);
}
.friend-last-msg {
  font-size: 13px;
  color: var(--color-muted-fg);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.friend-time {
  font-size: 12px;
  color: var(--color-muted-fg);
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}
.avatar-clickable {
  cursor: pointer;
  transition: transform 0.3s var(--ease-bounce);
}
.avatar-clickable:hover {
  transform: scale(1.08);
}
</style>
