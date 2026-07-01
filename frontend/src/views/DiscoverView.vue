<template>
  <div class="page-container">
    <!-- 底部浮动几何装饰 -->
    <div class="falling-shapes" aria-hidden="true">
      <!-- 左侧密集 -->
      <div class="shape shape-tri" style="left:0%;--o:0.22;animation-duration:22s;animation-delay:0s;width:36px;height:36px;"></div>
      <div class="shape shape-sq" style="left:2%;--o:0.19;animation-duration:25s;animation-delay:2s;width:28px;height:28px;"></div>
      <div class="shape shape-circle" style="left:4%;--o:0.20;animation-duration:23s;animation-delay:4s;width:32px;height:32px;"></div>
      <div class="shape shape-diamond" style="left:6%;--o:0.17;animation-duration:26s;animation-delay:1s;width:24px;height:24px;"></div>
      <div class="shape shape-trap" style="left:8%;--o:0.19;animation-duration:24s;animation-delay:3s;width:30px;height:22px;"></div>
      <div class="shape shape-tri" style="left:10%;--o:0.21;animation-duration:27s;animation-delay:5s;width:34px;height:34px;"></div>
      <div class="shape shape-sq" style="left:12%;--o:0.18;animation-duration:22s;animation-delay:0.5s;width:26px;height:26px;"></div>
      <div class="shape shape-circle" style="left:14%;--o:0.23;animation-duration:25s;animation-delay:6s;width:30px;height:30px;"></div>
      <div class="shape shape-diamond" style="left:16%;--o:0.15;animation-duration:24s;animation-delay:2.5s;width:28px;height:28px;"></div>
      <div class="shape shape-trap" style="left:18%;--o:0.18;animation-duration:26s;animation-delay:4.5s;width:32px;height:24px;"></div>
      <div class="shape shape-circle" style="left:20%;--o:0.20;animation-duration:23s;animation-delay:1.5s;width:30px;height:30px;"></div>
      <!-- 中间稀疏 -->
      <div class="shape shape-tri" style="left:30%;--o:0.14;animation-duration:25s;animation-delay:3s;width:28px;height:28px;"></div>
      <div class="shape shape-sq" style="left:40%;--o:0.13;animation-duration:27s;animation-delay:6s;width:26px;height:26px;"></div>
      <div class="shape shape-diamond" style="left:50%;--o:0.15;animation-duration:24s;animation-delay:1s;width:30px;height:30px;"></div>
      <div class="shape shape-trap" style="left:60%;--o:0.14;animation-duration:26s;animation-delay:4s;width:24px;height:18px;"></div>
      <div class="shape shape-circle" style="left:70%;--o:0.13;animation-duration:23s;animation-delay:7s;width:26px;height:26px;"></div>
      <!-- 右侧密集 -->
      <div class="shape shape-tri" style="left:80%;--o:0.20;animation-duration:24s;animation-delay:1.5s;width:34px;height:34px;"></div>
      <div class="shape shape-sq" style="left:82%;--o:0.22;animation-duration:26s;animation-delay:3.5s;width:30px;height:30px;"></div>
      <div class="shape shape-circle" style="left:84%;--o:0.18;animation-duration:23s;animation-delay:0.5s;width:28px;height:28px;"></div>
      <div class="shape shape-diamond" style="left:86%;--o:0.21;animation-duration:25s;animation-delay:5.5s;width:32px;height:32px;"></div>
      <div class="shape shape-trap" style="left:88%;--o:0.19;animation-duration:27s;animation-delay:2.5s;width:26px;height:19px;"></div>
      <div class="shape shape-tri" style="left:90%;--o:0.23;animation-duration:22s;animation-delay:4.5s;width:36px;height:36px;"></div>
      <div class="shape shape-sq" style="left:92%;--o:0.17;animation-duration:25s;animation-delay:1s;width:28px;height:28px;"></div>
      <div class="shape shape-circle" style="left:94%;--o:0.20;animation-duration:24s;animation-delay:6s;width:30px;height:30px;"></div>
      <div class="shape shape-diamond" style="left:96%;--o:0.16;animation-duration:26s;animation-delay:3s;width:34px;height:34px;"></div>
      <div class="shape shape-trap" style="left:98%;--o:0.22;animation-duration:23s;animation-delay:0s;width:28px;height:21px;"></div>
      <div class="shape shape-circle" style="left:100%;--o:0.18;animation-duration:25s;animation-delay:5s;width:30px;height:30px;transform:translateX(-100%);"></div>
    </div>

    <header class="page-header" style="display:flex;align-items:baseline;gap:16px;">
      <h1 class="page-title"><el-icon><Search /></el-icon> 发现技能搭子</h1>
      <p class="page-subtitle">找到与你技能互补的人</p>
    </header>

    <!-- 全局用户搜索 -->
    <div class="search-bar geo-card">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户昵称、技能..."
        :prefix-icon="Search"
        clearable
        size="large"
        class="global-search-input"
        @keyup.enter="doSearch"
        @clear="clearSearch"
      />
      <button class="geo-btn primary" @click="doSearch">搜索</button>
    </div>

    <!-- 搜索状态 -->
    <div v-if="isSearching" class="search-status">
      <span>搜索 "{{ searchKeyword }}" · 共找到 {{ searchTotal }} 人</span>
      <span class="back-link" @click="clearSearch">← 返回推荐</span>
    </div>

    <!-- 搜索结果 -->
    <template v-if="isSearching">
      <div v-if="searchLoading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 搜索中...</div>
      <div v-else-if="searchResults.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><Search /></el-icon></div>
        <div style="font-weight:700;font-size:18px;color:var(--color-fg);">未找到相关用户</div>
        <div style="color:var(--color-muted-fg);margin-top:4px;">换个关键词试试</div>
      </div>
      <template v-else>
        <div class="search-list">
          <div v-for="user in searchResults" :key="user.userId" class="search-item geo-card" @click="showUserCard(user)">
            <img :src="user.avatarUrl || getDefaultAvatar(user.userId || user.name)" class="search-avatar" />
            <div class="search-info">
              <div class="search-name">{{ user.name }}</div>
              <div class="search-meta">
                <span v-if="user.city"><el-icon><Location /></el-icon> {{ user.city }}</span>
                <span v-if="user.distance" class="search-dist">{{ user.distance }}</span>
              </div>
            </div>
            <div class="search-skills">
              <span v-for="s in (user.canSkills || []).slice(0,2)" :key="s" class="geo-tag can" style="font-size:11px;padding:2px 8px;">{{ s }}</span>
              <span v-for="s in (user.wantSkills || []).slice(0,2)" :key="s" class="geo-tag want" style="font-size:11px;padding:2px 8px;">{{ s }}</span>
            </div>
          </div>
        </div>
        <div class="flex-center" style="margin-top:32px;" v-if="searchTotal > searchSize">
          <el-pagination :current-page="searchPage" :page-size="searchSize" :total="searchTotal" layout="prev, pager, next" @current-change="(p) => { searchPage = p; doSearch(); }" />
        </div>
      </template>
    </template>

    <!-- 发现页推荐 -->
    <template v-else>
      <!-- 主控制区域 -->
      <div class="control-bar geo-card">
        <div class="segment-control">
          <button
            class="segment-btn"
            :class="{ active: matchType === 'standard' }"
            @click="switchMatchType('standard')"
          >
            普通匹配
          </button>
          <button
            class="segment-btn"
            :class="{ active: matchType === 'nearby' }"
            @click="switchMatchType('nearby')"
          >
            附近匹配
          </button>
        </div>
        <button class="geo-btn refresh-btn" @click="refreshUsers" :disabled="refreshing || refreshCooldown > 0">
          <el-icon class="refresh-icon" :class="{ spinning: refreshing }"><Refresh /></el-icon>
          {{ refreshCooldown > 0 ? `${refreshCooldown}s 后可刷新` : '刷新匹配' }}
        </button>
      </div>

      <!-- Location tip (仅附近匹配) -->
      <div v-if="matchType === 'nearby' && !authStore.latitude && !authStore.longitude" class="location-tip geo-card accent-yellow">
        <span><el-icon><Location /></el-icon> 尚未获取位置，开启定位以获得更精准的匹配</span>
        <button class="geo-btn primary small" @click="enableLocation" :disabled="locating">
          {{ locating ? '获取中...' : '开启定位' }}
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 正在匹配中...</div>

      <!-- 空状态 -->
      <div v-else-if="users.length === 0" class="empty-block">
        <div class="icon"><el-icon :size="48"><Search /></el-icon></div>
        <div style="font-weight:700;font-size:18px;color:var(--color-fg);">暂无匹配用户</div>
        <div style="color:var(--color-muted-fg);margin-top:4px;">试试添加更多技能标签</div>
      </div>

      <!-- 用户卡片网格 -->
      <template v-else>
        <div class="geo-grid-3">
          <div v-for="(user, idx) in users" :key="user.userId" class="user-card geo-card" @click="showUserCard(user)">
            <!-- 顶部装饰条 -->
            <div class="card-deco-bar" :class="'deco-' + (idx % 3)"></div>

            <!-- 头部：头像 + 名字 + 位置 + 签名 -->
            <div class="card-header">
              <div class="card-avatar-wrap">
                <img :src="user.avatarUrl || getDefaultAvatar(user.userId || user.name)" class="card-avatar" />
                <div class="avatar-ring" :class="'ring-' + (idx % 3)"></div>
              </div>
              <div class="card-header-info">
                <h3 class="card-name">{{ user.name }}</h3>
                <div class="card-location" v-if="user.city || user.distance">
                  <el-icon><Location /></el-icon>
                  <span v-if="user.city">{{ user.city }}</span>
                  <span v-if="user.city && user.distance" class="loc-sep">·</span>
                  <span v-if="user.distance">{{ user.distance }}</span>
                </div>
                <div class="card-bio" v-if="user.bio">{{ user.bio.length > 30 ? user.bio.slice(0, 30) + '...' : user.bio }}</div>
              </div>
            </div>

            <!-- 技能标签 -->
            <div class="card-skills">
              <div class="skill-row" v-if="user.canSkills?.length">
                <span class="skill-pill can-pill">教</span>
                <div class="flex-wrap">
                  <span v-for="s in user.canSkills.slice(0,3)" :key="s" class="geo-tag can">{{ s }}</span>
                  <span v-if="user.canSkills.length > 3" class="geo-tag more-tag">+{{ user.canSkills.length - 3 }}</span>
                </div>
              </div>
              <div class="skill-row" v-if="user.wantSkills?.length">
                <span class="skill-pill want-pill">学</span>
                <div class="flex-wrap">
                  <span v-for="s in user.wantSkills.slice(0,3)" :key="s" class="geo-tag want">{{ s }}</span>
                  <span v-if="user.wantSkills.length > 3" class="geo-tag more-tag">+{{ user.wantSkills.length - 3 }}</span>
                </div>
              </div>
            </div>

            <!-- AI 建议 -->
            <div class="ai-suggestion" :class="'ai-' + (idx % 3)" style="margin-top:auto;">
              <div class="ai-suggestion-icon">✦</div>
              <div class="ai-suggestion-content">
                <div class="ai-suggestion-label">AI 建议</div>
                <template v-if="user._aiLoading">
                  <div class="ai-suggestion-loading">
                    <div class="loading-dots"><span></span><span></span><span></span></div>
                    <span>AI 正在思考...</span>
                  </div>
                </template>
                <p v-else class="ai-suggestion-text">{{ user.aiSuggestion }}</p>
              </div>
            </div>

            <!-- 匹配度 -->
            <div class="match-score-bar" @click.stop="loadMatchScore(user)">
              <template v-if="user._scoreLoading">
                <div class="score-loading">
                  <div class="loading-dots"><span></span><span></span><span></span></div>
                  <span>正在分析...</span>
                </div>
              </template>
              <template v-else-if="user._matchScore !== undefined && user._matchScore !== null">
                <div class="score-result">
                  <div class="match-track">
                    <div class="match-fill" :style="{ width: Math.max(user._matchScore, 8) + '%', background: matchColor(user._matchScore) }"></div>
                  </div>
                  <span class="score-value" :style="{ color: matchColor(user._matchScore) }">{{ user._matchScore }}%</span>
                </div>
              </template>
              <template v-else>
                <span class="score-prompt">点击查看匹配度</span>
              </template>
            </div>
          </div>
        </div>
      </template>
    </template>

    <!-- User Card Dialog -->
    <el-dialog v-model="cardVisible" :title="cardUser?.name" width="420px" destroy-on-close>
      <div v-if="cardUser" class="user-card-detail">
        <div class="detail-header">
          <img :src="cardUser.avatarUrl || getDefaultAvatar(cardUser.userId || cardUser.name)" class="geo-avatar xl" />
          <div>
            <h2 style="font-weight:800;font-family:var(--font-heading);">{{ cardUser.name }}</h2>
            <div v-if="cardUser.signature" style="color:var(--color-muted-fg);font-size:13px;margin-top:2px;">{{ cardUser.signature }}</div>
            <div v-if="cardUser.city || cardUser.distance" style="color:var(--color-muted-fg);font-size:13px;margin-top:4px;">
              <el-icon><Location /></el-icon>
              <span v-if="cardUser.city">{{ cardUser.city }}</span>
              <span v-if="cardUser.city && cardUser.distance"> · </span>
              <span v-if="cardUser.distance">{{ cardUser.distance }}</span>
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

        <div class="detail-actions" style="margin-top:20px;display:flex;gap:12px;">
          <button v-if="cardUser.hasPendingRequest" class="geo-btn outline small" disabled>
            <el-icon><Clock /></el-icon> 已发送请求
          </button>
          <button v-else class="geo-btn primary small" @click="sendRequest(cardUser)">
            <el-icon><ChatDotRound /></el-icon> 发起交换
          </button>
          <button class="geo-btn outline small" @click="$router.push(`/user/${cardUser.userId}`)">查看主页</button>
        </div>
      </div>
    </el-dialog>

    <!-- Send Request Dialog -->
    <el-dialog v-model="requestVisible" title="发起技能交换请求" width="420px">
      <el-form @submit.prevent="confirmRequest">
        <el-form-item label="交换理由">
          <el-input v-model="requestReason" type="textarea" :rows="3" placeholder="你好，想和你交流一下..." maxlength="150" show-word-limit />
        </el-form-item>
        <button type="submit" class="geo-btn primary" style="width:100%;justify-content:center;">发送请求</button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from '@/utils/message'
