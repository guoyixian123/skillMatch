<template>
  <div class="page-container">
    <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      <i class="pi pi-arrow-left"></i> 返回
    </button>

    <template v-if="profile">
      <!-- Header -->
      <div class="brutal-card" style="padding:0;overflow:hidden;">
        <div class="profile-cover pop-stripes"></div>
        <div class="profile-top">
          <img :src="profile.avatarUrl || getDefaultAvatar(profile.userId || profile.name)" class="brutal-avatar xl profile-avatar" />
          <div>
            <h1 class="profile-name">{{ profile.name }}</h1>
            <div class="profile-stats">
              <span><strong>{{ profile.likeCount || 0 }}</strong> 点赞</span>
              <span><strong>{{ profile.postCount || 0 }}</strong> 帖子</span>
            </div>
          </div>
          <div style="margin-left:auto;display:flex;gap:8px;">
            <button v-if="!isOwnProfile" class="brutal-btn small like-profile-btn" :class="{ liked: profileLiked }" @click="handleLikeProfile" :disabled="liking">
              <svg class="lp-icon" width="18" height="18" viewBox="0 0 24 24"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z" :fill="profileLiked ? '#FF6B6B' : 'none'" :stroke="profileLiked ? '#1A1A1A' : '#888'" stroke-width="2.5"/></svg>
              <span>{{ profileLiked ? '已赞' : '点赞' }}</span>
            </button>
            <button v-if="isFriend" class="brutal-btn primary small" @click="$router.push(`/chat/${userId}`)"><i class="pi pi-comment"></i> 发消息</button>
            <button v-else-if="!isOwnProfile" class="brutal-btn primary small" @click="showSendRequest = true"><i class="pi pi-send"></i> 发起交换</button>
          </div>
        </div>
      </div>

      <!-- Bio -->
      <div class="brutal-card accent-yellow" style="margin-top:20px;" v-if="profile.signature || profile.bio">
        <div v-if="profile.signature" class="profile-signature">"{{ profile.signature }}"</div>
        <div v-if="profile.bio" class="profile-bio">{{ profile.bio }}</div>
      </div>

      <!-- Contact (only if exchange accepted) -->
      <div v-if="profile.contactInfo" class="brutal-card accent-green" style="margin-top:20px;">
        <div class="section-title"><span class="dot" style="background:var(--color-green)"></span> 联系方式</div>
        <div style="font-size:15px;font-weight:700;">{{ profile.contactInfo }}</div>
      </div>

      <!-- Skills -->
      <div class="brutal-grid-2" style="margin-top:20px;">
        <div class="brutal-card accent-cyan">
          <div class="section-title"><span class="dot" style="background:var(--color-cyan)"></span> 我会的</div>
          <div class="flex-wrap">
            <span v-for="s in profile.canSkills" :key="s" class="brutal-tag can">{{ s }}</span>
            <span v-if="!profile.canSkills?.length" style="color:#888;font-size:13px;">暂无</span>
          </div>
        </div>
        <div class="brutal-card accent-pink">
          <div class="section-title"><span class="dot" style="background:var(--color-pink)"></span> 想学的</div>
          <div class="flex-wrap">
            <span v-for="s in profile.wantSkills" :key="s" class="brutal-tag want">{{ s }}</span>
            <span v-if="!profile.wantSkills?.length" style="color:#888;font-size:13px;">暂无</span>
          </div>
        </div>
      </div>

      <!-- Hobbies -->
      <div class="brutal-card" style="margin-top:20px;" v-if="profile.hobbies?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 兴趣爱好</div>
        <div class="flex-wrap">
          <span v-for="h in profile.hobbies" :key="h" class="brutal-tag hobby">{{ h }}</span>
        </div>
      </div>

      <!-- Gallery -->
      <div class="brutal-card" style="margin-top:20px;" v-if="profile.gallery?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-purple)"></span> 相册</div>
        <div class="brutal-grid-3" style="gap:12px;">
          <img v-for="img in profile.gallery" :key="img.id || img" :src="img.imageUrl || img" class="gallery-img" />
        </div>
      </div>

      <!-- Posts -->
      <div class="brutal-card accent-blue" style="margin-top:20px;" v-if="profile.posts?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-blue)"></span> 他的帖子</div>
        <div class="post-list">
          <div v-for="post in profile.posts" :key="post.id" class="post-item" @click="$router.push(`/community/${post.id}`)">
            <h3>{{ post.title }}</h3>
            <p>{{ post.body?.slice(0, 80) }}{{ post.body?.length > 80 ? '...' : '' }}</p>
            <div style="color:#888;font-size:12px;margin-top:4px;"><i class="pi pi-star"></i> {{ post.likeCount || 0 }} · <i class="pi pi-comments"></i> {{ post.commentCount || 0 }}</div>
          </div>
        </div>
      </div>
    </template>

    <!-- 无数据 -->
    <div v-else-if="!loading" class="empty-block">
      <div class="icon"><i class="pi pi-user" style="font-size:48px;"></i></div>
      <div style="font-weight:800;font-size:18px;">用户不存在</div>
    </div>

    <!-- Send Request Dialog -->
    <Dialog v-model:visible="showSendRequest" header="发起技能交换请求" :modal="true" :style="{ width: '420px' }">
      <div class="request-form">
        <label class="field-label">交换理由</label>
        <textarea v-model="requestReason" class="brutal-input" rows="3" placeholder="你好，想和你交流一下..." maxlength="150"></textarea>
        <div class="field-count">{{ requestReason.length }}/150</div>
        <button class="brutal-btn primary" style="width:100%;justify-content:center;margin-top:12px;" @click="handleSendRequest">发送请求</button>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Dialog from 'primevue/dialog'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { getUserProfile, getUserCard } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { getFriends } from '@/api/friend'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const userId = route.params.userId
