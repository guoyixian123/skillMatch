<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><Search /></el-icon> 发现技能搭子</h1>
      <p class="page-subtitle">找到与你技能互补的人</p>
    </header>

    <!-- Filters -->
    <div class="filter-bar geo-card accent-yellow">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索昵称、技能..."
          :prefix-icon="Search"
          clearable
          class="filter-search"
          @keyup.enter="fetchUsers"
        />
        <el-select v-model="sort" class="filter-select">
          <el-option label="匹配度" value="score" />
          <el-option label="距离近" value="dist" />
          <el-option label="活跃度" value="active" />
        </el-select>
        <el-select v-model="radius" class="filter-select" @change="fetchUsers">
          <el-option label="5km" :value="5" />
          <el-option label="10km" :value="10" />
          <el-option label="20km" :value="20" />
          <el-option label="50km" :value="50" />
          <el-option label="100km" :value="100" />
        </el-select>
        <button class="geo-btn primary small" @click="fetchUsers">筛选</button>
      </div>
    </div>

    <!-- Location tip -->
    <div v-if="!authStore.latitude && !authStore.longitude" class="location-tip geo-card accent-yellow">
      <span><el-icon><Location /></el-icon> 尚未获取位置，开启定位以获得更精准的匹配</span>
      <button class="geo-btn primary small" @click="enableLocation" :disabled="locating">
        {{ locating ? '获取中...' : '开启定位' }}
      </button>
    </div>

    <!-- Results Grid -->
    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 正在匹配中...</div>

    <div v-else-if="users.length === 0" class="empty-block">
      <div class="icon"><el-icon :size="48"><Search /></el-icon></div>
      <div style="font-weight:700;font-size:18px;color:var(--color-fg);">暂无匹配用户</div>
      <div style="color:var(--color-muted-fg);margin-top:4px;">试试扩大搜索范围或添加更多技能标签</div>
    </div>

    <template v-else>
      <div class="sort-indicator">按{{ sort === 'score' ? '匹配度' : sort === 'dist' ? '距离' : '活跃度' }}排序 · 共 {{ total }} 人</div>
      <div class="geo-grid-3">
        <div
          v-for="user in users"
          :key="user.userId"
          class="user-card geo-card"
          @click="showUserCard(user)"
        >
          <!-- Header: avatar + name inline -->
          <div class="card-header">
            <div class="card-avatar-wrap">
              <img
                :src="user.avatarUrl || getDefaultAvatar(user.userId || user.name)"
                class="geo-avatar lg card-avatar"
              />
            </div>
            <div class="card-header-info">
              <h3 class="card-name">{{ user.name }}</h3>
              <div class="card-location" v-if="user.city || user.distance">
                <el-icon><Location /></el-icon>
                <span v-if="user.city">{{ user.city }}</span>
                <span v-if="user.city && user.distance" class="loc-sep">·</span>
                <span v-if="user.distance">{{ user.distance }}</span>
              </div>
            </div>
          </div>

          <div class="card-bio" v-if="user.bio">{{ user.bio }}</div>

          <!-- Skills -->
          <div class="card-skills">
            <div class="skill-group" v-if="user.canSkills?.length">
              <div class="skill-label">我会</div>
              <div class="flex-wrap">
                <span v-for="s in user.canSkills.slice(0,3)" :key="s" class="geo-tag can">{{ s }}</span>
                <span v-if="user.canSkills.length > 3" class="geo-tag">+{{ user.canSkills.length - 3 }}</span>
              </div>
            </div>
            <div class="skill-group" v-if="user.wantSkills?.length">
              <div class="skill-label">想学</div>
              <div class="flex-wrap">
                <span v-for="s in user.wantSkills.slice(0,3)" :key="s" class="geo-tag want">{{ s }}</span>
                <span v-if="user.wantSkills.length > 3" class="geo-tag">+{{ user.wantSkills.length - 3 }}</span>
              </div>
            </div>
          </div>

          <!-- Score Bar -->
          <div class="match-bar" v-if="user.matchScore">
            <div class="match-label">匹配度</div>
            <div class="match-track">
              <div class="match-fill" :style="{ width: Math.max(user.matchScore, 8) + '%', background: matchColor(user.matchScore) }"></div>
            </div>
            <span class="match-num" :style="{ color: matchColor(user.matchScore) }">{{ user.matchScore }}%</span>
          </div>
        </div>
      </div>
    </template>

    <!-- Pagination -->
    <div class="flex-center" style="margin-top:32px;" v-if="total > size">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="(p) => { page = p; fetchUsers(); }"
      />
    </div>

    <!-- User Card Dialog -->
    <el-dialog v-model="cardVisible" :title="cardUser?.name" width="420px" destroy-on-close>
      <div v-if="cardUser" class="user-card-detail">
        <div class="detail-header">
          <img
            :src="cardUser.avatarUrl || getDefaultAvatar(cardUser.userId || cardUser.name)"
            class="geo-avatar xl"
          />
          <div>
            <h2 style="font-weight:800;font-family:var(--font-heading);">{{ cardUser.name }}</h2>
            <div v-if="cardUser.signature" style="color:var(--color-muted-fg);font-size:13px;margin-top:2px;">
              {{ cardUser.signature }}
            </div>
            <div v-if="cardUser.city || cardUser.distance" style="color:var(--color-muted-fg);font-size:13px;margin-top:4px;">
              <el-icon><Location /></el-icon>
              <span v-if="cardUser.city">{{ cardUser.city }}</span>
              <span v-if="cardUser.city && cardUser.distance"> · </span>
              <span v-if="cardUser.distance">{{ cardUser.distance }}</span>
            </div>
            <div v-if="cardUser.likeCount || cardUser.postCount" style="color:var(--color-muted-fg);font-size:12px;margin-top:4px;">
              <span v-if="cardUser.likeCount"><el-icon><StarFilled /></el-icon> {{ cardUser.likeCount }}</span>
              <span v-if="cardUser.likeCount && cardUser.postCount"> · </span>
              <span v-if="cardUser.postCount"><el-icon><Document /></el-icon> {{ cardUser.postCount }}</span>
            </div>
          </div>
        </div>

        <div v-if="cardUser.bio" class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-tertiary)"></span> 简介</div>
          <p>{{ cardUser.bio }}</p>
        </div>

        <div class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-quaternary)"></span> 我会</div>
          <div class="flex-wrap">
            <span v-for="s in cardUser.canSkills" :key="s" class="geo-tag can">{{ s }}</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-secondary)"></span> 想学</div>
          <div class="flex-wrap">
            <span v-for="s in cardUser.wantSkills" :key="s" class="geo-tag want">{{ s }}</span>
          </div>
        </div>

        <div v-if="cardUser.matchScore" class="match-bar" style="margin-top:16px;">
          <div class="match-label">匹配度</div>
          <div class="match-track">
            <div class="match-fill" :style="{ width: Math.max(cardUser.matchScore, 8) + '%', background: matchColor(cardUser.matchScore) }"></div>
          </div>
          <span class="match-num" :style="{ color: matchColor(cardUser.matchScore) }">{{ cardUser.matchScore }}%</span>
        </div>

        <!-- AI 匹配分析 -->
        <div class="ai-analysis" v-if="aiExplanation || aiLoading">
          <div class="ai-header">
            <div class="ai-icon-wrap">✦</div>
            <span class="ai-title">AI 智能分析</span>
          </div>
          <div v-if="aiLoading" class="ai-loading">
            <div class="ai-loading-dots">
              <span></span><span></span><span></span>
            </div>
            <span>正在分析匹配原因...</span>
          </div>
          <div v-else class="ai-content">{{ aiExplanation }}</div>
        </div>

        <div class="detail-actions" style="margin-top:20px;display:flex;gap:12px;">
          <button v-if="cardUser.hasPendingRequest" class="geo-btn outline small" disabled>
            <el-icon><Clock /></el-icon> 已发送请求
          </button>
          <button v-else class="geo-btn primary small" @click="sendRequest(cardUser)">
            <el-icon><ChatDotRound /></el-icon> 发起交换
          </button>
          <button class="geo-btn outline small" @click="$router.push(`/user/${cardUser.userId}`)">
            查看主页
          </button>
        </div>
      </div>
    </el-dialog>

    <!-- Send Request Dialog -->
    <el-dialog v-model="requestVisible" title="发起技能交换请求" width="420px">
      <el-form @submit.prevent="confirmRequest">
        <el-form-item label="交换理由">
          <el-input
            v-model="requestReason"
            type="textarea"
            :rows="3"
            placeholder="你好，想和你交流一下..."
            maxlength="150"
            show-word-limit
          />
        </el-form-item>
        <button type="submit" class="geo-btn primary" style="width:100%;justify-content:center;">
          发送请求
        </button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from '@/utils/message'