import { Search, Location, Loading, Clock, ChatDotRound, Refresh } from '@element-plus/icons-vue'
import { getDiscoverUsers, getUserCard, searchUsers, getMatchScore, getAiSuggestion } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { getDefaultAvatar } from '@/utils/avatar'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const locating = ref(false)
const refreshing = ref(false)
const users = ref([])
const matchType = ref('standard')
const shownUserIds = ref([])
const refreshCooldown = ref(0)
let cooldownTimer = null

// sessionStorage 持久化
function loadFromStorage() {
  try {
    const ids = sessionStorage.getItem('discover_shown_ids')
    if (ids) shownUserIds.value = JSON.parse(ids)
    const cards = sessionStorage.getItem('discover_cards')
    if (cards) {
      const parsed = JSON.parse(cards)
      if (parsed.length > 0) return parsed
    }
  } catch {}
  return null
}
function saveToStorage() {
  sessionStorage.setItem('discover_shown_ids', JSON.stringify(shownUserIds.value))
  sessionStorage.setItem('discover_cards', JSON.stringify(users.value))
}

// 搜索
const searchKeyword = ref('')
const searchResults = ref([])
const searchTotal = ref(0)
const searchPage = ref(1)
const searchSize = ref(12)
const searchLoading = ref(false)
const isSearching = ref(false)

