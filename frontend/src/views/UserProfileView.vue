<template>
  <div class="page-container" v-loading="loading">
    <button class="geo-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      <el-icon><ArrowLeft /></el-icon> 返回
    </button>

    <template v-if="profile">
      <!-- Header -->
      <div class="geo-card" style="padding:0;overflow:hidden;">
        <div class="profile-cover"></div>
        <div class="profile-top">
          <img
            :src="profile.avatarUrl || getDefaultAvatar(profile.userId || profile.name)"
            class="geo-avatar xl profile-avatar"
          />
          <div>
            <h1 class="profile-name">{{ profile.name }}</h1>
            <div class="profile-stats">
              <span><strong>{{ profile.likeCount || 0 }}</strong> 点赞</span>
              <span><strong>{{ profile.postCount || 0 }}</strong> 帖子</span>
            </div>
          </div>
          <div style="margin-left:auto;display:flex;gap:8px;">
            <button
              class="geo-btn small like-profile-btn"
              :class="{ liked: profileLiked }"
              @click="handleLikeProfile"
              :disabled="liking"
            >
              <svg class="lp-icon" width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                  :fill="profileLiked ? '#F472B6' : 'none'"
                  :stroke="profileLiked ? '#1E293B' : '#94A3B8'"
                  stroke-width="2.5"
                />
              </svg>
              <span>{{ profileLiked ? '已赞' : '点赞' }}</span>
            </button>
            <button v-if="isFriend" class="geo-btn primary small" @click="$router.push(`/chat/${userId}`)">
              <el-icon><ChatDotRound /></el-icon> 发消息
            </button>
            <button v-else class="geo-btn primary small" @click="showSendRequest = true">
              <el-icon><ChatDotRound /></el-icon> 发起交换
            </button>
          </div>
        </div>
      </div>

      <!-- Bio -->
      <div class="geo-card accent-yellow" style="margin-top:16px;" v-if="profile.signature || profile.bio">
        <div v-if="profile.signature" class="signature">"{{ profile.signature }}"</div>
        <div v-if="profile.bio" class="bio-text">{{ profile.bio }}</div>
      </div>

      <!-- Contact -->
      <div v-if="profile.contactInfo" class="geo-card accent-green" style="margin-top:16px;">
        <div class="section-title"><span class="dot" style="background:var(--color-quaternary)"></span> 联系方式</div>
        <div style="font-weight:600;">{{ profile.contactInfo }}</div>
      </div>

      <!-- Skills -->
      <div class="geo-grid-2" style="margin-top:16px;">
        <div class="geo-card accent-green">
          <div class="section-title">
            <span class="dot" style="background:var(--color-quaternary)"></span> 我会的
          </div>
          <div class="flex-wrap">
            <span v-for="s in profile.canSkills" :key="s" class="geo-tag can">{{ s }}</span>
            <span v-if="!profile.canSkills?.length" style="color:var(--color-muted-fg);">暂无</span>
          </div>
        </div>
        <div class="geo-card accent-pink">
          <div class="section-title">
            <span class="dot" style="background:var(--color-secondary)"></span> 想学的
          </div>
          <div class="flex-wrap">
            <span v-for="s in profile.wantSkills" :key="s" class="geo-tag want">{{ s }}</span>
            <span v-if="!profile.wantSkills?.length" style="color:var(--color-muted-fg);">暂无</span>
          </div>
        </div>
      </div>

      <!-- Hobbies -->
      <div class="geo-card accent-yellow" style="margin-top:16px;" v-if="profile.hobbies?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-tertiary)"></span> 兴趣爱好</div>
        <div class="flex-wrap">
          <span v-for="h in profile.hobbies" :key="h.id" class="geo-tag hobby" style="font-size:15px;padding:6px 16px;">
            {{ h.icon }} {{ h.hobbyName }}
          </span>
        </div>
      </div>

      <!-- Gallery -->
      <div class="geo-card" style="margin-top:16px;" v-if="profile.gallery?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-tertiary)"></span> 相册</div>
        <div class="geo-grid-3">
          <img v-for="(url, i) in profile.gallery" :key="i" :src="url" class="gallery-img" />
        </div>
      </div>

      <!-- Posts -->
      <div class="geo-card accent-violet" style="margin-top:16px;" v-if="posts.length">
        <div class="section-title"><span class="dot" style="background:var(--color-accent)"></span> Ta 的帖子</div>
        <div class="post-list">
          <div
            v-for="post in posts"
            :key="post.id"
            class="post-item"
            @click="$router.push(`/community/${post.id}`)"
          >
            <h3 class="post-item-title">{{ post.title }}</h3>
            <p class="post-item-body">{{ post.body?.slice(0, 100) }}{{ post.body?.length > 100 ? '...' : '' }}</p>
            <div class="post-item-tags flex-wrap" v-if="post.tags?.length">
              <span v-for="tag in post.tags" :key="tag" class="geo-tag">{{ tag }}</span>
            </div>
            <div class="post-item-meta">
              <button
                class="card-like-btn"
                :class="{ liked: post.isLiked, animating: post._animating }"
                @click.stop="handlePostLike(post)"
              >
                <span class="like-icon-wrap">
                  <el-icon :size="14" :color="post.isLiked ? '#F59E0B' : ''"><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
                  <span v-if="post._showPlus" class="float-plus">+1</span>
                </span>
                <span class="like-count">{{ formatCount(post.likeCount) }}</span>
              </button>
              <span class="comment-meta">
                <el-icon><ChatDotRound /></el-icon> {{ post.commentCount || 0 }}
              </span>
              <span class="post-item-time">{{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Activities -->
      <div class="geo-card" style="margin-top:16px;" v-if="profile.activities?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-accent)"></span> 最近动态</div>
        <div v-for="(act, idx) in profile.activities" :key="idx" class="activity-item">
          <span class="activity-type">{{ act.type }}</span>
          <span class="activity-content">{{ act.content }}</span>
          <span class="activity-time">{{ formatTime(act.createdAt) }}</span>
        </div>
      </div>
    </template>

    <!-- Send Request Dialog -->
    <el-dialog v-model="showSendRequest" title="发起技能交换请求" width="420px">
      <el-form @submit.prevent="handleSendRequest">
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
        <button
          type="submit"
          class="geo-btn primary"
          style="width:100%;justify-content:center;"
          :disabled="sending"
        >
          {{ sending ? '发送中...' : '发送请求' }}
        </button>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDefaultAvatar } from '@/utils/avatar'
import { ElMessage } from '@/utils/message'
import { ArrowLeft, ChatDotRound, Star, StarFilled } from '@element-plus/icons-vue'
import { getUserProfile } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { likeProfile, unlikeProfile } from '@/api/user'
import { getPosts, togglePostLike, unlikePost } from '@/api/community'
import { getFriends } from '@/api/friend'

const route = useRoute()
const userId = computed(() => route.params.userId)

const loading = ref(true)
const profile = ref(null)
const liking = ref(false)
const profileLiked = ref(false)
const posts = ref([])

const isFriend = ref(false)
const showSendRequest = ref(false)
const requestReason = ref('')
const sending = ref(false)

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

function formatCount(n) {
  if (!n) return '0'
  return n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : String(n)
}

async function handlePostLike(post) {
  const wasLiked = post.isLiked
  if (!wasLiked) {
    post._animating = true
    post._showPlus = true
    setTimeout(() => { post._animating = false }, 500)
    setTimeout(() => { post._showPlus = false }, 800)
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
    post.isLiked = wasLiked
    post.likeCount = Math.max(0, (post.likeCount || 0) + (wasLiked ? 1 : -1))
  }
}

onMounted(async () => {
  try {
    const [profileRes, postsRes] = await Promise.all([
      getUserProfile(userId.value),
      getPosts({ authorId: userId.value, page: 1, size: 10 }),
    ])
    profile.value = profileRes.data
    profileLiked.value = profileRes.data?.isLiked ?? false
    isFriend.value = profileRes.data?.isFriend ?? false
    posts.value = postsRes.data?.list || []

    if (!isFriend.value) {
      try {
        const friendsRes = await getFriends()
        const list = Array.isArray(friendsRes.data) ? friendsRes.data : []
        isFriend.value = list.some(f => f.userId === userId.value)
      } catch { /* ignore */ }
    }
  } catch { /* handled */ } finally {
    loading.value = false
  }
})

async function handleLikeProfile() {
  if (!profile.value) return
  const wasLiked = profileLiked.value
  liking.value = true
  profileLiked.value = !wasLiked
  profile.value.likeCount = Math.max(0, (profile.value.likeCount || 0) + (wasLiked ? -1 : 1))
  try {
    if (wasLiked) {
      const res = await unlikeProfile(userId.value)
      ElMessage.success(res.message || '已取消点赞')
    } else {
      const res = await likeProfile(userId.value)
      ElMessage.success(res.message || '点赞成功')
    }
  } catch {
    profileLiked.value = wasLiked
    profile.value.likeCount = Math.max(0, (profile.value.likeCount || 0) + (wasLiked ? 1 : -1))
    ElMessage.warning('操作失败，请重试')
  } finally {
    liking.value = false
  }
}

async function handleSendRequest() {
  if (!requestReason.value.trim()) {
    ElMessage.warning('请输入交换理由')
    return
  }
  sending.value = true
  try {
    const res = await createRequest({ toUserId: String(userId.value), reason: requestReason.value.trim() })
    ElMessage.success(res.message || '交换请求已发送')
    showSendRequest.value = false
  } catch { /* handled */ } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.profile-cover {
  height: 100px;
  background: linear-gradient(135deg, #EDE9FE, #FCE7F3, #FEF3C7);
}
.profile-top {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 0 24px 20px;
  margin-top: -48px;
}
.profile-avatar {
  flex-shrink: 0;
  background: #fff;
  box-shadow: 4px 4px 0 var(--color-fg);
  position: relative;
  z-index: 1;
}
.profile-name {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  color: var(--color-fg);
}
.profile-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-muted-fg);
  margin-top: 4px;
}
.profile-stats strong { color: var(--color-fg); }
.signature {
  font-size: 16px;
  font-weight: 600;
  font-style: italic;
  margin-bottom: 8px;
  color: var(--color-fg);
}
.bio-text {
  font-size: 14px;
  color: var(--color-muted-fg);
  line-height: 1.7;
}
.gallery-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 2px solid var(--color-border);
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}
.activity-type {
  padding: 2px 10px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  font-family: var(--font-heading);
}
.activity-content { flex: 1; font-size: 13px; }
.activity-time { font-size: 12px; color: var(--color-muted-fg); flex-shrink: 0; }

