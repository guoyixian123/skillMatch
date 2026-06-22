<template>
  <div class="page-container">
    <button class="geo-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      <el-icon><ArrowLeft /></el-icon> 返回社区
    </button>

    <div v-if="loading" class="loading-block"><el-icon class="is-loading"><Loading /></el-icon> 加载中...</div>

    <template v-if="post">
      <!-- Post Content -->
      <div class="geo-card accent-violet">
        <div class="post-author">
          <img
            :src="post.author?.avatarUrl || getDefaultAvatar(post.author?.id || post.author?.name)"
            class="geo-avatar"
            style="cursor:pointer;"
            @click="$router.push(`/user/${post.author?.id}`)"
          />
          <div>
            <div class="post-author-name link" @click="$router.push(`/user/${post.author?.id}`)">{{ post.author?.name }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-body-full">{{ post.body }}</div>

        <div class="post-tags flex-wrap" v-if="post.tags?.length" style="margin:16px 0;">
          <span v-for="tag in post.tags" :key="tag" class="geo-tag">{{ tag }}</span>
        </div>

        <div class="post-actions">
          <button
            class="geo-btn small like-btn"
            :class="{ liked: post.isLiked, animating: likeAnimating }"
            @click="handleToggleLike"
          >
            <span class="like-icon-wrap">
              <el-icon :size="20" :color="post.isLiked ? '#FCE7F3' : ''"><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
              <span v-if="showPlus" class="float-plus">+1</span>
              <span v-if="showParticles" class="particles">
                <i v-for="n in 6" :key="n" class="particle" :style="{ '--i': n }"></i>
              </span>
            </span>
            <span class="like-text">{{ post.isLiked ? '已赞' : '点赞' }}</span>
            <span class="like-count">{{ post.likeCount || 0 }}</span>
          </button>
          <span style="color:var(--color-muted-fg);font-weight:600;"><el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }} 条评论</span>
          <button v-if="isPostAuthor" class="geo-btn danger small" style="margin-left:auto;" @click="handleDeletePost" :disabled="deletingPost">
            <el-icon><Delete /></el-icon> {{ deletingPost ? '删除中...' : '删除帖子' }}
          </button>
        </div>
      </div>

      <!-- Comments -->
      <div style="margin-top:24px;">
        <div class="section-title">
          <span class="dot" style="background:var(--color-tertiary)"></span>
          评论 ({{ comments.length }})
        </div>

        <!-- Comment input -->
        <div class="geo-card" style="margin-bottom:16px;padding:16px;">
          <div style="display:flex;gap:12px;">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              placeholder="写下你的评论..."
              maxlength="500"
              show-word-limit
            />
            <button class="geo-btn primary small" style="flex-shrink:0;align-self:flex-end;" @click="handleComment" :disabled="commenting">
              {{ commenting ? '发送中' : '发送' }}
            </button>
          </div>
        </div>

        <!-- Comments list -->
        <div v-if="comments.length === 0" style="text-align:center;padding:40px;color:var(--color-muted-fg);font-weight:500;">
          暂无评论，来说两句吧
        </div>

        <div v-for="comment in comments" :key="comment.id" class="comment-item geo-card" style="margin-bottom:8px;padding:16px;">
          <div class="comment-header">
            <img
              :src="comment.user?.avatarUrl || getDefaultAvatar(comment.user?.id || comment.user?.name)"
              class="geo-avatar"
              style="width:32px;height:32px;"
            />
            <span class="comment-author">{{ comment.user?.name }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            <button
              v-if="canDeleteComment(comment)"
              class="geo-btn danger small"
              style="margin-left:auto;padding:4px 10px;font-size:11px;"
              @click="handleDeleteComment(comment)"
              :disabled="deletingCommentId === comment.id"
            >
              {{ deletingCommentId === comment.id ? '...' : '删除' }}
            </button>
          </div>
          <div class="comment-body">{{ comment.body }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ElMessage } from '@/utils/message'
import { Star, StarFilled, Delete, ArrowLeft, Loading, ChatDotRound } from '@element-plus/icons-vue'
import { getPostDetail, togglePostLike, unlikePost, getComments, createComment, deletePost, deleteComment } from '@/api/community'
import { getDefaultAvatar } from '@/utils/avatar'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const postId = route.params.postId

const loading = ref(true)
const post = ref(null)
const comments = ref([])
const newComment = ref('')
const commenting = ref(false)
const deletingPost = ref(false)
const deletingCommentId = ref(null)
const likeAnimating = ref(false)
const showPlus = ref(false)
const showParticles = ref(false)

const currentUserId = computed(() => String(authStore.user?.userId || ''))
const isPostAuthor = computed(() => currentUserId.value && currentUserId.value === String(post.value?.author?.id))

function canDeleteComment(comment) {
  if (!currentUserId.value) return false
  return currentUserId.value === String(comment.user?.id) || isPostAuthor.value
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

async function fetchPost() {
  loading.value = true
  try {
    const [postRes, commentsRes] = await Promise.all([
      getPostDetail(postId),
      getComments(postId, { page: 1, size: 50 }),
    ])
    post.value = postRes.data
    comments.value = commentsRes.data?.list || []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleToggleLike() {
  if (!post.value) return
  const wasLiked = post.value.isLiked

  likeAnimating.value = true
  setTimeout(() => likeAnimating.value = false, 500)

  if (!wasLiked) {
    showPlus.value = true
    showParticles.value = true
    setTimeout(() => { showPlus.value = false }, 800)
    setTimeout(() => { showParticles.value = false }, 600)
  }

  post.value.isLiked = !wasLiked
  post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (wasLiked ? -1 : 1))

  try {
    if (wasLiked) {
      const res = await unlikePost(postId)
      ElMessage.success(res.message || '已取消点赞')
    } else {
      const res = await togglePostLike(postId)
      post.value.likeCount = res.data?.likeCount ?? post.value.likeCount
      ElMessage.success(res.message || '点赞成功')
    }
  } catch {
    post.value.isLiked = wasLiked
    post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (wasLiked ? 1 : -1))
    ElMessage.warning('操作失败，请重试')
  }
}

async function handleComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    const createRes = await createComment(postId, newComment.value.trim())
    ElMessage.success(createRes.message || '评论成功')
    newComment.value = ''
    const commentRes = await getComments(postId, { page: 1, size: 50 })
    comments.value = commentRes.data?.list || []
    if (post.value) post.value.commentCount = (post.value.commentCount || 0) + 1
  } catch { /* handled */ } finally {
    commenting.value = false
  }
}