const cardVisible = ref(false)
const cardUser = ref(null)

const requestVisible = ref(false)
const requestTarget = ref(null)
const requestReason = ref('')

function matchColor(score) {
  if (score >= 70) return '#34D399'
  if (score >= 40) return '#FBBF24'
  if (score >= 20) return '#F472B6'
  return '#CBD5E1'
}

async function fetchDiscoverUsers() {
  loading.value = true
  try {
    const params = { matchType: matchType.value }
    if (shownUserIds.value.length > 0) {
      params.excludeUserIds = shownUserIds.value.join(',')
    }
    const res = await getDiscoverUsers(params)
    let newUsers = res.data || []

    // 用户池用完，静默重新获取（用户无感知）
    if (newUsers.length === 0 && shownUserIds.value.length > 0) {
      shownUserIds.value = []
      saveToStorage()
      const retryRes = await getDiscoverUsers({ matchType: matchType.value })
      newUsers = retryRes.data || []
    }

    if (newUsers.length > 0) {
      users.value = newUsers.map(u => ({ ...u, _matchScore: null, _scoreLoading: false, _aiLoading: true }))
      shownUserIds.value = [...shownUserIds.value, ...newUsers.map(u => u.userId)]
      saveToStorage()
      // 异步加载 AI 建议（不阻塞卡片展示）
      loadAiSuggestions(newUsers)
      // 启动 10 秒冷却
      startCooldown()
    } else {
      users.value = []
    }
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

// 异步加载 AI 建议（逐个替换规则建议）
function loadAiSuggestions(userList) {
  userList.forEach(async (u) => {
    try {
      const res = await getAiSuggestion(u.userId)
      if (res.data) {
        const idx = users.value.findIndex(item => item.userId === u.userId)
        if (idx !== -1) {
          users.value[idx].aiSuggestion = res.data
        }
      }
    } catch {} finally {
      const idx = users.value.findIndex(item => item.userId === u.userId)
      if (idx !== -1) {
        users.value[idx]._aiLoading = false
        saveToStorage()
      }
    }
  })
}

// 刷新冷却（10秒）
function startCooldown() {
  refreshCooldown.value = 10
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    refreshCooldown.value--
    if (refreshCooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

async function refreshUsers() {
  refreshing.value = true
  await fetchDiscoverUsers()
  refreshing.value = false
}

function switchMatchType(type) {
  if (matchType.value === type) return
  matchType.value = type
  // 只切换模式，不自动刷新，等用户点刷新按钮
}

async function loadMatchScore(user) {
  if (user._scoreLoading || user._matchScore !== null) return
  user._scoreLoading = true
  try {
    const res = await getMatchScore(user.userId)
    await new Promise(resolve => setTimeout(resolve, 800))
    user._matchScore = res.data ?? 0
  } catch {
    user._matchScore = 0
  } finally {
    user._scoreLoading = false
  }
}

async function enableLocation() {
  locating.value = true
  try {
    await authStore.fetchLocation()
    if (authStore.latitude && authStore.longitude) {
      ElMessage.success('位置获取成功')
      fetchDiscoverUsers()
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
  try {
    const res = await getUserCard(user.userId)
    if (res.data) cardUser.value = { ...user, ...res.data }
  } catch { /* fallback */ }
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

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  isSearching.value = true
  searchLoading.value = true
  try {
    const res = await searchUsers({ keyword: kw, page: searchPage.value, size: searchSize.value })
    searchResults.value = res.data?.list || []
    searchTotal.value = res.data?.total || 0
  } catch { /* handled */ } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  searchResults.value = []
  searchTotal.value = 0
  searchPage.value = 1
  isSearching.value = false
}

onMounted(() => {
  const saved = loadFromStorage()
  if (saved) {
    users.value = saved
    // 已缓存的卡片不需要重新加载 AI 建议
  } else {
    fetchDiscoverUsers()
  }
})

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
/* 顶部飘落几何背景 */
.falling-shapes {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.shape {
  position: absolute;
  top: 0;
  opacity: 0;
  animation: shapeFall linear infinite;
  filter: blur(0.5px);
}
.shape-tri {
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.5), rgba(244, 114, 182, 0.3));
  border: 1px solid rgba(236, 72, 153, 0.15);
}
.shape-sq {
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.45), rgba(253, 186, 116, 0.25));
  border: 1px solid rgba(251, 146, 60, 0.12);
}
.shape-trap {
  clip-path: polygon(12% 0%, 88% 0%, 100% 100%, 0% 100%);
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.45), rgba(110, 231, 183, 0.25));
  border: 1px solid rgba(52, 211, 153, 0.12);
}
.shape-circle {
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(244, 114, 182, 0.4), rgba(251, 113, 133, 0.2));
  border: 1px solid rgba(244, 114, 182, 0.12);
}
.shape-diamond {
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.4), rgba(251, 146, 60, 0.3));
}
@keyframes shapeFall {
  0% {
    transform: translateY(-10vh) rotate(0deg) scale(0.85);
    opacity: 0;
  }
  5% {
    opacity: var(--o, 0.2);
    transform: translateY(0vh) rotate(18deg) scale(1);
  }
  50% {
    opacity: var(--o, 0.2);
    transform: translateY(50vh) rotate(180deg) scale(1.05);
  }
  85% {
    opacity: var(--o, 0.2);
    transform: translateY(85vh) rotate(300deg) scale(0.95);
  }
  100% {
    transform: translateY(110vh) rotate(360deg) scale(0.85);
    opacity: 0;
  }
}

