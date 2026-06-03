<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title"><i class="pi pi-compass"></i> 发现技能搭子</h1>
      <p class="page-subtitle">找到与你技能互补的人</p>
    </header>

    <!-- Filters -->
    <div class="filter-bar brutal-card accent-yellow">
      <div class="filter-row">
        <div class="filter-search-wrap">
          <i class="pi pi-search filter-search-icon"></i>
          <input
            v-model="keyword"
            class="brutal-input filter-search-input"
            placeholder="搜索昵称、技能..."
            autocomplete="off"
            @keyup.enter="fetchUsers"
          />
        </div>
        <Select
          v-model="sort"
          :options="sortOptions"
          option-label="label"
          option-value="value"
          class="filter-select"
        />
        <button class="brutal-btn primary small" @click="page = 1; fetchUsers()">筛选</button>
      </div>
    </div>

    <!-- Location tip -->
    <div v-if="!authStore.latitude && !authStore.longitude" class="location-tip brutal-card accent-yellow">
      <span><i class="pi pi-map-marker"></i> 尚未获取位置，开启定位以获得更精准的匹配</span>
      <button class="brutal-btn primary small" @click="enableLocation" :disabled="locating">
        {{ locating ? '获取中...' : '开启定位' }}
      </button>
    </div>

    <!-- 列表视图 -->
    <div v-if="loading" class="loading-block"><i class="pi pi-spinner pi-spin"></i> 正在匹配中...</div>

    <div v-else-if="users.length === 0" class="empty-block">
      <div class="icon"><i class="pi pi-compass" style="font-size:48px;"></i></div>
      <div style="font-weight:800;font-size:18px;">暂无匹配用户</div>
      <div style="color:#888;margin-top:4px;">试试扩大搜索范围或添加更多技能标签</div>
    </div>

    <template v-else>
      <div class="sort-indicator">按{{ sortLabel }}排序 · 共 {{ total }} 人</div>
      <div class="brutal-grid-3">
        <div
          v-for="(user, idx) in users"
          :key="user.userId"
          class="user-card brutal-card accent-blue"
          :style="{ animationDelay: idx * 0.05 + 's' }"
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
              <i class="pi pi-map-marker" style="font-size:13px;"></i>
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
      <Paginator
        v-model:first="first"
        :rows="size"
        :total-records="total"
        :rows-per-page-options="[12]"
        template="PrevPageLink PageLinks NextPageLink"
        @page="onPageChange"
      />
    </div>

    <!-- User Card Dialog -->
    <Dialog v-model:visible="cardVisible" :header="cardUser?.name" :modal="true" :style="{ width: '420px' }" :pt="{ root: 'brutal-dialog' }">
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
              <i class="pi pi-map-marker"></i>
              <span v-if="cardUser.city">{{ cardUser.city }}</span>
              <span v-if="cardUser.city && cardUser.distance"> · </span>
              <span v-if="cardUser.distance">{{ cardUser.distance }}</span>
            </div>
            <div v-if="cardUser.likeCount || cardUser.postCount" style="color:#888;font-size:12px;margin-top:4px;">
              <span v-if="cardUser.likeCount"><i class="pi pi-star-fill"></i> {{ cardUser.likeCount }}</span>
              <span v-if="cardUser.likeCount && cardUser.postCount"> · </span>
              <span v-if="cardUser.postCount"><i class="pi pi-file"></i> {{ cardUser.postCount }}</span>
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
            <i class="pi pi-clock"></i> 已发送请求
          </button>
          <button v-else class="brutal-btn primary small" @click="sendRequest(cardUser)">
            <i class="pi pi-send"></i> 发起交换
          </button>
          <button class="brutal-btn outline small" @click="router.push(`/user/${cardUser.userId}`)">
            查看主页
          </button>
        </div>
      </div>
    </Dialog>

    <!-- Send Request Dialog -->
    <Dialog v-model:visible="requestVisible" header="发起技能交换请求" :modal="true" :style="{ width: '420px' }">
      <div class="request-form">
        <label class="field-label">交换理由</label>
        <textarea
          v-model="requestReason"
          class="brutal-input request-textarea"
          placeholder="你好，想和你交流一下..."
          maxlength="150"
          rows="3"
        ></textarea>
        <div class="field-count">{{ requestReason.length }}/150</div>
        <button class="brutal-btn primary" style="width:100%;justify-content:center;margin-top:12px;" @click="confirmRequest">
          发送请求
        </button>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Paginator from 'primevue/paginator'
import { getRecommendedUsers, getUserCard } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { getDefaultAvatar } from '@/utils/avatar'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()
const loading = ref(false)
const locating = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const first = ref(0)
const size = ref(12)
const keyword = ref('')
const sort = ref('score')

