<template>
  <div class="page-container" v-loading="loading">
    <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:16px;">
      ← 返回
    </button>

    <template v-if="profile">
      <!-- Header -->
      <div class="brutal-card" style="padding:0;overflow:hidden;">
        <div class="profile-cover pop-stripes"></div>
        <div class="profile-top">
          <img
            :src="profile.avatarUrl || getDefaultAvatar(profile.userId || profile.name)"
            class="brutal-avatar xl profile-avatar"
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
              class="brutal-btn small like-profile-btn"
              :class="{ liked: profileLiked }"
              @click="handleLikeProfile"
              :disabled="liking"
            >
              <svg class="lp-icon" width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                  :fill="profileLiked ? '#FF6B6B' : 'none'"
                  :stroke="profileLiked ? '#1A1A1A' : '#888'"
                  stroke-width="2.5"
                />
              </svg>
              <span>{{ profileLiked ? '已赞' : '点赞' }}</span>
            </button>
            <button class="brutal-btn primary small" @click="showSendRequest = true">
              💬 发起交换
            </button>
          </div>
        </div>
      </div>

      <!-- Bio -->
      <div class="brutal-card accent-yellow" style="margin-top:16px;" v-if="profile.signature || profile.bio">
        <div v-if="profile.signature" class="signature">"{{ profile.signature }}"</div>
        <div v-if="profile.bio" class="bio-text">{{ profile.bio }}</div>
      </div>

      <!-- Contact (only if exchange accepted) -->
      <div v-if="profile.contactInfo" class="brutal-card accent-green" style="margin-top:16px;">
        <div class="section-title"><span class="dot" style="background:var(--color-green)"></span> 联系方式</div>
        <div style="font-weight:700;">{{ profile.contactInfo }}</div>
      </div>

      <!-- Skills -->
      <div class="brutal-grid-2" style="margin-top:16px;">
        <div class="brutal-card accent-cyan">
          <div class="section-title">
            <span class="dot" style="background:var(--color-cyan)"></span> 我会的
          </div>
          <div class="flex-wrap">
            <span v-for="s in profile.canSkills" :key="s" class="brutal-tag can">{{ s }}</span>
            <span v-if="!profile.canSkills?.length" style="color:#888;">暂无</span>
          </div>
        </div>
        <div class="brutal-card accent-pink">
          <div class="section-title">
            <span class="dot" style="background:var(--color-pink)"></span> 想学的
          </div>
          <div class="flex-wrap">
            <span v-for="s in profile.wantSkills" :key="s" class="brutal-tag want">{{ s }}</span>
            <span v-if="!profile.wantSkills?.length" style="color:#888;">暂无</span>
          </div>
        </div>
      </div>

      <!-- Hobbies -->
      <div class="brutal-card accent-purple" style="margin-top:16px;" v-if="profile.hobbies?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-purple)"></span> 兴趣爱好</div>
        <div class="flex-wrap">
          <span v-for="h in profile.hobbies" :key="h.name" class="brutal-tag hobby" style="font-size:15px;padding:6px 16px;">
            {{ h.icon }} {{ h.hobbyName }}
          </span>
        </div>
      </div>

      <!-- Gallery -->
      <div class="brutal-card" style="margin-top:16px;" v-if="profile.gallery?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 相册</div>
        <div class="brutal-grid-3">
          <img v-for="(url, i) in profile.gallery" :key="i" :src="url" class="gallery-img" />
        </div>
      </div>

      <!-- Activities -->
      <div class="brutal-card" style="margin-top:16px;" v-if="profile.activities?.length">
        <div class="section-title"><span class="dot" style="background:var(--color-blue)"></span> 最近动态</div>
        <div v-for="act in profile.activities" :key="act.id" class="activity-item">
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
          class="brutal-btn primary"
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDefaultAvatar } from '@/utils/avatar'
import { ElMessage } from 'element-plus'
import { getUserProfile } from '@/api/matching'
import { createRequest } from '@/api/notification'
import { likeProfile, unlikeProfile } from '@/api/user'

const route = useRoute()
const userId = route.params.userId

const loading = ref(true)
const profile = ref(null)
const liking = ref(false)
const profileLiked = ref(false)

const showSendRequest = ref(false)
const requestReason = ref('')
const sending = ref(false)

function formatTime(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  try {
    const res = await getUserProfile(userId)
    profile.value = res.data
    profileLiked.value = res.data?.isLiked ?? false
  } catch { /* handled */ } finally {
    loading.value = false
  }
})

async function handleLikeProfile() {
  if (!profile.value) return
  const wasLiked = profileLiked.value

  liking.value = true

  // optimistic update
  profileLiked.value = !wasLiked
  profile.value.likeCount = Math.max(0, (profile.value.likeCount || 0) + (wasLiked ? -1 : 1))

  try {
    if (wasLiked) {
      await unlikeProfile(userId)
      ElMessage.success('已取消点赞')
    } else {
      await likeProfile(userId)
      ElMessage.success('点赞成功')
    }
  } catch {
    // rollback
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
    await createRequest({ toUserId: String(userId), reason: requestReason.value.trim() })
    ElMessage.success('交换请求已发送')
    showSendRequest.value = false
  } catch { /* handled */ } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.profile-cover { height: 100px; }
.profile-top {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  padding: 0 24px 20px;
  margin-top: -48px;
}
.profile-avatar { flex-shrink: 0; background: #fff; }
.profile-name { font-size: 24px; font-weight: 900; }
.profile-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}
.profile-stats strong { color: #1A1A1A; }
.signature {
  font-size: 16px;
  font-weight: 700;
  font-style: italic;
  margin-bottom: 8px;
}
.bio-text {
  font-size: 14px;
  color: #555;
  line-height: 1.7;
}
.gallery-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border: 2px solid #1A1A1A;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.activity-type {
  padding: 2px 8px;
  border: 1px solid #ccc;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  flex-shrink: 0;
}
.activity-content { flex: 1; font-size: 13px; }
.activity-time { font-size: 12px; color: #aaa; flex-shrink: 0; }

/* Like profile button */
.like-profile-btn {
  transition: all 0.2s ease;
}
.like-profile-btn.liked {
  background: #FF6B6B;
  border-color: #1A1A1A;
  color: #fff;
}
.like-profile-btn .lp-icon {
  display: inline-block;
  vertical-align: middle;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.like-profile-btn:not(:disabled):active .lp-icon {
  transform: scale(1.3);
}
.like-profile-btn.liked .lp-icon {
  filter: drop-shadow(2px 2px 0 rgba(0,0,0,0.15));
}
</style>
