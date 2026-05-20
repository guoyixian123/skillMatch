<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title">🔍 发现技能搭子</h1>
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
        <el-select v-model="filter" class="filter-select">
          <el-option label="全部" value="all" />
          <el-option label="技能交换" value="exchange" />
          <el-option label="找搭子" value="partner" />
          <el-option label="最近活跃" value="active" />
        </el-select>
        <el-select v-model="sort" class="filter-select">
          <el-option label="匹配度" value="score" />
          <el-option label="距离近" value="dist" />
          <el-option label="活跃度" value="active" />
        </el-select>
        <button class="brutal-btn primary small" @click="fetchUsers">筛选</button>
      </div>
      <div class="filter-tags flex-wrap" v-if="activeTags.length">
        <span v-for="tag in activeTags" :key="tag" class="brutal-tag">
          {{ tag }}
          <span style="cursor:pointer;margin-left:4px;" @click="clearTag(tag)">×</span>
        </span>
      </div>
    </div>

    <!-- Results Grid -->
    <div v-if="loading" class="loading-block">⚡ 正在匹配中...</div>

    <div v-else-if="users.length === 0" class="empty-block">
      <div class="icon">🔍</div>
      <div style="font-weight:800;font-size:18px;">暂无匹配用户</div>
      <div style="color:#888;margin-top:4px;">试试扩大搜索范围或添加更多技能标签</div>
    </div>

    <div v-else class="brutal-grid-3">
      <div
        v-for="user in users"
        :key="user.id"
        class="user-card brutal-card accent-blue"
        @click="showUserCard(user)"
      >
        <div class="card-cover pop-dots"></div>
        <div class="card-body">
          <img
            :src="user.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
            class="brutal-avatar lg card-avatar"
          />
          <h3 class="card-name">{{ user.nickname }}</h3>
          <div class="card-distance" v-if="user.distance">
            <el-icon><Location /></el-icon>
            {{ user.distance }}
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
              <div class="match-fill" :style="{ width: user.matchScore + '%' }"></div>
            </div>
            <span class="match-num">{{ user.matchScore }}%</span>
          </div>
        </div>
      </div>
    </div>

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
    <el-dialog v-model="cardVisible" :title="cardUser?.nickname" width="420px" destroy-on-close>
      <div v-if="cardUser" class="user-card-detail">
        <div class="detail-header">
          <img
            :src="cardUser.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
            class="brutal-avatar xl"
          />
          <div>
            <h2 style="font-weight:900;">{{ cardUser.nickname }}</h2>
            <div v-if="cardUser.distance" style="color:#888;">
              <el-icon><Location /></el-icon> {{ cardUser.distance }}
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
            <div class="match-fill" :style="{ width: cardUser.matchScore + '%' }"></div>
          </div>
          <span class="match-num">{{ cardUser.matchScore }}%</span>
        </div>

        <div class="detail-actions" style="margin-top:20px;display:flex;gap:12px;">
          <button class="brutal-btn primary small" @click="sendRequest(cardUser)">
            💬 发起交换
          </button>
          <button class="brutal-btn outline small" @click="$router.push(`/user/${cardUser.id}`)">
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Location } from '@element-plus/icons-vue'
import { getRecommendedUsers } from '@/api/matching'
import { createRequest } from '@/api/notification'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const keyword = ref('')
const filter = ref('all')
const sort = ref('score')

const cardVisible = ref(false)
const cardUser = ref(null)

const requestVisible = ref(false)
const requestTarget = ref(null)
const requestReason = ref('')

const activeTags = computed(() => {
  const tags = []
  if (filter.value !== 'all') tags.push(filter.value === 'exchange' ? '技能交换' : filter.value === 'partner' ? '找搭子' : '最近活跃')
  if (sort.value !== 'score') tags.push(sort.value === 'dist' ? '距离近' : '活跃度')
  return tags
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getRecommendedUsers({
      filter: filter.value,
      sort: sort.value,
      keyword: keyword.value,
      page: page.value,
      size: size.value,
    })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch {
    // API may not be implemented yet, show mock
  } finally {
    loading.value = false
  }
}

function clearTag(tag) {
  if (tag === '技能交换' || tag === '找搭子' || tag === '最近活跃') filter.value = 'all'
  if (tag === '距离近' || tag === '活跃度') sort.value = 'score'
  fetchUsers()
}

function showUserCard(user) {
  cardUser.value = user
  cardVisible.value = true
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
    await createRequest({ toUserId: requestTarget.value.id, reason: requestReason.value })
    ElMessage.success('交换请求已发送')
    requestVisible.value = false
  } catch { /* handled */ }
}

onMounted(fetchUsers)
</script>

<style scoped>
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
.card-distance {
  font-size: 13px;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
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
  background: var(--color-yellow);
  transition: width 0.3s;
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
