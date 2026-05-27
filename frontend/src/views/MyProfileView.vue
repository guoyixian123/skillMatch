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
          <h1 class="profile-name">{{ authStore.user?.name || authStore.user?.nickname || '未设置昵称' }}</h1>
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
        <div class="quick-icon">🎯</div>
        <div class="quick-title">技能标签</div>
        <div class="quick-desc">管理"我会的"和"想学的"技能</div>
        <div class="flex-wrap" style="margin-top:8px;">
          <span class="brutal-tag can" v-for="s in skills.canSkills?.slice(0,3)" :key="s.id">{{ s.skillName }}</span>
          <span class="brutal-tag want" v-for="s in skills.wantSkills?.slice(0,3)" :key="s.id">{{ s.skillName }}</span>
          <span v-if="(skills.canSkills?.length || 0) + (skills.wantSkills?.length || 0) === 0" class="brutal-tag">点击设置</span>
        </div>
      </div>

      <div class="brutal-card accent-pink quick-card" @click="$router.push('/profile/hobbies')">
        <div class="quick-icon">🎨</div>
        <div class="quick-title">兴趣爱好</div>
        <div class="quick-desc">展示你的兴趣爱好</div>
        <div class="flex-wrap" style="margin-top:8px;">
          <span class="brutal-tag hobby" v-for="h in hobbies.slice(0,4)" :key="h.id">{{ h.icon }} {{ h.hobbyName }}</span>
          <span v-if="!hobbies.length" class="brutal-tag">点击设置</span>
        </div>
      </div>

      <div class="brutal-card accent-green quick-card" @click="$router.push('/profile/gallery')">
        <div class="quick-icon">🖼️</div>
        <div class="quick-title">个人相册</div>
        <div class="quick-desc">上传照片展示生活</div>
        <div class="gallery-preview" v-if="gallery.length">
          <img v-for="img in gallery.slice(0,4)" :key="img.id" :src="img.imageUrl" class="gallery-thumb" />
        </div>
        <span v-else class="brutal-tag" style="margin-top:8px;">点击上传</span>
      </div>

      <div class="brutal-card quick-card" @click="$router.push('/profile/password')">
        <div class="quick-icon">🔒</div>
        <div class="quick-title">修改密码</div>
        <div class="quick-desc">更新你的登录密码</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getDefaultAvatar } from '@/utils/avatar'
import { useUserStore } from '@/stores/user'
import { Edit, Phone } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const userStore = useUserStore()

const profile = computed(() => userStore.profile)
const skills = computed(() => userStore.skills)
const hobbies = computed(() => userStore.hobbies)
const gallery = computed(() => userStore.gallery)

onMounted(async () => {
  const userId = authStore.user?.userId
  if (userId) {
    await Promise.all([
      userStore.fetchProfile(userId),
      userStore.fetchSkills(),
      userStore.fetchHobbies(),
      userStore.fetchGallery(),
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
</style>
