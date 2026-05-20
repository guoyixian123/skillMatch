<template>
  <div class="page-container">
    <header class="page-header flex-between">
      <div>
        <h1 class="page-title">💬 社区</h1>
        <p class="page-subtitle">技能搭子交流广场</p>
      </div>
      <button class="brutal-btn primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon> 发帖
      </button>
    </header>

    <!-- Filter -->
    <div class="filter-bar brutal-card accent-yellow" style="margin-bottom:20px;">
      <div class="filter-row">
        <el-input
          v-model="keyword"
          placeholder="搜索帖子..."
          :prefix-icon="Search"
          clearable
          class="filter-search"
          @keyup.enter="fetchPosts"
        />
        <el-select v-model="sort" class="filter-select">
          <el-option label="最新" value="latest" />
          <el-option label="最热" value="hot" />
        </el-select>
        <button class="brutal-btn primary small" @click="fetchPosts">筛选</button>
      </div>
    </div>

    <!-- Posts -->
    <div v-if="loading" class="loading-block">⚡ 加载中...</div>

    <div v-else-if="posts.length === 0" class="empty-block">
      <div class="icon">📝</div>
      <div style="font-weight:800;font-size:18px;">暂无帖子</div>
      <div style="color:#888;">发布第一个帖子吧</div>
    </div>

    <div v-else class="post-list">
      <div
        v-for="post in posts"
        :key="post.id"
        class="post-card brutal-card accent-blue"
        @click="$router.push(`/community/${post.id}`)"
      >
        <div class="post-author">
          <img
            :src="post.author?.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
            class="brutal-avatar"
          />
          <span class="post-author-name">{{ post.author?.nickname }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>

        <h2 class="post-title">{{ post.title }}</h2>
        <p class="post-body">{{ post.body?.slice(0, 150) }}{{ post.body?.length > 150 ? '...' : '' }}</p>

        <div class="post-tags flex-wrap" v-if="post.tags?.length">
          <span v-for="tag in post.tags" :key="tag" class="brutal-tag">{{ tag }}</span>
        </div>

        <div class="post-meta">
          <span class="meta-item">
            <el-icon><Star /></el-icon> {{ post.likeCount || 0 }}
          </span>
          <span class="meta-item">
            <el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}
          </span>
        </div>
      </div>
    </div>

    <div class="flex-center" style="margin-top:32px;" v-if="total > size">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="(p) => { page = p; fetchPosts(); }"
      />
    </div>

    <!-- Create Post Dialog -->
    <el-dialog v-model="showCreateDialog" title="发布帖子" width="560px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="帖子标题" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="body">
          <el-input
            v-model="createForm.body"
            type="textarea"
            :rows="5"
            placeholder="分享你的想法..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签 (逗号分隔, 最多5个)">
          <el-input v-model="createForm.tagsStr" placeholder="Python, 找搭子, 刷题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="brutal-btn outline small" @click="showCreateDialog = false">取消</button>
        <button class="brutal-btn primary small" @click="handleCreate" :disabled="creating">
          {{ creating ? '发布中...' : '发布' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Star, ChatDotRound } from '@element-plus/icons-vue'
import { getPosts, createPost } from '@/api/community'

const router = useRouter()
const loading = ref(false)
const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const sort = ref('latest')

const showCreateDialog = ref(false)
const createFormRef = ref(null)
const creating = ref(false)
const createForm = ref({
  title: '',
  body: '',
  tagsStr: '',
})
const createRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 64, message: '1-64字符', trigger: 'blur' },
  ],
  body: [
    { required: true, message: '请输入正文', trigger: 'blur' },
    { min: 1, max: 5000, message: '1-5000字符', trigger: 'blur' },
  ],
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

async function fetchPosts() {
  loading.value = true
  try {
    const res = await getPosts({
      sort: sort.value,
      keyword: keyword.value,
      page: page.value,
      size: size.value,
    })
    posts.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* API may not be implemented yet */ } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    const tags = createForm.value.tagsStr
      .split(',')
      .map(t => t.trim())
      .filter(Boolean)
      .slice(0, 5)
    await createPost({
      title: createForm.value.title,
      body: createForm.value.body,
      tags,
    })
    ElMessage.success('发布成功')
    showCreateDialog.value = false
    createForm.value = { title: '', body: '', tagsStr: '' }
    fetchPosts()
  } catch { /* handled */ } finally {
    creating.value = false
  }
}

onMounted(fetchPosts)
</script>

<style scoped>
.filter-bar { padding: 16px; }
.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.filter-search { flex: 1; }
.filter-select { width: 120px; flex-shrink: 0; }

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.post-card {
  cursor: pointer;
}
.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.post-author-name {
  font-weight: 700;
  font-size: 14px;
}
.post-time {
  font-size: 12px;
  color: #aaa;
  margin-left: auto;
}
.post-title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 8px;
}
.post-body {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 10px;
}
.post-tags { margin-bottom: 10px; }
.post-meta {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 2px solid #eee;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #888;
}
</style>