const sortOptions = [
  { label: '匹配度', value: 'score' },
  { label: '距离近', value: 'dist' },
  { label: '活跃度', value: 'active' },
]

const sortLabel = computed(() => {
  const opt = sortOptions.find(o => o.value === sort.value)
  return opt?.label || '匹配度'
})

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

function onPageChange(event) {
  page.value = event.page + 1
  first.value = event.first
  fetchUsers()
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getRecommendedUsers({
      sort: sort.value,
      keyword: keyword.value,
      page: page.value,
      size: size.value,
    })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled by interceptor */ } finally {
    loading.value = false
  }
}

async function enableLocation() {
  locating.value = true
  try {
    await authStore.fetchLocation()
    if (authStore.latitude && authStore.longitude) {
      toast.add({ severity: 'success', summary: '成功', detail: '位置获取成功', life: 3000 })
      await new Promise(r => setTimeout(r, 500))
      fetchUsers()
    } else {
      toast.add({ severity: 'warn', summary: '提示', detail: '无法获取位置，请检查定位权限', life: 3000 })
    }
  } catch { /* handled */ } finally {
    locating.value = false
  }
}

async function showUserCard(user) {
  cardUser.value = { ...user }
  cardVisible.value = true
  try {
    const res = await getUserCard(user.userId)
    if (res.data) {
      cardUser.value = { ...user, ...res.data }
    }
  } catch { /* 降级 */ }
}

function sendRequest(user) {
  if (String(authStore.user?.userId) === String(user.userId)) {
    toast.add({ severity: 'warn', summary: '提示', detail: '不能给自己发送交换请求', life: 3000 })
    return
  }
  cardVisible.value = false
  requestTarget.value = user
  requestReason.value = ''
  requestVisible.value = true
}

async function confirmRequest() {
  if (!requestReason.value.trim()) {
    toast.add({ severity: 'warn', summary: '提示', detail: '请输入交换理由', life: 3000 })
    return
  }
  try {
    const res = await createRequest({ toUserId: requestTarget.value.userId, reason: requestReason.value })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '交换请求已发送', life: 3000 })
    requestVisible.value = false
  } catch { /* handled by interceptor */ }
}

onMounted(async () => {
  if (authStore.latitude && authStore.longitude) {
    fetchUsers()
  } else {
    loading.value = true
    try { await authStore.fetchLocation() } catch { /* ignore */ }
    fetchUsers()
  }
})
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
.filter-bar { margin-bottom: 24px; padding: 16px; }
.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filter-search-wrap {
  flex: 1;
  min-width: 180px;
  position: relative;
}
.filter-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #aaa;
  z-index: 1;
  font-size: 16px;
}
.filter-search-input {
  padding-left: 36px !important;
}
.filter-select { width: 130px; flex-shrink: 0; }

/* 用户卡片 */
.user-card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  animation: cardFadeIn 0.4s ease-out both;
}
@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
.user-card:hover {
  transform: translate(-3px, -3px);
  box-shadow: 8px 8px 0 #0057FF;
}
.card-cover { height: 80px; background-color: #f0f0f0; }
.card-body {
  padding: 0 16px 20px;
  text-align: center;
  margin-top: -24px;
}
.card-avatar { display: block; margin: 0 auto; }
.card-name { font-size: 18px; font-weight: 900; margin: 8px 0 4px; }
.card-location {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
}
.loc-sep { color: #ccc; }
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
}
.match-label { font-size: 12px; font-weight: 700; text-transform: uppercase; flex-shrink: 0; }
.match-track { flex: 1; height: 8px; background: #eee; border: 1px solid #ccc; }
.match-fill { height: 100%; transition: width 0.3s; }
.match-num { font-size: 14px; font-weight: 900; flex-shrink: 0; }
.sort-indicator {
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #888;
  margin-bottom: 16px;
}

/* Detail */
.detail-header { display: flex; gap: 16px; align-items: center; margin-bottom: 20px; }
.detail-section { margin-bottom: 16px; }

/* Request form */
.request-form { display: flex; flex-direction: column; gap: 8px; }
.field-label {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #555;
}
.request-textarea {
  width: 100%;
  resize: vertical;
  font-family: var(--font-sans);
  font-size: 15px;
  padding: 12px 14px;
}
.field-count {
  text-align: right;
  font-size: 12px;
  color: #aaa;
}

@media (max-width: 768px) {
  .filter-row { flex-wrap: wrap; }
  .filter-search-wrap { min-width: 100%; }
  .filter-select { flex: 1; min-width: 0; }
}
</style>
