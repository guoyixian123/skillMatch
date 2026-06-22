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
            <div class="friend-right">
              <div class="friend-time">{{ formatTime(f.lastMessageTime) }}</div>
              <el-dropdown trigger="click" popper-class="friend-action-menu" @command="(cmd) => handleCommand(cmd, f)" @click.stop>
                <button class="friend-more-btn" @click.stop>
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><UserFilled /></el-icon> 查看主页
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon> 删除好友
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 删除好友确认弹窗 -->
    <el-dialog v-model="showDeleteDialog" width="380px" :show-close="false" destroy-on-close>
      <div class="delete-dialog">
        <div class="delete-icon-wrap">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:40px;height:40px;">
            <polygon points="28,3 10,26 22,26 18,45 38,22 26,22 30,3" fill="#FBBF24" stroke="#1E293B" stroke-width="2.5" stroke-linejoin="round"/>
            <circle cx="8" cy="8" r="3" fill="#F472B6" stroke="#1E293B" stroke-width="1.2"/>
            <circle cx="40" cy="8" r="3" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.2"/>
            <circle cx="8" cy="40" r="3" fill="#34D399" stroke="#1E293B" stroke-width="1.2"/>
            <circle cx="40" cy="40" r="3" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.2"/>
          </svg>
        </div>
        <h3 class="delete-title">删除好友</h3>
        <p class="delete-desc">你即将删除好友 <strong>{{ deleteTarget?.name }}</strong></p>
        <div class="delete-warn">
          <el-icon><WarningFilled /></el-icon>
          <span>删除后聊天记录将无法恢复</span>
        </div>
        <div class="delete-actions">
          <button class="geo-btn outline" @click="showDeleteDialog = false">取消</button>
          <button class="geo-btn danger" @click="confirmDelete" :disabled="deleting">
            {{ deleting ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/avatar'
import { ElMessage } from 'element-plus'
import { ChatLineRound, Loading, MoreFilled, UserFilled, Delete, WarningFilled } from '@element-plus/icons-vue'
import { getFriends, removeFriend } from '@/api/friend'
import { useChatStore } from '@/stores/chat'
import ws from '@/utils/ws'

const router = useRouter()
const chatStore = useChatStore()
const loading = ref(true)
const friends = ref([])
const keyword = ref('')
const showDeleteDialog = ref(false)
const deleteTarget = ref(null)
const deleting = ref(false)
let unsubFriendUpdate = null
let unsubChat = null

const filteredFriends = computed(() => {
  let list = friends.value
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

function handleCommand(cmd, friend) {
  if (cmd === 'profile') {
    router.push(`/user/${friend.userId}`)
  } else if (cmd === 'delete') {
    handleDelete(friend)
  }
}

async function handleDelete(friend) {
  deleteTarget.value = friend
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deleting.value = true
  try {
    const res = await removeFriend(deleteTarget.value.userId)
    ElMessage.success(res.message || '已删除好友')
    friends.value = friends.value.filter(f => f.userId !== deleteTarget.value.userId)
    chatStore.fetchUnreadCount()
    showDeleteDialog.value = false
    deleteTarget.value = null
  } catch { /* handled */ } finally {
    deleting.value = false
  }
}

function handleChatMessage(msg) {
  const fromUserId = msg.fromUserId
  if (!fromUserId) return
  const friend = friends.value.find(f => f.userId === fromUserId)
  if (friend) {
    friend.lastMessage = msg.content
    friend.lastMessageTime = msg.createdAt
    if (chatStore.currentFriendId !== fromUserId) {
      friend.unreadCount = (friend.unreadCount || 0) + 1
    }
  } else {
    fetchFriends()
  }
}

onMounted(() => {
  fetchFriends()
  unsubFriendUpdate = ws.on('friend_update', () => fetchFriends())
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
.friend-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}
.friend-time {
  font-size: 12px;
  color: var(--color-muted-fg);
}
.friend-more-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  cursor: pointer;
  color: var(--color-muted-fg);
  transition: all 0.2s;
  opacity: 0;
}
.friend-card:hover .friend-more-btn {
  opacity: 1;
}
.friend-more-btn:hover {
  background: var(--color-muted);
  color: var(--color-fg);
}
.avatar-clickable {
  cursor: pointer;
  transition: transform 0.3s var(--ease-bounce);
}
.avatar-clickable:hover {
  transform: scale(1.08);
}

/* 删除弹窗 */
.delete-dialog {
  text-align: center;
  padding: 8px 0;
}
.delete-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: #FEE2E2;
  border-radius: var(--radius-full);
  border: 2px solid #FCA5A5;
  margin-bottom: 16px;
}
.delete-title {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 800;
  color: var(--color-fg);
  margin-bottom: 8px;
}
.delete-desc {
  font-size: 14px;
  color: var(--color-muted-fg);
  margin-bottom: 16px;
  line-height: 1.6;
}
.delete-desc strong {
  color: var(--color-fg);
  font-weight: 700;
}
.delete-warn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #FEF3C7;
  border: 2px solid #FCD34D;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: #92400E;
  margin-bottom: 20px;
}
.delete-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.delete-actions .geo-btn {
  min-width: 100px;
  justify-content: center;
}
</style>

<!-- 下拉菜单样式 -->
<style>
.friend-action-menu {
  border-radius: var(--radius-lg) !important;
  border: 2px solid var(--color-fg) !important;
  box-shadow: 4px 4px 0 var(--color-fg) !important;
  padding: 8px !important;
  min-width: 140px !important;
}
.friend-action-menu .el-dropdown-menu__item {
  border-radius: var(--radius-md) !important;
  padding: 8px 12px !important;
  font-family: var(--font-body) !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  transition: all 0.2s var(--ease-bounce) !important;
}
.friend-action-menu .el-dropdown-menu__item .el-icon {
  width: 24px !important;
  height: 24px !important;
  border-radius: 9999px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 12px !important;
}
.friend-action-menu .el-dropdown-menu__item:nth-child(1) .el-icon {
  background: #EDE9FE !important;
  color: #7C3AED !important;
}
.friend-action-menu .el-dropdown-menu__item:nth-child(3) .el-icon {
  background: #FEE2E2 !important;
  color: #DC2626 !important;
}
.friend-action-menu .el-dropdown-menu__item:hover {
  background: var(--color-muted) !important;
  transform: translateX(2px) !important;
}
.friend-action-menu .el-dropdown-menu__item:nth-child(3):hover {
  background: #FEE2E2 !important;
  color: #DC2626 !important;
}
.friend-action-menu .el-dropdown-menu__item--divided {
  border-top: 2px solid var(--color-border) !important;
  margin-top: 4px !important;
  padding-top: 10px !important;
}
</style>
