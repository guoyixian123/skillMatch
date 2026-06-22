<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><Bell /></el-icon> 通知</h1>
      <p class="page-subtitle">查看你的所有通知</p>
    </header>

    <!-- 通知类型切换 -->
    <div class="notif-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="notif-tab"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        <div class="tab-icon" :class="tab.color">
          <el-icon><component :is="tab.icon" /></el-icon>
        </div>
        <div class="tab-text">
          <span class="tab-label">{{ tab.label }}</span>
          <span class="tab-desc">{{ tab.desc }}</span>
        </div>
        <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
      </button>
    </div>

    <!-- 状态筛选（仅交换请求显示） -->
    <div v-if="activeTab === 'received' || activeTab === 'sent'" class="status-filter">
      <button
        v-for="s in statuses"
        :key="s.value"
        class="status-chip"
        :class="{ active: status === s.value, ['color-' + s.value]: true }"
        @click="status = s.value; fetchData()"
      >
        <span class="chip-dot"></span>
        {{ s.label }}
      </button>
    </div>

    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <!-- 收到的交换请求 -->
    <template v-else-if="activeTab === 'received'">
      <div v-if="requests.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><FolderOpened /></el-icon></div>
        <div style="font-weight:700;font-size:18px;">暂无收到的请求</div>
        <div style="color:var(--color-muted-fg);margin-top:4px;">有人发送技能交换请求时会出现在这里</div>
      </div>

      <div v-else class="request-list">
        <div v-for="req in requests" :key="req.id" class="request-card geo-card">
          <div class="request-header">
            <img
              :src="req.fromUser?.avatarUrl || getDefaultAvatar(req.fromUser?.id || req.fromUser?.name)"
              class="geo-avatar"
            />
            <div class="request-info">
              <div class="request-user">{{ req.fromUser?.name }}</div>
              <div class="request-reason">"{{ req.reason }}"</div>
              <div class="request-time">{{ formatTime(req.createdAt) }}</div>
            </div>
            <div class="request-status-tag" :class="'status-' + req.status">
              {{ statusMap[req.status] || req.status }}
            </div>
          </div>
          <div class="request-actions" v-if="req.status === 1">
            <button class="geo-btn primary small" @click="handleAccept(req)">
              <el-icon><Check /></el-icon> 同意
            </button>
            <button class="geo-btn danger small" @click="handleDecline(req)">
              <el-icon><Close /></el-icon> 拒绝
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- 已发送的请求 -->
    <template v-else-if="activeTab === 'sent'">
      <div v-if="requests.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><Promotion /></el-icon></div>
        <div style="font-weight:700;font-size:18px;">暂无已发送的请求</div>
        <div style="color:var(--color-muted-fg);margin-top:4px;">你发送的交换请求会出现在这里</div>
      </div>

      <div v-else class="request-list">
        <div v-for="req in requests" :key="req.id" class="request-card geo-card">
          <div class="request-header">
            <img
              :src="req.toUser?.avatarUrl || getDefaultAvatar(req.toUser?.id || req.toUser?.name)"
              class="geo-avatar"
            />
            <div class="request-info">
              <div class="request-user">{{ req.toUser?.name || '用户' }}</div>
              <div class="request-reason">"{{ req.reason }}"</div>
              <div class="request-time">{{ formatTime(req.createdAt) }}</div>
            </div>
            <div class="request-status-tag" :class="'status-' + req.status">
              {{ statusMap[req.status] || req.status }}
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 互动通知（点赞/评论） -->
    <template v-else-if="activeTab === 'likes'">
      <div v-if="likeItems.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><Star /></el-icon></div>
        <div style="font-weight:700;font-size:18px;">暂无互动通知</div>
        <div style="color:var(--color-muted-fg);margin-top:4px;">有人点赞或评论你的帖子时会出现在这里</div>
      </div>

      <div v-else class="request-list">
        <div
          v-for="item in likeItems"
          :key="item.id"
          class="request-card geo-card like-notify-card"
          :class="{ unread: !item.isRead }"
          @click="handleLikeClick(item)"
        >
          <div class="request-header">
            <img
              :src="item.actor?.avatarUrl || getDefaultAvatar(item.actor?.id || item.actor?.name)"
              class="geo-avatar"
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
          <button class="geo-btn outline small" @click="handleMarkAllRead">全部已读</button>
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
import {
  Bell, Loading, FolderOpened, Star, Check, Close, Promotion,
  ChatDotRound, Position, Connection,
} from '@element-plus/icons-vue'
import {
  getReceivedRequests,
  getSentRequests,
  acceptRequest,
  declineRequest,
  getLikeNotifications,
  markLikeRead,
  markLikeAllRead,
  getUnreadCount,
  getLikeUnreadCount,
} from '@/api/notification'

const router = useRouter()
const activeTab = ref('received')
const status = ref(1)
const loading = ref(false)
const requests = ref([])
const likeItems = ref([])

