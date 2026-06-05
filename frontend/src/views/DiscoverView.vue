<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><el-icon><Search /></el-icon> 发现技能搭子</h1>
      <p class="page-subtitle">找到与你技能互补的人</p>
    </header>

    <!-- Filters -->
    <div class="filter-bar brutal-card accent-yellow">
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
        <button class="brutal-btn primary small" @click="fetchUsers">筛选</button>
      </div>
    </div>

    <!-- Location tip -->
    <div v-if="!authStore.latitude && !authStore.longitude" class="location-tip brutal-card accent-yellow">
      <span><el-icon><Location /></el-icon> 尚未获取位置，开启定位以获得更精准的匹配</span>
      <button class="brutal-btn primary small" @click="enableLocation" :disabled="locating">
        {{ locating ? '获取中...' : '开启定位' }}
      </button>
    </div>

    <!-- Results Grid -->
    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 正在匹配中...</div>

    <div v-else-if="users.length === 0" class="empty-block">
      <div class="icon"><el-icon :size="48"><Search /></el-icon></div>
      <div style="font-weight:800;font-size:18px;">暂无匹配用户</div>
      <div style="color:#888;margin-top:4px;">试试扩大搜索范围或添加更多技能标签</div>
    </div>

    <template v-else>
      <div class="sort-indicator">按{{ sort === 'score' ? '匹配度' : sort === 'dist' ? '距离' : '活跃度' }}排序 · 共 {{ total }} 人</div>
      <div class="brutal-grid-3">
      <div
        v-for="user in users"
        :key="user.userId"
        class="user-card brutal-card accent-blue"
        @click="showUserCard(user)"
      >
        <div class="card-cover pop-dots"></div>
        <div class="card-body">
          <img
            :src="user.avatarUrl || getDefaultAvatar(user.userId || user.name)"
            class="brutal-avatar lg card-avatar"
          />
          <h3 class="card-name">{{ user.name }}</h3>
          <div class="card-location" v-if="user.city || user.distance">
            <el-icon><Location /></el-icon>
            <span v-if="user.city">{{ user.city }}</span>
            <span v-if="user.city && user.distance" class="loc-sep">·</span>
            <span v-if="user.distance">{{ user.distance }}</span>
          </div>
          <div class="card-bio" v-if="user.bio">{{ user.bio }}</div>

          <!-- Skills -->
          <div class="card-skills">
            <div class="skill-group" v-if="user.canSkills?.length">
              <div class="skill-label">我会</div>
              <div class="flex-wrap">
                <span v-for="s in user.canSkills.slice(0,3)" :key="s" class="brutal-tag can">{{ s }}</span>
                <span v-if="user.canSkills.length > 3" class="brutal-tag">+{{ user.canSkills.length - 3 }}</span>
              </div>
            </div>
            <div class="skill-group" v-if="user.wantSkills?.length">
              <div class="skill-label">想学</div>
              <div class="flex-wrap">
                <span v-for="s in user.wantSkills.slice(0,3)" :key="s" class="brutal-tag want">{{ s }}</span>
                <span v-if="user.wantSkills.length > 3" class="brutal-tag">+{{ user.wantSkills.length - 3 }}</span>
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
            class="brutal-avatar xl"
          />
          <div>
            <h2 style="font-weight:900;">{{ cardUser.name }}</h2>
            <div v-if="cardUser.signature" style="color:#666;font-size:13px;margin-top:2px;">
              {{ cardUser.signature }}
            </div>
            <div v-if="cardUser.city || cardUser.distance" style="color:#888;font-size:13px;margin-top:4px;">
              <el-icon><Location /></el-icon>
              <span v-if="cardUser.city">{{ cardUser.city }}</span>
              <span v-if="cardUser.city && cardUser.distance"> · </span>
              <span v-if="cardUser.distance">{{ cardUser.distance }}</span>
            </div>
            <div v-if="cardUser.likeCount || cardUser.postCount" style="color:#888;font-size:12px;margin-top:4px;">
              <span v-if="cardUser.likeCount"><el-icon><StarFilled /></el-icon> {{ cardUser.likeCount }}</span>
              <span v-if="cardUser.likeCount && cardUser.postCount"> · </span>
              <span v-if="cardUser.postCount"><el-icon><Document /></el-icon> {{ cardUser.postCount }}</span>
            </div>
          </div>
        </div>

        <div v-if="cardUser.bio" class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 简介</div>
          <p>{{ cardUser.bio }}</p>
        </div>

        <div class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-cyan)"></span> 我会</div>
          <div class="flex-wrap">
            <span v-for="s in cardUser.canSkills" :key="s" class="brutal-tag can">{{ s }}</span>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title"><span class="dot" style="background:var(--color-pink)"></span> 想学</div>
          <div class="flex-wrap">
            <span v-for="s in cardUser.wantSkills" :key="s" class="brutal-tag want">{{ s }}</span>
          </div>
        </div>

        <div v-if="cardUser.matchScore" class="match-bar" style="margin-top:16px;">
          <div class="match-label">匹配度</div>
          <div class="match-track">
            <div class="match-fill" :style="{ width: Math.max(cardUser.matchScore, 8) + '%', background: matchColor(cardUser.matchScore) }"></div>
          </div>
          <span class="match-num" :style="{ color: matchColor(cardUser.matchScore) }">{{ cardUser.matchScore }}%</span>
        </div>

        <div class="detail-actions" style="margin-top:20px;display:flex;gap:12px;">
          <button v-if="cardUser.hasPendingRequest" class="brutal-btn outline small" disabled>
            <el-icon><Clock /></el-icon> 已发送请求
          </button>
          <button v-else class="brutal-btn primary small" @click="sendRequest(cardUser)">
            <el-icon><ChatDotRound /></el-icon> 发起交换
          </button>
          <button class="brutal-btn outline small" @click="$router.push(`/user/${cardUser.userId}`)">
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
        <button type="submit" class="brutal-btn primary" style="width:100%;justify-content:center;">
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
import { getRecommendedUsers, getUserCard } from '@/api/matching'
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

