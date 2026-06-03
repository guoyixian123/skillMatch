<template>
  <div class="page-container">
    <header class="page-header flex-between">
      <div>
        <h1 class="page-title"><i class="pi pi-comments"></i> 社区</h1>
        <p class="page-subtitle">技能搭子交流广场</p>
      </div>
      <button class="brutal-btn primary" @click="showCreateDialog = true">
        <i class="pi pi-plus"></i> 发帖
      </button>
    </header>

    <!-- Filter -->
    <div class="filter-bar brutal-card accent-yellow" style="margin-bottom:20px;">
      <div class="filter-row">
        <div class="filter-search-wrap">
          <i class="pi pi-search filter-search-icon"></i>
          <input
            v-model="keyword"
            class="brutal-input filter-search-input"
            placeholder="搜索帖子..."
            @keyup.enter="fetchPosts"
          />
        </div>
        <Select
          v-model="sort"
          :options="sortOptions"
          option-label="label"
          option-value="value"
          class="filter-select"
        />
        <button class="brutal-btn primary small" @click="fetchPosts">筛选</button>
      </div>
    </div>

    <!-- Posts -->
    <div v-if="loading" class="loading-block"><i class="pi pi-spinner pi-spin"></i> 加载中...</div>

    <div v-else-if="posts.length === 0" class="empty-block">
      <div class="icon"><i class="pi pi-file" style="font-size:48px;"></i></div>
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
              <i class="pi" :class="post.isLiked ? 'pi-star-fill' : 'pi-star'" style="font-size:16px;"></i>
              <span v-if="post._showPlus" class="float-plus">+1</span>
            </span>
            <span class="like-label">{{ post.isLiked ? '已赞' : '点赞' }}</span>
            <span class="like-count">{{ formatCount(post.likeCount) }}</span>
          </button>
          <span class="meta-item comment-meta">
            <i class="pi pi-comments"></i> {{ post.commentCount || 0 }}
          </span>
        </div>
      </div>
    </div>

    <div class="flex-center" style="margin-top:32px;" v-if="total > size">
      <Paginator
        v-model:first="first"
        :rows="size"
        :total-records="total"
        :rows-per-page-options="[10]"
        template="PrevPageLink PageLinks NextPageLink"
        @page="onPageChange"
      />
    </div>

    <!-- Create Post Dialog -->
    <Dialog v-model:visible="showCreateDialog" header="发布帖子" :modal="true" :style="{ width: '560px' }">
      <form @submit.prevent="handleCreate" class="create-form">
        <div class="form-field">
          <label class="field-label">标题 <span class="required">*</span></label>
          <input v-model="createForm.title" class="brutal-input" placeholder="帖子标题" maxlength="64" />
          <span v-if="createErrors.title" class="field-error">{{ createErrors.title }}</span>
        </div>
        <div class="form-field">
          <label class="field-label">正文 <span class="required">*</span></label>
          <textarea v-model="createForm.body" class="brutal-input" rows="5" placeholder="分享你的想法..." maxlength="5000"></textarea>
          <span v-if="createErrors.body" class="field-error">{{ createErrors.body }}</span>
          <div class="field-count">{{ createForm.body.length }}/5000</div>
        </div>
        <div class="form-field">
          <label class="field-label">标签 (可选, 逗号分隔, 最多5个)</label>
          <input v-model="createForm.tagsStr" class="brutal-input" placeholder="输入标签, 如: #Python, #找搭子" maxlength="60" />
          <div class="tag-hint">标签以 # 开头, 长度 2-10, 只能包含中文/字母/数字</div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button type="button" class="brutal-btn outline small" @click="handleCancelCreate">取消</button>
          <button type="submit" class="brutal-btn primary small" :disabled="creating">
            {{ creating ? '发布中...' : '发布' }}
          </button>
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Paginator from 'primevue/paginator'
import { getDefaultAvatar } from '@/utils/avatar'
import { getPosts, createPost, togglePostLike, unlikePost } from '@/api/community'

const router = useRouter()
const toast = useToast()
const loading = ref(false)
const posts = ref([])
const total = ref(0)
const page = ref(1)
const first = ref(0)
const size = ref(10)

const likeTimers = []
const keyword = ref('')
const sort = ref('latest')

const sortOptions = [
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'hot' },
]

const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = ref({ title: '', body: '', tagsStr: '' })
const createErrors = ref({ title: '', body: '' })

function onPageChange(event) {
  page.value = event.page + 1
  first.value = event.first
  fetchPosts()
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
    const res = await getPosts({ sort: sort.value, keyword: keyword.value, page: page.value, size: size.value })
    posts.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch { /* handled */ } finally { loading.value = false }
}

function formatCount(n) {
  if (!n) return '0'
  return n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : String(n)
}

async function handleCardLike(post) {
  const wasLiked = post.isLiked
  if (!wasLiked) {
    post._animating = true
    post._showPlus = true
    likeTimers.push(setTimeout(() => { post._animating = false }, 500))
    likeTimers.push(setTimeout(() => { post._showPlus = false }, 800))
  }
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
		// 错误已在请求拦截器中提示
    post.isLiked = wasLiked
    post.likeCount = Math.max(0, (post.likeCount || 0) + (wasLiked ? 1 : -1))
  }
}

