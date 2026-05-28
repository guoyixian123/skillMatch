<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><ChatLineRound /></el-icon> 好友</h1>
      <p class="page-subtitle">聊天</p>
    </header>

    <div class="search-bar" style="margin-bottom:20px;">
      <input
        v-model="keyword"
        class="brutal-input"
        placeholder="搜索好友..."
        style="font-size:14px;padding:10px 14px;"
      />
    </div>

    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <template v-else>
      <div v-if="filteredFriends.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><ChatLineRound /></el-icon></div>
        <div style="font-weight:800;font-size:18px;">{{ keyword ? '没有找到好友' : '暂无好友' }}</div>
        <div style="color:#888;margin-top:8px;">发送技能交换请求，对方同意后成为好友</div>
      </div>

      <div v-else class="friend-list">
        <div
          v-for="f in filteredFriends"
          :key="f.userId"
          class="friend-card brutal-card"
          @click="$router.push(`/chat/${f.userId}`)"
        >
          <div class="friend-row">
            <el-badge :value="f.unreadCount" :hidden="!f.unreadCount" class="friend-avatar-badge">
              <img
                :src="f.avatarUrl || getDefaultAvatar(f.userId || f.name)"
                class="brutal-avatar avatar-clickable"
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
import { ref, computed, onMounted } from 'vue'
import { getDefaultAvatar } from '@/utils/avatar'
import { ChatLineRound, Loading } from '@element-plus/icons-vue'
import { getFriends } from '@/api/friend'

const loading = ref(true)
const friends = ref([])
const keyword = ref('')

const filteredFriends = computed(() => {
  if (!keyword.value.trim()) return friends.value
  const kw = keyword.value.trim().toLowerCase()
  return friends.value.filter(f => f.name?.toLowerCase().includes(kw))
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

onMounted(async () => {
  try {
    const res = await getFriends()
    friends.value = Array.isArray(res.data) ? res.data : []
  } catch { /* handled */ } finally {
    loading.value = false
  }
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
  transition: background 0.15s;
}
.friend-card:hover {
  background: #FFFDE7;
}
.friend-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.friend-avatar-badge {
  flex-shrink: 0;
}
.friend-info {
  flex: 1;
  min-width: 0;
}
.friend-name {
  font-weight: 900;
  font-size: 15px;
  margin-bottom: 4px;
}
.friend-last-msg {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.friend-time {
  font-size: 12px;
  color: #aaa;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}
.avatar-clickable {
  cursor: pointer;
  transition: transform 0.15s;
}
.avatar-clickable:hover {
  transform: scale(1.08);
}
</style>