const requestVisible = ref(false)
const requestTarget = ref(null)
const requestReason = ref('')

function matchColor(score) {
  if (score >= 70) return '#22c55e'
  if (score >= 40) return '#eab308'
  if (score >= 20) return '#f97316'
  return '#94a3b8'
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
  // 先用列表数据立即展示
  cardUser.value = { ...user }
  cardVisible.value = true
  // 再调接口获取完整名片（签名、发帖数、是否已发请求）
  try {
    const res = await getUserCard(user.userId)
    if (res.data) {
      cardUser.value = { ...user, ...res.data }
    }
  } catch { /* 降级：列表数据已展示 */ }
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
  font-weight: 700;
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
.filter-tags { margin-top: 12px; }

.user-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.user-card:hover {
  transform: translate(-3px, -3px);
  box-shadow: 8px 8px 0 #0057FF;
}
.card-cover {
  height: 80px;
  background-color: #f0f0f0;
}
.card-body {
  padding: 0 16px 20px;
  text-align: center;
  margin-top: -24px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.card-avatar {
  display: block;
  margin: 0 auto;
}
.card-name {
  font-size: 18px;
  font-weight: 900;
  margin: 8px 0 4px;
}
.card-location {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
}
.loc-sep {
  color: #ccc;
}
.card-bio {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-skills { margin-bottom: 12px; }
.skill-group { margin-bottom: 8px; }
.skill-label {
  font-size: 12px;
  font-weight: 700;
  color: #888;
  text-transform: uppercase;
  margin-bottom: 4px;
}

/* Match bar */
.match-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 2px solid #1A1A1A;
  background: #fafafa;
  margin-top: auto;
}
.match-label {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  flex-shrink: 0;
}
.match-track {
  flex: 1;
  height: 8px;
  background: #eee;
  border: 1px solid #ccc;
}
.match-fill {
  height: 100%;
  transition: width 0.3s;
}
.sort-indicator {
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #888;
  margin-bottom: 16px;
}
.match-num {
  font-size: 14px;
  font-weight: 900;
  flex-shrink: 0;
}

/* Detail */
.user-card-detail {}
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
</style>