.page-title {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin: 0;
}
.page-subtitle {
  font-size: 14px;
  color: var(--color-muted-fg);
  font-weight: 500;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
}
.global-search-input { flex: 1; }
.search-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-muted-fg);
}
.back-link {
  color: var(--color-accent);
  cursor: pointer;
}
.back-link:hover { color: #7C3AED; }

/* 搜索结果 */
.search-list { display: flex; flex-direction: column; gap: 12px; }
.search-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  cursor: pointer;
  transition: all 0.2s var(--ease-bounce);
}
.search-item:hover {
  transform: translateX(4px);
  box-shadow: 6px 6px 0 var(--color-border);
}
.search-avatar {
  width: 52px; height: 52px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 3px 3px 0 var(--color-fg);
}
.search-info { flex-shrink: 0; width: 140px; }
.search-name {
  font-family: var(--font-heading);
  font-size: 16px; font-weight: 700;
  color: var(--color-fg);
  margin-bottom: 4px;
}
.search-meta {
  font-size: 12px;
  color: var(--color-muted-fg);
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-dist { color: #CBD5E1; }
.search-skills { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; min-width: 0; }

/* 控制栏 */
.control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  gap: 16px;
  flex-wrap: wrap;
}

/* 分段控制 */
.segment-control {
  display: flex;
  padding: 4px;
  background: var(--color-muted);
  border: var(--border-chunky);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-pop);
}
.segment-btn {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-sm);
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-bounce);
  background: transparent;
  color: var(--color-muted-fg);
}
.segment-btn.active {
  background: var(--color-accent);
  color: #fff;
  box-shadow: 3px 3px 0 var(--color-fg);
}
.segment-btn:not(.active):hover {
  color: var(--color-fg);
  background: rgba(139, 92, 246, 0.08);
}

