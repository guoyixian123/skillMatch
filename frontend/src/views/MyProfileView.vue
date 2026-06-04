<template>
  <div class="page-container" v-loading="userStore.loading">
    <header class="profile-header brutal-card" style="padding:0;overflow:hidden;">
      <!-- Cover -->
      <div class="profile-cover pop-dots"></div>
      <!-- Avatar -->
      <div class="profile-top">
        <img
          :src="authStore.user?.avatarUrl || getDefaultAvatar(authStore.user?.userId || authStore.user?.name)"
          class="brutal-avatar xl profile-avatar"
        />
        <div class="profile-info">
          <h1 class="profile-name">{{ authStore.user?.name || '未设置昵称' }}</h1>
          <div class="profile-stats">
            <span class="stat"><strong>{{ profile?.likeCount || 0 }}</strong> 点赞</span>
            <span class="stat"><strong>{{ profile?.postCount || 0 }}</strong> 帖子</span>
          </div>
        </div>
        <div class="profile-actions-top">
          <button class="brutal-btn outline small" @click="$router.push('/profile/edit')">
            <el-icon><Edit /></el-icon> 编辑
          </button>
        </div>
      </div>
    </header>

    <!-- Bio & Contact -->
    <div class="brutal-card accent-yellow" style="margin-top:20px;" v-if="profile">
      <div v-if="profile.signature" class="profile-signature">"{{ profile.signature }}"</div>
      <div v-if="profile.bio" class="profile-bio">{{ profile.bio }}</div>
      <div v-if="profile.contactInfo" class="profile-contact">
        <el-icon><Phone /></el-icon> {{ profile.contactInfo }}
      </div>
    </div>

    <!-- Quick Actions Grid -->
    <div class="brutal-grid-2" style="margin-top:20px;">
      <div class="brutal-card accent-cyan quick-card" @click="$router.push('/profile/skills')">
        <div class="quick-icon"><el-icon :size="32"><Aim /></el-icon></div>
        <div class="quick-title">技能标签</div>
        <div class="quick-desc">管理"我会的"和"想学的"技能</div>
        <div class="flex-wrap" style="margin-top:8px;">
          <span class="brutal-tag can" v-for="s in skills.canSkills?.slice(0,3)" :key="s.id">{{ s.skillName }}</span>
          <span class="brutal-tag want" v-for="s in skills.wantSkills?.slice(0,3)" :key="s.id">{{ s.skillName }}</span>
          <span v-if="(skills.canSkills?.length || 0) + (skills.wantSkills?.length || 0) === 0" class="brutal-tag">点击设置</span>
        </div>
      </div>

      <div class="brutal-card accent-pink quick-card" @click="$router.push('/profile/hobbies')">
        <div class="quick-icon"><el-icon :size="32"><Brush /></el-icon></div>
        <div class="quick-title">兴趣爱好</div>
        <div class="quick-desc">展示你的兴趣爱好</div>
        <div class="flex-wrap" style="margin-top:8px;">
          <span class="brutal-tag hobby" v-for="h in hobbies.slice(0,4)" :key="h.id">{{ h.icon }} {{ h.hobbyName }}</span>
          <span v-if="!hobbies.length" class="brutal-tag">点击设置</span>
        </div>
      </div>

      <div class="brutal-card accent-green quick-card" @click="$router.push('/profile/gallery')">
        <div class="quick-icon"><el-icon :size="32"><Picture /></el-icon></div>
        <div class="quick-title">个人相册</div>
        <div class="quick-desc">上传照片展示生活</div>
        <div class="gallery-preview" v-if="gallery.length">
          <img v-for="img in gallery.slice(0,4)" :key="img.id" :src="img.imageUrl" class="gallery-thumb" />
        </div>
        <span v-else class="brutal-tag" style="margin-top:8px;">点击上传</span>
      </div>

      <div class="brutal-card quick-card" @click="$router.push('/profile/password')">
        <div class="quick-icon"><el-icon :size="32"><Lock /></el-icon></div>
        <div class="quick-title">修改密码</div>
        <div class="quick-desc">更新你的登录密码</div>
      </div>
    </div>

    <!-- My Posts -->
    <div class="brutal-card accent-blue" style="margin-top:20px;" v-if="posts.length">
      <div class="section-title"><span class="dot" style="background:var(--color-blue)"></span> 我的帖子</div>
      <div class="post-list">
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-item"
          @click="router.push(`/community/${post.id}`)"
        >
          <h3 class="post-item-title">{{ post.title }}</h3>
          <p class="post-item-body">{{ post.body?.slice(0, 100) }}{{ post.body?.length > 100 ? '...' : '' }}</p>
          <div class="post-item-tags flex-wrap" v-if="post.tags?.length">
            <span v-for="tag in post.tags" :key="tag" class="brutal-tag">{{ tag }}</span>
          </div>
          <div class="post-item-meta">
            <button
              class="card-like-btn"
              :class="{ liked: post.isLiked, animating: post._animating }"
              @click.stop="handlePostLike(post)"
            >
              <span class="like-icon-wrap">
                <el-icon :size="14" :color="post.isLiked ? '#FFE4B5' : ''"><StarFilled v-if="post.isLiked" /><Star v-else /></el-icon>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { useUserStore } from '@/stores/user'
