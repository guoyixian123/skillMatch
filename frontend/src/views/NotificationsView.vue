<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><Bell /></el-icon> 通知</h1>
      <p class="page-subtitle">技能交换请求</p>
    </header>

    <el-tabs v-model="activeTab" @tab-change="fetchData">
      <el-tab-pane label="技能交换" name="received" />
      <el-tab-pane label="已发送" name="sent" />
      <el-tab-pane label="互动" name="likes" />
    </el-tabs>

    <!-- Filter for exchange request tabs -->
    <div v-if="activeTab !== 'likes'" class="filter-row" style="margin-bottom:16px;display:flex;gap:8px;">
      <button
        v-for="s in statuses"
        :key="s.value"
        class="brutal-btn small"
        :class="status === s.value ? 'dark' : 'outline'"
        @click="status = s.value; fetchData()"
      >
        {{ s.label }}
      </button>
    </div>

    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <!-- Exchange Request Cards -->
    <template v-else-if="activeTab !== 'likes'">
      <div v-if="requests.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><FolderOpened /></el-icon></div>
        <div style="font-weight:800;font-size:18px;">暂无请求</div>
      </div>

      <div v-else class="request-list">
        <div v-for="req in requests" :key="req.id" class="request-card brutal-card">
          <div class="request-header">
            <img
              :src="req.fromUser?.avatarUrl || getDefaultAvatar(req.fromUser?.id || req.fromUser?.name)"
              class="brutal-avatar"
            />
            <div class="request-info">
              <div class="request-user">
                {{ req.fromUser?.name }}
              </div>
              <div class="request-reason">"{{ req.reason }}"</div>
              <div class="request-time">{{ formatTime(req.createdAt) }}</div>
            </div>
            <div class="request-status-tag" :class="'status-' + req.status">
              {{ statusMap[req.status] || req.status }}
            </div>
          </div>

          <div class="request-actions" v-if="activeTab === 'received' && req.status === 1">
            <button class="brutal-btn primary small" @click="handleAccept(req)">
              同意交换
            </button>
            <button class="brutal-btn danger small" @click="handleDecline(req)">
              拒绝
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Like Notification Cards -->
    <template v-else>
      <div v-if="likeItems.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><Star /></el-icon></div>
        <div style="font-weight:800;font-size:18px;">暂无点赞通知</div>
      </div>

      <div v-else class="request-list">
        <div
          v-for="item in likeItems"
          :key="item.id"
          class="request-card brutal-card like-notify-card"
          :class="{ unread: !item.isRead }"
          @click="handleLikeClick(item)"
        >
          <div class="request-header">
            <img
              :src="item.actor?.avatarUrl || getDefaultAvatar(item.actor?.id || item.actor?.name)"
              class="brutal-avatar"
            />
            <div class="request-info">
              <div class="request-user">
                <strong>{{ item.actor?.name }}</strong>
                <span class="like-preview">{{ item.preview }}</span>
              </div>
              <div class="request-time">{{ formatTime(item.createdAt) }}</div>
            </div>
            <span v-if="!item.isRead" class="unread-dot" />
          </div>
        </div>

        <div v-if="likeItems.length > 0" style="text-align:right;margin-top:12px;">
          <button class="brutal-btn outline small" @click="handleMarkAllRead">全部已读</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/avatar'
import { ElMessageBox } from 'element-plus'
import { ElMessage } from '@/utils/message'
import { Bell, Loading, FolderOpened, Star } from '@element-plus/icons-vue'
import {
  getReceivedRequests,
  getSentRequests,
  acceptRequest,
  declineRequest,
  getLikeNotifications,
  markLikeRead,
  markLikeAllRead,
} from '@/api/notification'

const router = useRouter()
const activeTab = ref('received')
const status = ref(1)
const loading = ref(false)
const requests = ref([])
const likeItems = ref([])
const likePage = ref(1)
const likeTotal = ref(0)

const statuses = [
  { label: '待处理', value: 1 },
  { label: '已同意', value: 2 },
  { label: '已拒绝', value: 3 },
]
const statusMap = { 1: '待处理', 2: '已同意', 3: '已拒绝' }

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'likes') {
      await fetchLikes()
    } else {
      const api = activeTab.value === 'received' ? getReceivedRequests : getSentRequests
      const res = await api({ status: status.value })
      requests.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    }
  } catch { /* API may not be implemented yet */ } finally {
    loading.value = false
  }
}

async function fetchLikes() {
  try {
    const res = await getLikeNotifications({ page: likePage.value, size: 20 })
    likeItems.value = res.data?.list || []
    likeTotal.value = res.data?.total || 0
  } catch { /* handled */ }
}

async function handleLikeClick(item) {
  if (!item.isRead) {
    try { await markLikeRead(item.id) } catch { /* ignore */ }
    item.isRead = true
  }
  if ((item.type === 2 || item.type === 3) && item.bizId) {
    router.push(`/community/${item.bizId}`)
  } else {
    router.push(`/user/${item.actor?.id}`)
  }
}

async function handleMarkAllRead() {
  try {
    const res = await markLikeAllRead()
    likeItems.value.forEach(i => i.isRead = true)
    ElMessage.success(res.message || '全部已读')
  } catch { /* ignore */ }
}

async function handleAccept(req) {
  try {
    await ElMessageBox.confirm(
      `同意后你们将成为好友，确定同意 ${req.fromUser?.name} 的交换请求吗？`,
      '确认',
      { confirmButtonText: '同意', cancelButtonText: '取消', type: 'info', showClose: false }
    )
    const res = await acceptRequest(req.id)
    ElMessage.success(res.message || '已同意交换请求，你们已成为好友')
    if (res.data) {
      ElMessage.info('对方联系方式：' + res.data)
    }
    fetchData()
  } catch { /* cancel or error */ }
}

async function handleDecline(req) {
  try {
    await ElMessageBox.confirm('确定拒绝该请求吗？', '确认', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: false,
    })
    const res = await declineRequest(req.id)
    ElMessage.success(res.message || '已拒绝')
    fetchData()
  } catch { /* cancel or error */ }
}

onMounted(fetchData)
</script>

<style scoped>
.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.request-card { padding: 16px; }
.request-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.request-info { flex: 1; }
.request-user {
  font-weight: 900;
  font-size: 15px;
  margin-bottom: 4px;
}
.request-reason {
  font-size: 14px;
  color: #555;
  font-style: italic;
  margin-bottom: 4px;
}
.request-time {
  font-size: 12px;
  color: #aaa;
}
.request-status-tag {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  border: 2px solid #1A1A1A;
  flex-shrink: 0;
}
.request-status-tag.status-1 { background: var(--color-yellow); }
.request-status-tag.status-2 { background: var(--color-green); color: #000; }
.request-status-tag.status-3 { background: #eee; color: #888; }
.request-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #eee;
}

/* Like notification */
.like-notify-card {
  cursor: pointer;
  transition: background 0.15s;
}
.like-notify-card:hover { background: #FFFDE7; }
.like-notify-card.unread {
  border-left: 4px solid var(--color-yellow);
}
.like-preview {
  font-weight: 400;
  font-size: 14px;
  color: #555;
  margin-left: 6px;
}
.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-yellow);
  flex-shrink: 0;
  align-self: center;
}
</style>
