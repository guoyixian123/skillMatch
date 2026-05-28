<template>
  <nav class="app-navbar">
    <div class="navbar-inner">
      <router-link to="/discover" class="logo">
        <span class="logo-icon">⚡</span>
        <span class="logo-text">SkillMatch</span>
      </router-link>

      <div class="nav-links">
        <router-link to="/discover" class="nav-link" active-class="active">
          <el-icon><Compass /></el-icon>
          <span>发现</span>
        </router-link>
        <router-link to="/community" class="nav-link" active-class="active">
          <el-icon><ChatDotRound /></el-icon>
          <span>社区</span>
        </router-link>
        <router-link to="/friends" class="nav-link" active-class="active">
          <el-badge :value="chatStore.totalUnread" :hidden="!chatStore.totalUnread" class="badge-wrap">
            <el-icon><ChatLineRound /></el-icon>
          </el-badge>
          <span>好友</span>
        </router-link>
        <router-link to="/notifications" class="nav-link" active-class="active">
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="badge-wrap">
            <el-icon><Bell /></el-icon>
          </el-badge>
          <span>通知</span>
        </router-link>
        <router-link to="/profile" class="nav-link" active-class="active">
          <el-icon><UserFilled /></el-icon>
          <span>我的</span>
        </router-link>
      </div>

      <div class="nav-actions">
        <el-dropdown trigger="click">
          <div class="user-chip">
            <img
              :src="authStore.user?.avatarUrl || getDefaultAvatar(authStore.user?.userId || authStore.user?.name)"
              class="user-chip-avatar"
              alt="avatar"
            />
            <span class="user-chip-name">{{ authStore.user?.name || '用户' }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">
                <el-icon><UserFilled /></el-icon> 我的主页
              </el-dropdown-item>
              <el-dropdown-item @click="$router.push('/profile/edit')">
                <el-icon><Edit /></el-icon> 编辑资料
              </el-dropdown-item>
              <el-dropdown-item @click="$router.push('/profile/skills')">
                <el-icon><SetUp /></el-icon> 技能管理
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getDefaultAvatar } from '@/utils/avatar'
import { getUnreadCount, getLikeUnreadCount } from '@/api/notification'
import {
  Compass, ChatDotRound, ChatLineRound, Bell, UserFilled,
  Edit, SetUp, SwitchButton,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()
const unreadCount = ref(0)

async function fetchUnread() {
  try {
    const [contactRes, likeRes] = await Promise.all([
      getUnreadCount(),
      getLikeUnreadCount(),
    ])
    chatStore.fetchUnreadCount()
    const contactCount = (typeof contactRes.data === 'number') ? contactRes.data : (contactRes.data?.pendingRequestCount || 0)
    const likeCount = (typeof likeRes.data === 'number') ? likeRes.data : 0
    unreadCount.value = contactCount + likeCount
  } catch { /* ignore */ }
}

async function handleLogout() {
  await authStore.doLogout()
  router.push('/login')
}

onMounted(fetchUnread)
watch(() => route.path, fetchUnread)
</script>

<style scoped>
.app-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 3px solid #1A1A1A;
  padding: 0 20px;
}
.navbar-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  gap: 32px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-icon { font-size: 28px; }
.logo-text {
  font-size: 22px;
  font-weight: 900;
  color: #1A1A1A;
  text-transform: uppercase;
  letter-spacing: -0.5px;
}
.nav-links {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-weight: 700;
  font-size: 14px;
  color: #555;
  text-decoration: none;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border: 2px solid transparent;
  transition: all 0.15s;
}
.nav-link:hover { color: #1A1A1A; background: #f5f5f5; }
.nav-link.active {
  color: #1A1A1A;
  border-bottom: 3px solid var(--color-yellow);
  margin-bottom: -1px;
  background: transparent;
}
.nav-actions { flex-shrink: 0; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border: 2px solid #1A1A1A;
  cursor: pointer;
  box-shadow: 2px 2px 0 #1A1A1A;
  transition: all 0.15s;
}
.user-chip:hover {
  transform: translate(-1px, -1px);
  box-shadow: 3px 3px 0 #1A1A1A;
}
.user-chip-avatar {
  width: 32px;
  height: 32px;
  border: 2px solid #1A1A1A;
  object-fit: cover;
}
.user-chip-name {
  font-weight: 700;
  font-size: 13px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.badge-wrap { display: flex; align-items: center; }

@media (max-width: 768px) {
  .nav-links { display: none; }
  .logo-text { display: none; }
}
</style>