import { Search, Location, Loading, StarFilled, Document, Clock, ChatDotRound } from '@element-plus/icons-vue'
import { getRecommendedUsers, getUserCard, getMatchExplanation } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { getDefaultAvatar } from '@/utils/avatar'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const locating = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const keyword = ref('')
const sort = ref('score')
const radius = ref(100)

const cardVisible = ref(false)
const cardUser = ref(null)
const aiExplanation = ref('')
const aiLoading = ref(false)

const requestVisible = ref(false)
const requestTarget = ref(null)
const requestReason = ref('')

function matchColor(score) {
  if (score >= 70) return '#34D399'
  if (score >= 40) return '#FBBF24'
  if (score >= 20) return '#F472B6'
  return '#CBD5E1'
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getRecommendedUsers({
      sort: sort.value,
      keyword: keyword.value,
      radius: radius.value,
      page: page.value,
      size: size.value,
    })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function enableLocation() {
  locating.value = true
  try {
    await authStore.fetchLocation()
    if (authStore.latitude && authStore.longitude) {
      ElMessage.success('位置获取成功')
      fetchUsers()
    } else {
      ElMessage.warning('无法获取位置，请检查定位权限')
    }
  } catch { /* handled */ } finally {
    locating.value = false
  }
}

async function showUserCard(user) {
  cardUser.value = { ...user }
  cardVisible.value = true
  aiExplanation.value = ''
  aiLoading.value = true
  try {
    const res = await getUserCard(user.userId)
    if (res.data) {
      cardUser.value = { ...user, ...res.data }
    }
  } catch { /* fallback */ }
  // 异步获取 AI 分析，不阻塞卡片显示
  getMatchExplanation(user.userId).then(res => {
    aiExplanation.value = res.data || ''
  }).catch(() => {
    aiExplanation.value = ''
  }).finally(() => {
    aiLoading.value = false
  })
}

function sendRequest(user) {
  cardVisible.value = false
  requestTarget.value = user
  requestReason.value = ''
  requestVisible.value = true
}

async function confirmRequest() {
  if (!requestReason.value.trim()) {
    ElMessage.warning('请输入交换理由')
    return
  }
  try {
    const res = await createRequest({ toUserId: requestTarget.value.userId, reason: requestReason.value })
    ElMessage.success(res.message || '交换请求已发送')
    requestVisible.value = false
  } catch { /* handled */ }
}

onMounted(fetchUsers)
</script>

<style scoped>
.location-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
}
.filter-bar {
  margin-bottom: 24px;
  padding: 16px;
}
.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filter-search { flex: 1; min-width: 180px; }
.filter-select { width: 130px; flex-shrink: 0; }

