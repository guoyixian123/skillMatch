<template>
  <div class="page-container">
    <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      <i class="pi pi-arrow-left"></i> 返回社区
    </button>

    <div v-if="loading" class="loading-block"><i class="pi pi-spinner pi-spin"></i> 加载中...</div>

    <template v-if="post">
      <!-- Post Content -->
      <div class="brutal-card accent-blue">
        <div class="post-author">
          <img :src="post.author?.avatarUrl || getDefaultAvatar(post.author?.id || post.author?.name)" class="brutal-avatar" style="cursor:pointer;" @click="$router.push(`/user/${post.author?.id}`)" />
          <div>
            <div class="post-author-name link" @click="$router.push(`/user/${post.author?.id}`)">{{ post.author?.name }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-body-full">{{ post.body }}</div>

        <div class="post-tags flex-wrap" v-if="post.tags?.length" style="margin:16px 0;">
          <span v-for="tag in post.tags" :key="tag" class="brutal-tag">{{ tag }}</span>
        </div>

        <div class="post-actions">
          <button class="brutal-btn small like-btn" :class="{ liked: post.isLiked, animating: likeAnimating }" @click="handleToggleLike">
            <span class="like-icon-wrap">
              <i class="pi" :class="post.isLiked ? 'pi-star-fill' : 'pi-star'" style="font-size:20px;color:var(--color-pink);"></i>
              <span v-if="showPlus" class="float-plus">+1</span>
              <span v-if="showParticles" class="particles">
                <i v-for="n in 6" :key="n" class="particle" :style="{ '--i': n }"></i>
              </span>
            </span>
            <span class="like-text">{{ post.isLiked ? '已赞' : '点赞' }}</span>
            <span class="like-count">{{ post.likeCount || 0 }}</span>
          </button>
          <span style="color:#888;font-weight:700;"><i class="pi pi-comments"></i> {{ post.commentCount || 0 }} 条评论</span>
          <button v-if="isPostAuthor" class="brutal-btn danger small" style="margin-left:auto;" @click="handleDeletePost" :disabled="deletingPost">
            <i class="pi pi-trash"></i> {{ deletingPost ? '删除中...' : '删除帖子' }}
          </button>
        </div>
      </div>

      <!-- Comments -->
      <div class="brutal-card" style="margin-top:20px;">
        <div class="section-title"><span class="dot" style="background:var(--color-cyan)"></span> 评论</div>

        <!-- New comment -->
        <div style="display:flex;gap:10px;margin-bottom:24px;">
          <textarea v-model="newComment" class="brutal-input" rows="2" placeholder="写下你的评论..." style="flex:1;resize:none;"></textarea>
          <button class="brutal-btn primary small" @click="handleCreateComment" :disabled="!newComment.trim() || submitting" style="align-self:flex-end;">
            <i class="pi pi-send"></i> {{ submitting ? '发送中...' : '发送' }}
          </button>
        </div>

        <!-- Comment list -->
        <div v-if="comments.length === 0" style="text-align:center;padding:20px;color:#888;font-weight:700;">暂无评论</div>
        <div v-else class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <img :src="c.author?.avatarUrl || getDefaultAvatar(c.author?.id || c.author?.name)" class="brutal-avatar" style="width:36px;height:36px;" />
            <div class="comment-body">
              <div class="comment-header">
                <strong>{{ c.author?.name }}</strong>
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
              </div>
              <div class="comment-content">{{ c.body }}</div>
            </div>
            <button v-if="canDeleteComment(c)" class="brutal-btn danger small" style="padding:4px 8px;font-size:11px;" @click="handleDeleteComment(c)" :disabled="deletingCommentId === c.id">
              <i class="pi pi-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { getPostDetail, deletePost, togglePostLike, unlikePost, getComments, createComment, deleteComment } from '@/api/community'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const authStore = useAuthStore()

const postId = route.params.postId
const loading = ref(true)
const post = ref(null)
const comments = ref([])
const newComment = ref('')
const submitting = ref(false)
const deletingPost = ref(false)
const deletingCommentId = ref(null)
const likeAnimating = ref(false)
const showPlus = ref(false)
const showParticles = ref(false)

const isPostAuthor = computed(() => String(authStore.user?.userId) === String(post.value?.author?.id))

function canDeleteComment(comment) {
  return String(authStore.user?.userId) === String(comment.author?.id)
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

async function loadPost() {
  loading.value = true
  try {
    const [postRes, commentRes] = await Promise.all([
      getPostDetail(postId),
      getComments(postId),
    ])
    post.value = postRes.data
    comments.value = Array.isArray(commentRes.data) ? commentRes.data : (commentRes.data?.list || [])
  } catch { /* handled */ } finally { loading.value = false }
}

async function handleToggleLike() {
  const wasLiked = post.value.isLiked
  if (!wasLiked) {
    likeAnimating.value = true; showPlus.value = true; showParticles.value = true
    setTimeout(() => { likeAnimating.value = false }, 500)
    setTimeout(() => { showPlus.value = false; showParticles.value = false }, 800)
  }
  post.value.isLiked = !wasLiked
  post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (wasLiked ? -1 : 1))
  try {
    if (wasLiked) await unlikePost(postId)
    else { const res = await togglePostLike(postId); post.value.likeCount = res.data?.likeCount ?? post.value.likeCount }
  } catch {
    post.value.isLiked = wasLiked
    post.value.likeCount = Math.max(0, (post.value.likeCount || 0) + (wasLiked ? 1 : -1))
  }
}

async function handleCreateComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    const res = await createComment(postId, { body: newComment.value.trim() })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '评论成功', life: 3000 })
    newComment.value = ''
    loadPost()
  } catch { /* handled */ } finally { submitting.value = false }
}