const loading = ref(true)
const profile = ref(null)
const profileLiked = ref(false)
const liking = ref(false)
const isFriend = ref(false)
const showSendRequest = ref(false)
const requestReason = ref('')

const isOwnProfile = computed(() => String(authStore.user?.userId) === String(userId))

async function loadProfile() {
  loading.value = true
  try {
    const res = await getUserProfile(userId)
    profile.value = res.data
    profileLiked.value = res.data?.isLiked || false

    // 检查是否为好友
    try {
      const friendsRes = await getFriends()
      const list = Array.isArray(friendsRes.data) ? friendsRes.data : []
      isFriend.value = list.some(f => String(f.userId) === String(userId))
    } catch { /* ignore */ }
  } catch { profile.value = null } finally { loading.value = false }
}

async function handleLikeProfile() {
  if (liking.value) return
  liking.value = true
  const wasLiked = profileLiked.value
  profileLiked.value = !wasLiked
  try {
    const { likeProfile, unlikeProfile } = await import('@/api/user')
    if (wasLiked) await unlikeProfile(userId)
    else await likeProfile(userId)
    toast.add({ severity: 'success', summary: '成功', detail: wasLiked ? '已取消点赞' : '点赞成功', life: 3000 })
  } catch {
    profileLiked.value = wasLiked
    toast.add({ severity: 'warn', summary: '提示', detail: '操作失败，请重试', life: 3000 })
  } finally { liking.value = false }
}

async function handleSendRequest() {
  if (!requestReason.value.trim()) {
    toast.add({ severity: 'warn', summary: '提示', detail: '请输入交换理由', life: 3000 })
    return
  }
  try {
    const res = await createRequest({ toUserId: userId, reason: requestReason.value })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '交换请求已发送', life: 3000 })
    showSendRequest.value = false
    requestReason.value = ''
  } catch { /* handled */ }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-cover { height: 140px; background-color: #f0f0f0; }
.profile-top { display: flex; align-items: flex-end; gap: 16px; padding: 0 24px 20px; margin-top: -48px; }
.profile-avatar { flex-shrink: 0; background: #fff; }
.profile-name { font-size: 24px; font-weight: 900; }
.profile-stats { display: flex; gap: 16px; margin-top: 4px; font-size: 13px; color: #888; }
.profile-signature { font-size: 16px; font-weight: 700; font-style: italic; margin-bottom: 8px; }
.profile-bio { font-size: 14px; color: #555; line-height: 1.7; }

.gallery-img { width: 100%; aspect-ratio: 1; object-fit: cover; border: 2px solid #1A1A1A; }
.post-list { display: flex; flex-direction: column; gap: 10px; }
.post-item { padding: 12px; border: 2px solid #eee; cursor: pointer; }
.post-item:hover { border-color: var(--color-blue); }

.like-profile-btn.liked { background: var(--color-pink); color: #fff; }
.like-profile-btn.liked .lp-icon path { fill: #FF6B6B; stroke: #fff; }

.request-form { display: flex; flex-direction: column; gap: 8px; }
.field-label { font-size: 13px; font-weight: 700; text-transform: uppercase; }
.field-count { text-align: right; font-size: 12px; color: #aaa; }
</style>