.user-card {
  cursor: pointer;
  transition: transform 0.3s var(--ease-bounce), box-shadow 0.3s var(--ease-bounce);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}
/* 左上角装饰色块 */
.user-card::before {
  content: '';
  position: absolute;
  top: -1px;
  left: -1px;
  width: 48px;
  height: 48px;
  background: var(--color-accent);
  border-radius: 0 0 var(--radius-lg) 0;
  opacity: 0.12;
  transition: opacity 0.3s;
}
.user-card:nth-child(3n+2)::before { background: var(--color-secondary); }
.user-card:nth-child(3n+3)::before { background: var(--color-tertiary); }
.user-card:hover::before { opacity: 0.22; }
.user-card:hover {
  transform: rotate(-1deg) scale(1.02);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
}
.card-avatar-wrap {
  flex-shrink: 0;
  position: relative;
}
.card-avatar {
  display: block;
  box-shadow: 3px 3px 0 var(--color-fg);
  background: #fff;
}
.card-header-info {
  flex: 1;
  min-width: 0;
}
.card-name {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 2px;
  color: var(--color-fg);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-location {
  font-size: 12px;
  color: var(--color-muted-fg);
  display: flex;
  align-items: center;
  gap: 4px;
}
.loc-sep { color: #CBD5E1; }
.card-bio {
  font-size: 13px;
  color: var(--color-muted-fg);
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}
.card-skills { margin-bottom: 12px; }
.skill-group { margin-bottom: 8px; }
.skill-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-muted-fg);
  margin-bottom: 4px;
  font-family: var(--font-heading);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Match bar */
.match-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-muted);
  margin-top: auto;
}
.match-label {
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  font-family: var(--font-heading);
  color: var(--color-muted-fg);
}
.match-track {
  flex: 1;
  height: 8px;
  background: #E2E8F0;
  border-radius: var(--radius-full);
  overflow: hidden;
}
.match-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.5s var(--ease-bounce);
}
.sort-indicator {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-muted-fg);
  margin-bottom: 16px;
}
.match-num {
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
  font-family: var(--font-heading);
}

/* Detail */
.detail-header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
}
.detail-section { margin-bottom: 16px; }

@media (max-width: 768px) {
  .filter-row {
    flex-wrap: wrap;
  }
  .filter-search { min-width: 100%; }
  .filter-select { flex: 1; min-width: 0; }
}

/* AI 分析卡片 */
.ai-analysis {
  margin-top: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 2px solid var(--color-accent);
  border-radius: var(--radius-md);
  box-shadow: 4px 4px 0 rgba(139, 92, 246, 0.15);
}
.ai-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.ai-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  font-size: 14px;
  color: var(--color-accent);
  flex-shrink: 0;
}
.ai-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--color-accent);
}
.ai-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--color-muted-fg);
}
.ai-loading-dots {
  display: flex;
  gap: 4px;
}
.ai-loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  animation: aiDotPulse 1.2s ease-in-out infinite;
}
.ai-loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.ai-loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes aiDotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.2); }
}
.ai-content {
  font-size: 14px;
  color: var(--color-fg);
  line-height: 1.8;
}
</style>