/* Like profile button */
.like-profile-btn {
  transition: all 0.3s var(--ease-bounce);
}
.like-profile-btn.liked {
  background: #FCE7F3;
  border-color: var(--color-fg);
  color: var(--color-fg);
}
.like-profile-btn .lp-icon {
  display: inline-block;
  vertical-align: middle;
  transition: transform 0.3s var(--ease-bounce);
}
.like-profile-btn:not(:disabled):active .lp-icon {
  transform: scale(1.3);
}

/* Post list */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-item {
  padding: 14px 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.3s, background 0.3s, transform 0.3s var(--ease-bounce);
}
.post-item:hover {
  border-color: var(--color-accent);
  background: #F5F3FF;
  transform: translateX(4px);
}
.post-item-title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 6px;
  color: var(--color-fg);
}
.post-item-body {
  font-size: 13px;
  color: var(--color-muted-fg);
  line-height: 1.6;
  margin-bottom: 8px;
}
.post-item-tags { margin-bottom: 8px; }
.post-item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted-fg);
}
.post-item-time { margin-left: auto; }
.comment-meta {
  display: flex;
  align-items: center;
  gap: 3px;
  font-weight: 600;
  color: var(--color-muted-fg);
}
.card-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-full);
  background: #fff;
  cursor: pointer;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-fg);
  transition: all 0.3s var(--ease-bounce);
  position: relative;
  overflow: visible;
}
.card-like-btn:hover {
  border-color: var(--color-tertiary);
  background: #FEF3C7;
  color: #92400E;
  transform: translateY(-1px);
}
.card-like-btn:active { transform: scale(0.94); }
.card-like-btn.liked {
  border-color: #D97706;
  background: #FEF3C7;
  color: #92400E;
  box-shadow: 2px 2px 0 #D97706;
}
.card-like-btn.liked .el-icon { color: #F59E0B; }
.like-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.like-count {
  font-weight: 800;
  min-width: 16px;
  text-align: center;
}
.float-plus {
  position: absolute;
  top: -8px;
  right: -14px;
  font-size: 11px;
  font-weight: 900;
  color: var(--color-secondary);
  pointer-events: none;
  animation: floatUp 0.8s ease-out forwards;
}
@keyframes floatUp {
  0%   { opacity: 1; transform: translateY(0) scale(1); }
  40%  { opacity: 1; transform: translateY(-14px) scale(1.25); }
  100% { opacity: 0; transform: translateY(-24px) scale(0.7); }
}
.card-like-btn.animating .like-icon-wrap {
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
</style>
