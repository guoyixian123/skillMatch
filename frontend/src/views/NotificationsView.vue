<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title">🔔 通知</h1>
      <p class="page-subtitle">技能交换请求</p>
    </header>

    <el-tabs v-model="activeTab" @tab-change="fetchData">
      <el-tab-pane label="收到的请求" name="received" />
      <el-tab-pane label="已发送的请求" name="sent" />
    </el-tabs>

    <div class="filter-row" style="margin-bottom:16px;display:flex;gap:8px;">
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

    <div v-if="loading" class="loading-block">⚡ 加载中...</div>

    <div v-else-if="requests.length === 0" class="empty-block">
      <div class="icon">📭</div>
      <div style="font-weight:800;font-size:18px;">暂无请求</div>
    </div>

    <div v-else class="request-list">
      <div v-for="req in requests" :key="req.id" class="request-card brutal-card">
        <div class="request-header">
          <img
            :src="(activeTab === 'received' ? req.fromUser : req.toUser)?.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
            class="brutal-avatar"
          />
          <div class="request-info">
            <div class="request-user">
              {{ activeTab === 'received' ? req.fromUser?.nickname : req.toUser?.nickname }}
            </div>
            <div class="request-reason">"{{ req.reason }}"</div>
            <div class="request-time">{{ formatTime(req.createdAt) }}</div>
          </div>
          <div class="request-status-tag" :class="req.status">
            {{ statusMap[req.status] || req.status }}
          </div>
        </div>

        <div class="request-actions" v-if="activeTab === 'received' && req.status === 'pending'">
          <button class="brutal-btn primary small" @click="handleAccept(req)">
            同意交换
          </button>
          <button class="brutal-btn danger small" @click="handleDecline(req)">
            拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getReceivedRequests,
  getSentRequests,
  acceptRequest,
  declineRequest,
} from '@/api/notification'

const activeTab = ref('received')
const status = ref('pending')
const loading = ref(false)
const requests = ref([])

const statuses = [
  { label: '待处理', value: 'pending' },
  { label: '已同意', value: 'accepted' },
  { label: '已拒绝', value: 'declined' },
]
const statusMap = { pending: '待处理', accepted: '已同意', declined: '已拒绝' }

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
    const api = activeTab.value === 'received' ? getReceivedRequests : getSentRequests
    const res = await api({ status: status.value })
    requests.value = res.data?.list || []
  } catch { /* API may not be implemented yet */ } finally {
    loading.value = false
  }
}

async function handleAccept(req) {
  await ElMessageBox.confirm(
    `同意后双方可查看联系方式，确定同意 ${req.fromUser?.nickname} 的交换请求吗？`,
    '确认',
    { confirmButtonText: '同意', cancelButtonText: '取消', type: 'info' }
  )
  try {
    const res = await acceptRequest(req.id)
    ElMessage.success('已同意交换请求')
    if (res.data?.contactInfo) {
      ElMessage.info('对方联系方式：' + res.data.contactInfo)
    }
    fetchData()
  } catch { /* handled */ }
}

async function handleDecline(req) {
  await ElMessageBox.confirm('确定拒绝该请求吗？', '确认', {
    confirmButtonText: '拒绝',
    cancelButtonText: '取消',
    type: 'warning',
  })
  try {
    await declineRequest(req.id)
    ElMessage.success('已拒绝')
    fetchData()
  } catch { /* handled */ }
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
.request-status-tag.pending { background: var(--color-yellow); }
.request-status-tag.accepted { background: var(--color-green); color: #000; }
.request-status-tag.declined { background: #eee; color: #888; }
.request-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #eee;
}
</style>