async function handleDeletePost() {
  confirm.require({
    message: '确定要删除这篇帖子吗？删除后不可恢复。',
    header: '确认删除',
    rejectLabel: '取消',
    acceptLabel: '删除',
    acceptClass: 'p-button-danger',
    accept: async () => {
      deletingPost.value = true
      try {
        await deletePost(postId)
        toast.add({ severity: 'success', summary: '成功', detail: '帖子已删除', life: 3000 })
        router.push('/community')
      } catch { /* handled */ } finally { deletingPost.value = false }
    },
  })
}

async function handleDeleteComment(comment) {
  confirm.require({
    message: '确定要删除这条评论吗？',
    header: '确认删除',
    rejectLabel: '取消',
    acceptLabel: '删除',
    acceptClass: 'p-button-danger',
    accept: async () => {
      deletingCommentId.value = comment.id
      try {
        await deleteComment(comment.id)
        toast.add({ severity: 'success', summary: '成功', detail: '评论已删除', life: 3000 })
        loadPost()
      } catch { /* handled */ } finally { deletingCommentId.value = null }
    },
  })
}

onMounted(loadPost)
</script>

<style scoped>
.post-author { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.post-author-name { font-weight: 700; font-size: 14px; }
.post-author-name.link { cursor: pointer; }
.post-author-name.link:hover { text-decoration: underline; }
.post-time { font-size: 12px; color: #aaa; }
.post-title { font-size: 24px; font-weight: 900; margin-bottom: 12px; }
.post-body-full { font-size: 15px; line-height: 1.8; white-space: pre-wrap; color: #333; }

.post-actions { display: flex; align-items: center; gap: 12px; padding-top: 16px; border-top: 2px solid #eee; margin-top: 16px; }

.like-btn { transition: all 0.25s; }
.like-btn.liked { background: linear-gradient(135deg, #F5A623, #FFD700); color: #fff; border-color: #1A1A1A; }
.like-icon-wrap { position: relative; display: inline-flex; align-items: center; }
.like-text { font-weight: 700; }
.like-count { font-weight: 900; margin-left: 4px; }

.float-plus { position: absolute; top: -8px; right: -14px; font-size: 14px; font-weight: 900; color: #FFD700; animation: floatUp 0.8s ease-out forwards; }
@keyframes floatUp {
  0% { opacity: 1; transform: translateY(0) scale(1); }
  40% { opacity: 1; transform: translateY(-14px) scale(1.25); }
  100% { opacity: 0; transform: translateY(-24px) scale(0.8); }
}
.particles { position: absolute; inset: 0; pointer-events: none; }
.particle { position: absolute; width: 6px; height: 6px; background: #FFD700; border-radius: 50%; animation: particleBurst 0.6s ease-out forwards; }
.particle:nth-child(1) { --angle: 0deg; }
.particle:nth-child(2) { --angle: 60deg; }
.particle:nth-child(3) { --angle: 120deg; }
.particle:nth-child(4) { --angle: 180deg; }
.particle:nth-child(5) { --angle: 240deg; }
.particle:nth-child(6) { --angle: 300deg; }
@keyframes particleBurst {
  0% { opacity: 1; transform: translate(0, 0) scale(1); }
  100% { opacity: 0; transform: translate(calc(cos(var(--angle)) * 30px), calc(sin(var(--angle)) * 30px)) scale(0); }
}
.like-btn.animating .like-icon-wrap { animation: likePop 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
@keyframes likePop {
  0% { transform: scale(1); } 25% { transform: scale(1.45); } 50% { transform: scale(0.8); } 75% { transform: scale(1.15); } 100% { transform: scale(1); }
}

.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-item { display: flex; gap: 10px; align-items: flex-start; }
.comment-body { flex: 1; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.comment-header strong { font-size: 14px; }
.comment-time { font-size: 11px; color: #aaa; }
.comment-content { font-size: 14px; color: #444; line-height: 1.6; }
</style>