function handleCancelCreate() {
  showCreateDialog.value = false
  createForm.value = { title: '', body: '', tagsStr: '' }
  createErrors.value = { title: '', body: '' }
}

async function handleCreate() {
  createErrors.value = { title: '', body: '' }
  let valid = true
  if (!createForm.value.title?.trim()) { createErrors.value.title = '请输入标题'; valid = false }
  if (!createForm.value.body?.trim()) { createErrors.value.body = '请输入正文'; valid = false }
  if (!valid) return

  creating.value = true
  try {
    const tagRe = /^#[一-龥a-zA-Z0-9]{1,9}$/
    const tagsStr = createForm.value.tagsStr.trim()
    let tags = []
    if (tagsStr) {
      tags = tagsStr.split(',').map(t => t.trim()).filter(Boolean).slice(0, 5)
      const invalid = tags.filter(t => !tagRe.test(t))
      if (invalid.length) {
        toast.add({ severity: 'warn', summary: '提示', detail: '标签格式错误: ' + invalid.join(', '), life: 3000 })
        return
      }
    }
    const res = await createPost({ title: createForm.value.title, body: createForm.value.body, tags })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '发布成功', life: 3000 })
    showCreateDialog.value = false
    createForm.value = { title: '', body: '', tagsStr: '' }
    fetchPosts()
  } catch {
		// 错误已在请求拦截器中提示
  } finally { creating.value = false }
}

onMounted(fetchPosts)
onUnmounted(() => likeTimers.forEach(clearTimeout))
</script>

<style scoped>
.filter-bar { padding: 16px; }
.filter-row { display: flex; gap: 12px; align-items: center; }
.filter-search-wrap { flex: 1; position: relative; }
.filter-search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #aaa; z-index: 1; font-size: 16px; }
.filter-search-input { padding-left: 36px !important; }
.filter-select { width: 120px; flex-shrink: 0; }

.post-list { display: flex; flex-direction: column; gap: 16px; }
.post-card { cursor: pointer; }
.post-author { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.post-author-name { font-weight: 700; font-size: 14px; cursor: pointer; }
.post-author-name:hover { text-decoration: underline; }
.post-author .brutal-avatar { cursor: pointer; }
.post-author .brutal-avatar:hover { opacity: 0.85; }
.post-time { font-size: 12px; color: #aaa; margin-left: auto; }
.post-title { font-size: 18px; font-weight: 900; margin-bottom: 8px; }
.post-body { font-size: 14px; color: #555; line-height: 1.6; margin-bottom: 10px; }
.post-tags { margin-bottom: 10px; }
.post-meta { display: flex; gap: 16px; align-items: center; padding-top: 12px; border-top: 2px solid #eee; }

.comment-meta { display: flex; align-items: center; gap: 4px; font-size: 13px; font-weight: 700; color: #888; margin-left: auto; }
.tag-hint { font-size: 12px; color: #aaa; margin-top: 4px; }

/* Create form */
.create-form { display: flex; flex-direction: column; gap: 14px; }
.form-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #555; }
.field-label .required { color: var(--color-pink); }
.field-error { font-size: 12px; font-weight: 700; color: var(--color-pink); }
.field-count { text-align: right; font-size: 12px; color: #aaa; }

/* 点赞按钮 */
.card-like-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 14px 6px 10px; border: 2px solid #ddd; border-radius: 99px;
  background: #fafafa; cursor: pointer; font-family: inherit;
  font-size: 13px; font-weight: 700; color: #888;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1); overflow: visible;
}
.card-like-btn:hover { border-color: #FFD700; background: #FFFBE6; color: #E6A800; transform: translateY(-1px); box-shadow: 0 2px 0 rgba(0,0,0,0.1); }
.card-like-btn:active { transform: scale(0.96); }
.card-like-btn.liked {
  border-color: #1A1A1A; background: linear-gradient(135deg, #F5A623, #FFD700);
  color: #fff; box-shadow: 2px 2px 0 rgba(0,0,0,0.15), 0 0 12px rgba(245,166,35,0.3);
}
.card-like-btn.liked:hover { background: linear-gradient(135deg, #FFB830, #FFE44D); }
.like-icon-wrap { position: relative; display: inline-flex; align-items: center; }
.float-plus {
  position: absolute; top: -8px; right: -14px; font-size: 12px; font-weight: 900;
  color: #FFD700; pointer-events: none; animation: floatUp 0.8s ease-out forwards;
  text-shadow: 1px 1px 0 rgba(0,0,0,0.2);
}
@keyframes floatUp {
  0% { opacity: 1; transform: translateY(0) scale(1); }
  40% { opacity: 1; transform: translateY(-14px) scale(1.25); }
  100% { opacity: 0; transform: translateY(-24px) scale(0.8); }
}
.like-label { font-weight: 800; font-size: 12px; }
.like-count { font-weight: 900; font-size: 13px; min-width: 20px; text-align: center; background: rgba(0,0,0,0.06); border-radius: 10px; padding: 1px 7px; }
.card-like-btn.liked .like-count { background: rgba(255,255,255,0.25); }
.card-like-btn.animating .like-icon-wrap { animation: likePop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes likePop {
  0% { transform: scale(1); } 25% { transform: scale(1.45); }
  50% { transform: scale(0.8); } 75% { transform: scale(1.15); } 100% { transform: scale(1); }
}
</style>
