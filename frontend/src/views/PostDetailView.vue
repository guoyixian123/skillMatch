<template>
  <div class="page-container">
    <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      ← 返回社区
    </button>

    <div v-if="loading" class="loading-block">⚡ 加载中...</div>

    <template v-if="post">
      <!-- Post Content -->
      <div class="brutal-card accent-blue">
        <div class="post-author">
          <img
            :src="post.author?.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
            class="brutal-avatar"
          />
          <div>
            <div class="post-author-name">{{ post.author?.name }}</div>
            <div class="post-time">{{ formatTime(post.createdAt) }}</div>
          </div>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-body-full">{{ post.body }}</div>

        <div class="post-tags flex-wrap" v-if="post.tags?.length" style="margin:16px 0;">
          <span v-for="tag in post.tags" :key="tag" class="brutal-tag">{{ tag }}</span>
        </div>

        <div class="post-actions">
          <button class="brutal-btn small" :class="post.isLiked ? 'dark' : 'outline'" @click="handleToggleLike">
            <el-icon><Star /></el-icon>
            {{ post.isLiked ? '已赞' : '点赞' }} ({{ post.likeCount || 0 }})
          </button>
          <span style="color:#888;font-weight:700;">💬 {{ post.commentCount || 0 }} 条评论</span>
          <button v-if="isPostAuthor" class="brutal-btn danger small" style="margin-left:auto;" @click="handleDeletePost" :disabled="deletingPost">
            <el-icon><Delete /></el-icon> {{ deletingPost ? '删除中...' : '删除帖子' }}
          </button>
        </div>
      </div>

      <!-- Comments -->
      <div style="margin-top:24px;">
        <div class="section-title">
          <span class="dot" style="background:var(--color-yellow)"></span>
          评论 ({{ comments.length }})
        </div>

        <!-- Comment input -->
        <div class="brutal-card" style="margin-bottom:16px;padding:16px;">
          <div style="display:flex;gap:12px;">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              placeholder="写下你的评论..."
              maxlength="500"
              show-word-limit
            />
            <button class="brutal-btn primary small" style="flex-shrink:0;align-self:flex-end;" @click="handleComment" :disabled="commenting">
              {{ commenting ? '发送中' : '发送' }}
            </button>
          </div>
        </div>

        <!-- Comments list -->
        <div v-if="comments.length === 0" style="text-align:center;padding:40px;color:#888;font-weight:600;">
          暂无评论，来说两句吧
        </div>

        <div v-for="comment in comments" :key="comment.id" class="comment-item brutal-card" style="margin-bottom:8px;padding:16px;">
          <div class="comment-header">
            <img
              :src="comment.user?.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
              class="brutal-avatar"
              style="width:32px;height:32px;"
            />
            <span class="comment-author">{{ comment.user?.name }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            <button
              v-if="canDeleteComment(comment)"
              class="brutal-btn danger small"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, Delete } from '@element-plus/icons-vue'
import { getPostDetail, togglePostLike, getComments, createComment, deletePost, deleteComment } from '@/api/community'
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

const currentUserId = computed(() => String(authStore.user?.id || ''))
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
  try {
    const res = await togglePostLike(postId)
    post.value.isLiked = true
    post.value.likeCount = res.data.likeCount
    ElMessage.success('点赞成功')
  } catch {
    // may already be liked — backend returns error on duplicate
    ElMessage.warning('已经点赞过了')
  }
}

async function handleComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await createComment(postId, newComment.value.trim())
    ElMessage.success('评论成功')
    newComment.value = ''
    const res = await getComments(postId, { page: 1, size: 50 })
    comments.value = res.data?.list || []
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
    })
  } catch {
    return // user cancelled
  }
  deletingPost.value = true
  try {
    await deletePost(postId)
    ElMessage.success('帖子已删除')
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
    })
  } catch {
    return
  }
  deletingCommentId.value = comment.id
  try {
    await deleteComment(postId, comment.id)
    ElMessage.success('评论已删除')
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
.post-author-name { font-weight: 700; font-size: 15px; }
.post-time { font-size: 12px; color: #aaa; }
.post-title {
  font-size: 24px;
  font-weight: 900;
  margin-bottom: 12px;
}
.post-body-full {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}
.post-actions {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 2px solid #eee;
}

.comment-item {}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.comment-author { font-weight: 700; font-size: 14px; }
.comment-time { font-size: 12px; color: #aaa; }
.comment-body { font-size: 14px; color: #444; line-height: 1.6; }
</style>