async function handleDeletePost() {
  try {
    await ElMessageBox.confirm('确定要删除这篇帖子吗？删除后不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: false,
    })
  } catch {
    return
  }
  deletingPost.value = true
  try {
    const res = await deletePost(postId)
    ElMessage.success(res.message || '帖子已删除')
    router.replace('/community')
  } catch { /* handled */ } finally {
    deletingPost.value = false
  }
}

async function handleDeleteComment(comment) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: false,
    })
  } catch {
    return
  }
  deletingCommentId.value = comment.id
  try {
    const res = await deleteComment(postId, comment.id)
    ElMessage.success(res.message || '评论已删除')
    comments.value = comments.value.filter(c => c.id !== comment.id)
    if (post.value) post.value.commentCount = Math.max(0, (post.value.commentCount || 1) - 1)
  } catch { /* handled */ } finally {
    deletingCommentId.value = null
  }
}

onMounted(fetchPost)
</script>

<style scoped>
.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.post-author-name {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
  color: var(--color-fg);
}
.post-author-name.link { cursor: pointer; }
.post-author-name.link:hover { text-decoration: underline; }
.post-time { font-size: 12px; color: var(--color-muted-fg); }
.post-title {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 12px;
  color: var(--color-fg);
}
.post-body-full {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-fg);
  white-space: pre-wrap;
}
.post-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 2px solid var(--color-border);
}

/* Like button */
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px 6px 12px;
  border-radius: var(--radius-full);
  transition: all 0.3s var(--ease-bounce);
  user-select: none;
  position: relative;
  overflow: visible;
}
.like-btn:hover {
  border-color: var(--color-tertiary);
  background: #FEF3C7;
  color: #92400E;
  transform: translateY(-1px);
}
.like-btn:active { transform: scale(0.95); }
.like-btn.liked {
  background: linear-gradient(135deg, #F472B6, #8B5CF6);
  border-color: var(--color-fg);
  color: #fff;
  box-shadow: 3px 3px 0 var(--color-fg);
}
.like-btn.liked:hover {
  background: linear-gradient(135deg, #EC4899, #7C3AED);
  box-shadow: 4px 4px 0 var(--color-fg);
}
.like-btn .like-count {
  font-weight: 800;
  margin-left: 2px;
  min-width: 18px;
  text-align: center;
  background: rgba(0,0,0,0.06);
  border-radius: var(--radius-full);
  padding: 1px 7px;
  transition: background 0.2s;
}
.like-btn.liked .like-count {
  background: rgba(255,255,255,0.25);
}

.like-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.like-btn.animating .like-icon-wrap {
  animation: likeBounce 0.5s var(--ease-bounce);
}
@keyframes likeBounce {
  0%   { transform: scale(1); }
  20%  { transform: scale(1.5); }
  40%  { transform: scale(0.75); }
  60%  { transform: scale(1.2); }
  80%  { transform: scale(0.95); }
  100% { transform: scale(1); }
}

.float-plus {
  position: absolute;
  top: -10px;
  right: -16px;
  font-size: 13px;
  font-weight: 900;
  color: var(--color-secondary);
  pointer-events: none;
  animation: floatUp 0.8s ease-out forwards;
}
@keyframes floatUp {
  0%   { opacity: 1; transform: translateY(0) scale(1); }
  40%  { opacity: 1; transform: translateY(-16px) scale(1.3); }
  100% { opacity: 0; transform: translateY(-28px) scale(0.7); }
}

/* Particle burst */
.particles {
  position: absolute;
  top: 50%;
  left: 50%;
  pointer-events: none;
}
.particle {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  animation: particleBurst 0.6s ease-out forwards;
  --angle: calc(var(--i) * 60deg);
}
.particle:nth-child(1) { background: var(--color-tertiary); }
.particle:nth-child(2) { background: var(--color-secondary); }
.particle:nth-child(3) { background: var(--color-accent); }
.particle:nth-child(4) { background: var(--color-tertiary); }
.particle:nth-child(5) { background: var(--color-secondary); }
.particle:nth-child(6) { background: var(--color-quaternary); }

@keyframes particleBurst {
  0%   { transform: translate(0, 0) scale(1); opacity: 1; }
  100% { transform: translate(calc(cos(var(--angle)) * 20px), calc(sin(var(--angle)) * 20px)) scale(0); opacity: 0; }
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-author {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 14px;
  color: var(--color-fg);
}
.comment-time { font-size: 12px; color: var(--color-muted-fg); }
.comment-body { font-size: 14px; color: var(--color-fg); line-height: 1.6; }
</style>
