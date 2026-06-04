<template>
  <div class="page-container">
    <header class="page-header flex-between">
      <div>
        <h1 class="page-title"><el-icon><ChatDotRound /></el-icon> 社区</h1>
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
    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <div v-else-if="posts.length === 0" class="empty-block">
      <div class="icon"><el-icon :size="48"><Document /></el-icon></div>
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
            :src="post.author?.avatarUrl || getDefaultAvatar(post.author?.id || post.author?.name)"
            class="brutal-avatar"
            @click.stop="$router.push(`/user/${post.author?.id}`)"
          />
          <span class="post-author-name" @click.stop="$router.push(`/user/${post.author?.id}`)">{{ post.author?.name }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>

        <h2 class="post-title">{{ post.title }}</h2>
        <p class="post-body">{{ post.body?.slice(0, 150) }}{{ post.body?.length > 150 ? '...' : '' }}</p>

        <div class="post-tags flex-wrap" v-if="post.tags?.length">
          <span v-for="tag in post.tags" :key="tag" class="brutal-tag">{{ tag }}</span>
        </div>

        <div class="post-meta">
          <button
            class="card-like-btn"
            :class="{ liked: post.isLiked, animating: post._animating }"
            @click.stop="handleCardLike(post)"
          >
            <span class="like-icon-wrap">
              <el-icon :size="16"><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
              <span v-if="post._showPlus" class="float-plus">+1</span>
            </span>
            <span class="like-label">{{ post.isLiked ? '已赞' : '点赞' }}</span>
            <span class="like-count">{{ formatCount(post.likeCount) }}</span>
          </button>
          <span class="meta-item comment-meta">
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
    <el-dialog v-model="showCreateDialog" title="发布帖子" width="560px" @open="() => createFormRef?.clearValidate()">
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
        <el-form-item label="标签 (可选, 逗号分隔, 最多5个)">
          <el-input v-model="createForm.tagsStr" placeholder="输入标签, 如: #Python, #找搭子" maxlength="60" show-word-limit />
          <div class="tag-hint">标签以 # 开头, 长度 2-10, 只能包含中文/字母/数字</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="brutal-btn outline small" @click="handleCancelCreate">取消</button>
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
import { Plus, Search, Star, StarFilled, ChatDotRound, Loading, Document } from '@element-plus/icons-vue'
import { getDefaultAvatar } from '@/utils/avatar'
import { getPosts, createPost, togglePostLike, unlikePost } from '@/api/community'

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

function formatCount(n) {
  if (!n) return '0'
  return n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : String(n)
}

async function handleCardLike(post) {
  const wasLiked = post.isLiked

  // 点赞时触发动画
  if (!wasLiked) {
    post._animating = true
    post._showPlus = true
    setTimeout(() => { post._animating = false }, 500)
    setTimeout(() => { post._showPlus = false }, 800)
  }

  // optimistic update
  post.isLiked = !wasLiked
  post.likeCount = Math.max(0, (post.likeCount || 0) + (wasLiked ? -1 : 1))

  try {
    if (wasLiked) {
      await unlikePost(post.id)
    } else {
      const res = await togglePostLike(post.id)
      post.likeCount = res.data?.likeCount ?? post.likeCount
    }
  } catch {
    // rollback
    post.isLiked = wasLiked
    post.likeCount = Math.max(0, (post.likeCount || 0) + (wasLiked ? 1 : -1))
  }
}

function handleCancelCreate() {
  showCreateDialog.value = false
  createForm.value = { title: '', body: '', tagsStr: '' }
  createFormRef.value?.clearValidate()
}

async function handleCreate() {
  // 兜底验证：确保必填字段不为空
  if (!createForm.value.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!createForm.value.body?.trim()) {
    ElMessage.warning('请输入正文')
    return
  }
  // Element Plus 表单验证
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    const tagRe = /^#[一-龥a-zA-Z0-9]{1,9}$/
    const tagsStr = createForm.value.tagsStr.trim()
    let tags = []
    if (tagsStr) {
      tags = tagsStr.split(',').map(t => t.trim()).filter(Boolean).slice(0, 5)
      const invalid = tags.filter(t => !tagRe.test(t))
      if (invalid.length) {
        ElMessage.warning('标签格式错误: ' + invalid.join(', '))
        return
      }
    }
    const res = await createPost({
      title: createForm.value.title,
      body: createForm.value.body,
      tags,
    })
    ElMessage.success(res.message || '发布成功')
    showCreateDialog.value = false
    createForm.value = { title: '', body: '', tagsStr: '' }
    fetchPosts()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '发布失败')
  } finally {
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
  cursor: pointer;
}
.post-author-name:hover { text-decoration: underline; }
.post-author .brutal-avatar { cursor: pointer; }
.post-author .brutal-avatar:hover { opacity: 0.85; }
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
  gap: 16px;
  align-items: center;
  padding-top: 12px;
  border-top: 2px solid #eee;
}

/* 评论数 */
.comment-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #888;
  margin-left: auto;
}

/* 标签提示 */
.tag-hint {
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
  line-height: 1.4;
}

/* ========= 卡片点赞按钮 - 粗野主义风格 ========= */
.card-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px 6px 10px;
  border: 2px solid #ddd;
  border-radius: 99px;
  background: #fafafa;
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  color: #888;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
  position: relative;
  overflow: visible;
}

.card-like-btn:hover {
  border-color: #FFD700;
  background: #FFFBE6;
  color: #E6A800;
  transform: translateY(-1px);
  box-shadow: 0 2px 0 rgba(0,0,0,0.1);
}

.card-like-btn:active {
  transform: scale(0.96);
}

/* 已赞态 */
.card-like-btn.liked {
  border-color: #1A1A1A;
  background: linear-gradient(135deg, #F5A623, #FFD700);
  color: #fff;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.15), 0 0 12px rgba(245,166,35,0.3);
}

.card-like-btn.liked .el-icon {
  color: #FFE4B5;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}

.card-like-btn.liked:hover {
  background: linear-gradient(135deg, #FFB830, #FFE44D);
  border-color: #1A1A1A;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.2), 0 0 16px rgba(245,166,35,0.45);
}

/* 图标容器 */
.like-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* +1 浮动动画 */
.float-plus {
  position: absolute;
  top: -8px;
  right: -14px;
  font-size: 12px;
  font-weight: 900;
  color: #FFD700;
  pointer-events: none;
  animation: floatUp 0.8s ease-out forwards;
  text-shadow: 1px 1px 0 rgba(0,0,0,0.2);
}

@keyframes floatUp {
  0%   { opacity: 1; transform: translateY(0) scale(1); }
  40%  { opacity: 1; transform: translateY(-14px) scale(1.25); }
  100% { opacity: 0; transform: translateY(-24px) scale(0.8); }
}

.like-label {
  font-weight: 800;
  font-size: 12px;
}

.like-count {
  font-weight: 900;
  font-size: 13px;
  min-width: 20px;
  text-align: center;
  background: rgba(0,0,0,0.06);
  border-radius: 10px;
  padding: 1px 7px;
}

.card-like-btn.liked .like-count {
  background: rgba(255,255,255,0.25);
}

/* 点击回弹动画 */
.card-like-btn.animating .like-icon-wrap {
  animation: likePop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes likePop {
  0%   { transform: scale(1); }
  25%  { transform: scale(1.45); }
  50%  { transform: scale(0.8); }
  75%  { transform: scale(1.15); }
  100% { transform: scale(1); }
}
</style>