const tabs = ref([
  { key: 'received', label: '收到的请求', desc: '等待你处理的交换请求', icon: 'ChatDotRound', color: 'violet', badge: 0 },
  { key: 'sent', label: '已发送', desc: '你发起的交换请求', icon: 'Position', color: 'green', badge: 0 },
  { key: 'likes', label: '互动消息', desc: '点赞和评论通知', icon: 'Star', color: 'yellow', badge: 0 },
])

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

function switchTab(key) {
  activeTab.value = key
  status.value = 1
  fetchData()
}

async function fetchBadges() {
  try {
    const [contactRes, likeRes] = await Promise.all([
      getUnreadCount(),
      getLikeUnreadCount(),
    ])
    const contactCount = (typeof contactRes.data === 'number') ? contactRes.data : (contactRes.data?.pendingRequestCount || 0)
    const likeCount = (typeof likeRes.data === 'number') ? likeRes.data : 0
    tabs.value[0].badge = contactCount || 0
    tabs.value[2].badge = likeCount || 0
  } catch { /* ignore */ }
}

async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'likes') {
      const res = await getLikeNotifications({ page: 1, size: 50 })
      likeItems.value = res.data?.list || []
    } else {
      const api = activeTab.value === 'received' ? getReceivedRequests : getSentRequests
      const res = await api({ status: status.value })
      requests.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    }
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleLikeClick(item) {
  if (!item.isRead) {
    try { await markLikeRead(item.id) } catch { /* ignore */ }
    item.isRead = true
    fetchBadges()
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
    fetchBadges()
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
    ElMessage.success(res.message || '已同意交换请求')
    if (res.data) {
      ElMessage.info('对方联系方式：' + res.data)
    }
    fetchData()
    fetchBadges()
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
    fetchBadges()
  } catch { /* cancel or error */ }
}

onMounted(() => {
  fetchData()
  fetchBadges()
})
</script>

<style scoped>
/* ===== 通知类型切换 ===== */
.notif-tabs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}
.notif-tab {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s var(--ease-bounce);
  text-align: left;
  font-family: inherit;
}
.notif-tab:hover {
  border-color: var(--color-accent);
  transform: translateX(4px);
}
.notif-tab.active {
  border-color: var(--color-accent);
  background: #F5F3FF;
  box-shadow: 4px 4px 0 rgba(139, 92, 246, 0.12);
}
.tab-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  font-size: 18px;
}
.tab-icon.violet { background: #EDE9FE; color: #7C3AED; }
.tab-icon.green { background: #D1FAE5; color: #059669; }
.tab-icon.yellow { background: #FEF3C7; color: #D97706; }
.tab-text {
  flex: 1;
  min-width: 0;
}
.tab-label {
  display: block;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
  color: var(--color-fg);
}
.tab-desc {
  display: block;
  font-size: 12px;
  color: var(--color-muted-fg);
  margin-top: 2px;
}
.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.tab-badge:empty,
.tab-badge[data-zero="true"] {
  display: none;
}

/* ===== 状态筛选 ===== */
.status-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-muted-fg);
  cursor: pointer;
  transition: all 0.3s var(--ease-bounce);
}
.status-chip:hover {
  border-color: var(--color-fg);
}
.status-chip.active {
  border-color: var(--color-fg);
  color: var(--color-fg);
}
.status-chip .chip-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-border);
  transition: background 0.2s;
}
.status-chip.active.color-1 .chip-dot { background: var(--color-tertiary); }
.status-chip.active.color-2 .chip-dot { background: var(--color-quaternary); }
.status-chip.active.color-3 .chip-dot { background: var(--color-muted-fg); }

/* ===== 请求卡片 ===== */
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
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
  margin-bottom: 4px;
  color: var(--color-fg);
}
.request-reason {
  font-size: 14px;
  color: var(--color-muted-fg);
  font-style: italic;
  margin-bottom: 4px;
}
.request-time {
  font-size: 12px;
  color: var(--color-muted-fg);
}
.request-status-tag {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  border: 2px solid var(--color-fg);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}
.request-status-tag.status-1 { background: var(--color-tertiary); color: var(--color-fg); }
.request-status-tag.status-2 { background: var(--color-quaternary); color: #065F46; }
.request-status-tag.status-3 { background: var(--color-muted); color: var(--color-muted-fg); }
.request-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid var(--color-border);
}

/* ===== 互动通知 ===== */
.like-notify-card {
  cursor: pointer;
  transition: background 0.3s, transform 0.3s var(--ease-bounce);
}
.like-notify-card:hover {
  background: #F5F3FF;
  transform: translateX(4px);
}
.like-notify-card.unread {
  border-left: 4px solid var(--color-accent);
}
.like-preview {
  font-weight: 400;
  font-size: 14px;
  color: var(--color-muted-fg);
  margin-left: 6px;
}
.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-accent);
  flex-shrink: 0;
  align-self: center;
}
</style>