/* 刷新按钮 */
.refresh-btn {
  background: var(--color-tertiary);
  color: var(--color-fg);
  display: flex;
  align-items: center;
  gap: 8px;
}
.refresh-icon { transition: transform 0.3s; }
.refresh-icon.spinning { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

/* 位置提示 */
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

/* 用户卡片 */
.user-card {
  cursor: pointer;
  transition: transform 0.3s var(--ease-bounce), box-shadow 0.3s var(--ease-bounce);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  padding: 0;
}
.user-card:hover {
  transform: translateY(-4px) rotate(-0.5deg);
  box-shadow: 8px 8px 0 var(--color-fg);
}

/* 顶部装饰条 */
.card-deco-bar {
  height: 6px;
  width: 100%;
  flex-shrink: 0;
}
.deco-0 { background: linear-gradient(90deg, var(--color-accent), #a78bfa); }
.deco-1 { background: linear-gradient(90deg, var(--color-secondary), #fb923c); }
.deco-2 { background: linear-gradient(90deg, var(--color-tertiary), #34d399); }

/* 卡片内容区 */
.card-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 16px 0;
}
.card-avatar-wrap {
  flex-shrink: 0;
  position: relative;
  width: 52px; height: 52px;
}
.card-avatar {
  width: 52px; height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 2.5px solid #fff;
  box-shadow:
    0 0 0 2px rgba(139, 92, 246, 0.25),
    0 4px 12px rgba(139, 92, 246, 0.15);
  background: #fff;
  display: block;
}
.avatar-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px dashed;
  opacity: 0.3;
  animation: ringRotate 12s linear infinite;
}
.ring-0 { border-color: var(--color-accent); }
.card:nth-child(3n+2) .card-avatar {
  box-shadow: 0 0 0 2px rgba(251, 146, 60, 0.25), 0 4px 12px rgba(251, 146, 60, 0.15);
}
.ring-1 { border-color: var(--color-secondary); }
.card:nth-child(3n+3) .card-avatar {
  box-shadow: 0 0 0 2px rgba(52, 211, 153, 0.25), 0 4px 12px rgba(52, 211, 153, 0.15);
}
.ring-2 { border-color: var(--color-tertiary); }
@keyframes ringRotate { to { transform: rotate(360deg); } }

.card-header-info { flex: 1; min-width: 0; padding-top: 2px; }
.card-name {
  font-family: var(--font-heading);
  font-size: 17px; font-weight: 700;
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
  font-size: 12px;
  color: var(--color-muted-fg);
  margin-top: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 技能标签 */
.card-skills {
  padding: 12px 16px 0;
}
.skill-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}
.skill-pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: 3px 10px 3px 8px;
  font-size: 11px;
  font-weight: 700;
  font-family: var(--font-heading);
  letter-spacing: 1px;
  margin-top: 3px;
  border-radius: 6px;
  line-height: 1;
  position: relative;
  transition: all 0.3s ease;
}
.skill-pill::before {
  content: '';
  position: absolute;
  left: 0; top: 2px; bottom: 2px;
  width: 3px;
  border-radius: 3px;
  transition: all 0.3s ease;
}
.can-pill {
  color: #059669;
  background: linear-gradient(90deg, rgba(52, 211, 153, 0.08), transparent);
}
.can-pill::before {
  background: linear-gradient(180deg, #34d399, #10b981);
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.3);
}
.want-pill {
  color: #db2777;
  background: linear-gradient(90deg, rgba(244, 114, 182, 0.08), transparent);
}
.want-pill::before {
  background: linear-gradient(180deg, #f472b6, #ec4899);
  box-shadow: 0 0 6px rgba(236, 72, 153, 0.3);
}
.more-tag {
  background: var(--color-muted) !important;
  color: var(--color-muted-fg) !important;
  border: 2px dashed var(--color-border) !important;
  font-weight: 700;
}

/* AI 建议 */
.ai-suggestion {
  display: flex;
  gap: 10px;
  margin: 12px 16px;
  padding: 10px 12px;
  border-left: 4px solid var(--color-accent);
  background: rgba(139, 92, 246, 0.04);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  position: relative;
}
.ai-1 { border-left-color: var(--color-secondary); background: rgba(251, 146, 60, 0.04); }
.ai-2 { border-left-color: var(--color-tertiary); background: rgba(52, 211, 153, 0.04); }
.ai-1 .ai-suggestion-label { color: var(--color-secondary); }
.ai-2 .ai-suggestion-label { color: var(--color-tertiary); }
.ai-suggestion-icon {
  position: absolute;
  left: -10px; top: -6px;
  width: 18px; height: 18px;
  font-size: 12px;
  color: var(--color-accent);
  background: var(--bg-card);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.ai-suggestion-content { flex: 1; }
.ai-suggestion-label {
  font-family: var(--font-heading);
  font-size: 10px; font-weight: 700;
  color: var(--color-accent);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 4px;
}
.ai-suggestion-text {
  font-size: 13px;
  color: var(--color-muted-fg);
  line-height: 1.5;
  font-style: italic;
  margin: 0;
}
.ai-suggestion-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-muted-fg);
}
.ai-suggestion-loading .loading-dots span {
  background: var(--color-accent);
}

/* 匹配度 */
.match-score-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-muted);
  margin: auto 16px 16px;
  cursor: pointer;
  transition: all 0.2s var(--ease-bounce);
  min-height: 40px;
  justify-content: center;
}
.match-score-bar:hover { background: #E2E8F0; }

.score-prompt {
  font-family: var(--font-heading);
  font-size: 13px; font-weight: 600;
  color: var(--color-accent);
}

.score-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-muted-fg);
}
.loading-dots { display: flex; gap: 3px; }
.loading-dots span {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: var(--color-accent);
  animation: dotPulse 1.2s ease-in-out infinite;
}
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1.2); }
}

.score-result {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  animation: scoreReveal 0.5s var(--ease-bounce);
}
@keyframes scoreReveal {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
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
.score-value {
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
  .geo-grid-3 { grid-template-columns: 1fr; }
  .control-bar { flex-direction: column; }
  .segment-control { width: 100%; }
  .segment-btn { flex: 1; text-align: center; }
  .refresh-btn { width: 100%; justify-content: center; }
}

</style>