import { Edit, Phone, Aim, Brush, Picture, Lock, Star, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
import { getPosts, togglePostLike, unlikePost } from '@/api/community'

const router = useRouter()

const authStore = useAuthStore()
const userStore = useUserStore()

const profile = computed(() => userStore.profile)
const skills = computed(() => userStore.skills)
const hobbies = computed(() => userStore.hobbies)
const gallery = computed(() => userStore.gallery)

const posts = ref([])

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
  const userId = authStore.user?.userId
  if (userId) {
    await Promise.all([
      userStore.fetchProfile(userId),
      userStore.fetchSkills(),
      userStore.fetchHobbies(),
      userStore.fetchGallery(),
      getPosts({ authorId: userId, page: 1, size: 10 }).then(res => {
        posts.value = res.data?.list || []
      }),
    ])
  }
})
</script>

<style scoped>
.profile-cover {
  height: 140px;
  background-color: #f0f0f0;
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
}
.profile-info { flex: 1; padding-bottom: 4px; }
.profile-name {
  font-size: 24px;
  font-weight: 900;
}
.profile-stats {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  font-size: 13px;
  color: #888;
}
.stat strong { color: #1A1A1A; }
.profile-actions-top { flex-shrink: 0; padding-bottom: 4px; }

.profile-signature {
  font-size: 16px;
  font-weight: 700;
  font-style: italic;
  margin-bottom: 8px;
}
.profile-bio {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
}
.profile-contact {
  margin-top: 12px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--color-yellow);
  font-weight: 700;
  font-size: 13px;
  border: 2px solid #1A1A1A;
}

/* Quick Cards */
.quick-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-card:hover {
  transform: translate(-2px, -2px);
}
@media (max-width: 768px) {
  .brutal-grid-2 {
    grid-template-columns: 1fr;
  }
  .profile-top {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 16px 16px;
  }
  .profile-info {
    text-align: center;
  }
  .profile-stats {
    justify-content: center;
  }
  .profile-actions-top {
    width: 100%;
    display: flex;
    justify-content: center;
  }
}
.quick-icon { font-size: 32px; margin-bottom: 8px; }
.quick-title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 4px;
}
.quick-desc {
  font-size: 13px;
  color: #888;
}

.gallery-preview {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.gallery-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border: 2px solid #1A1A1A;
}

/* Post list */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-item {
  padding: 14px 16px;
  border: 2px solid #eee;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.post-item:hover {
  border-color: var(--color-blue);
  background: #f8f9ff;
}
.post-item-title {
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 6px;
}
.post-item-body {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 8px;
}
.post-item-tags {
  margin-bottom: 8px;
}
.post-item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #aaa;
}
.post-item-time {
  margin-left: auto;
}
.comment-meta {
  display: flex;
  align-items: center;
  gap: 3px;
  font-weight: 700;
  color: #888;
}
.card-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1.5px solid #ddd;
  border-radius: 99px;
  background: #fafafa;
  cursor: pointer;
  font-family: inherit;
  font-size: 12px;
  font-weight: 700;
  color: #888;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: visible;
}
.card-like-btn:hover {
  border-color: #FFD700;
  background: #FFFBE6;
  color: #E6A800;
  transform: translateY(-1px);
}
.card-like-btn:active {
  transform: scale(0.94);
}
.card-like-btn.liked {
  border-color: #1A1A1A;
  background: linear-gradient(135deg, #F5A623, #FFD700);
  color: #fff;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.15), 0 0 12px rgba(245,166,35,0.3);
}
.card-like-btn.liked:hover {
  background: linear-gradient(135deg, #FFB830, #FFE44D);
  border-color: #1A1A1A;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.2), 0 0 16px rgba(245,166,35,0.45);
}
.card-like-btn.liked .el-icon {
  color: #FFE4B5;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.1));
}
.like-icon-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.like-count {
  font-weight: 900;
  min-width: 16px;
  text-align: center;
}
.float-plus {
  position: absolute;
  top: -8px;
  right: -14px;
  font-size: 11px;
  font-weight: 900;
  color: #FFD700;
  pointer-events: none;
  animation: floatUp 0.8s ease-out forwards;
  text-shadow: 1px 1px 0 rgba(0,0,0,0.2);
}
@keyframes floatUp {
  0%   { opacity: 1; transform: translateY(0) scale(1); }
  40%  { opacity: 1; transform: translateY(-14px) scale(1.25); }
  100% { opacity: 0; transform: translateY(-24px) scale(0.7); }
}
.card-like-btn.animating .like-icon-wrap {
  animation: likeBounce 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
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
