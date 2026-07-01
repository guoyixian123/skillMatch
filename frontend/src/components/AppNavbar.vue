<template>
  <nav class="app-navbar">
    <div class="navbar-inner">
      <router-link to="/discover" class="logo">
        <div class="logo-icon-wrap">
          <svg class="logo-svg" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <polygon points="28,3 10,26 22,26 18,45 38,22 26,22 30,3" fill="#FBBF24" stroke="#1E293B" stroke-width="2.5" stroke-linejoin="round"/>
            <circle cx="8" cy="8" r="4" fill="#F472B6" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="40" cy="8" r="4" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="8" cy="40" r="4" fill="#34D399" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="40" cy="40" r="4" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.5"/>
          </svg>
        </div>
        <div class="logo-text-wrap">
          <span class="logo-text">Skill</span><span class="logo-text accent">Match</span>
        </div>
      </router-link>

      <div class="nav-links">
        <router-link to="/discover" class="nav-link" active-class="active" data-idx="0">
          <el-icon class="nav-icon"><Compass /></el-icon>
          <span class="nav-label">发现</span>
        </router-link>
        <router-link to="/community" class="nav-link" active-class="active" data-idx="1">
          <el-icon class="nav-icon"><ChatDotRound /></el-icon>
          <span class="nav-label">社区</span>
        </router-link>
        <router-link to="/friends" class="nav-link" active-class="active" data-idx="2">
          <el-badge :value="chatStore.totalUnread" :hidden="!chatStore.totalUnread" class="badge-wrap">
            <el-icon class="nav-icon"><ChatLineRound /></el-icon>
          </el-badge>
          <span class="nav-label">好友</span>
        </router-link>
        <router-link to="/notifications" class="nav-link" active-class="active" data-idx="3">
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="badge-wrap">
            <el-icon class="nav-icon"><Bell /></el-icon>
          </el-badge>
          <span class="nav-label">通知</span>
        </router-link>
        <router-link to="/profile" class="nav-link" active-class="active" data-idx="4">
          <el-icon class="nav-icon"><UserFilled /></el-icon>
          <span class="nav-label">我的</span>
        </router-link>
      </div>

      <div class="nav-actions">
        <el-dropdown trigger="click" popper-class="user-dropdown-menu">
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { getDefaultAvatar } from '@/utils/avatar'
import { getUnreadCount, getLikeUnreadCount } from '@/api/notification'
import {
  Compass, ChatDotRound, ChatLineRound, Bell, UserFilled,
  Edit, SetUp, SwitchButton,
} from '@element-plus/icons-vue'
import ws from '@/utils/ws'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()
const unreadCount = ref(0)
let unsubNotif = null
let unsubFriend = null

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

onMounted(() => {
  fetchUnread()

  // 实时监听通知更新（点赞、评论等）
  unsubNotif = ws.on('notification_update', () => {
    console.log('[Navbar] 收到通知更新，刷新未读数')
    fetchUnread()
  })

  // 实时监听好友更新（新好友 → 刷新通知未读数）
  unsubFriend = ws.on('friend_update', () => {
    console.log('[Navbar] 收到好友更新，刷新未读数')
    fetchUnread()
  })
})

onUnmounted(() => {
  unsubNotif?.()
  unsubFriend?.()
})

watch(() => route.path, (path) => {
  if (path.startsWith('/chat/')) {
    setTimeout(fetchUnread, 800)
  } else {
    fetchUnread()
  }
})
</script>

<style scoped>
.app-navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 253, 245, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 2px solid var(--color-border);
  padding: 0 24px;
}
.navbar-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  gap: 20px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
  padding: 6px 10px 6px 6px;
  border-radius: 14px;
  transition: all 0.3s var(--ease-bounce);
  border: 2px solid transparent;
}
.logo:hover {
  border-color: var(--color-border);
  background: var(--color-muted);
}
.logo-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  transition: transform 0.4s var(--ease-bounce);
}
.logo:hover .logo-icon-wrap {
  transform: rotate(-10deg) scale(1.08);
}
.logo-svg {
  width: 38px;
  height: 38px;
}
.logo-text-wrap {
  display: flex;
  align-items: baseline;
}
.logo-text {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 800;
  color: var(--color-fg);
  letter-spacing: -0.5px;
  line-height: 1;
}
.logo-text.accent {
  color: var(--color-accent);
}

/* 导航链接 */
.nav-links {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
  color: var(--color-muted-fg);
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.25s var(--ease-bounce);
  position: relative;
  border: 2px solid transparent;
}
.nav-link:hover {
  color: var(--color-fg);
  background: var(--color-muted);
  border-color: var(--color-border);
}
.nav-link.active {
  color: var(--color-fg);
  background: #fff;
  border-color: var(--color-fg);
  box-shadow: 3px 3px 0 var(--color-fg);
}
.nav-icon {
  font-size: 16px;
  transition: transform 0.25s var(--ease-bounce);
}
.nav-link:hover .nav-icon {
  transform: scale(1.1);
}
.nav-link.active .nav-icon {
  color: var(--color-accent);
}
.nav-label {
  letter-spacing: 0.3px;
}

/* Badge */
.badge-wrap {
  display: flex;
  align-items: center;
}

/* 用户菜单 */
.nav-actions { flex-shrink: 0; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border: 2px solid var(--color-border);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s var(--ease-bounce);
  background: #fff;
}
.user-chip:hover {
  border-color: var(--color-accent);
  box-shadow: 3px 3px 0 rgba(139, 92, 246, 0.2);
}
.user-chip-avatar {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  border: 2px solid var(--color-border);
  object-fit: cover;
  transition: border-color 0.3s;
}
.user-chip:hover .user-chip-avatar {
  border-color: var(--color-accent);
}
.user-chip-name {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .nav-links { display: none; }
  .logo-text { display: none; }
}
</style>

<!-- 下拉菜单样式（teleport 到 body，不能 scoped） -->
<style>
.user-dropdown-menu {
  border-radius: 16px !important;
  border: 2px solid var(--color-fg) !important;
  box-shadow: 5px 5px 0 var(--color-fg) !important;
  padding: 6px !important;
  min-width: 180px !important;
  background: #fff !important;
}
.user-dropdown-menu .el-dropdown-menu__item {
  border-radius: 10px !important;
  padding: 10px 14px !important;
  font-family: var(--font-heading) !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  color: var(--color-fg) !important;
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  transition: all 0.2s var(--ease-bounce) !important;
}
.user-dropdown-menu .el-dropdown-menu__item .el-icon {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 26px !important;
  height: 26px !important;
  border-radius: 8px !important;
  font-size: 13px !important;
  flex-shrink: 0 !important;
  transition: transform 0.25s var(--ease-bounce) !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(1) .el-icon {
  background: #EDE9FE !important; color: #7C3AED !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(2) .el-icon {
  background: #D1FAE5 !important; color: #059669 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(3) .el-icon {
  background: #FEF3C7 !important; color: #D97706 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5) .el-icon {
  background: #FEE2E2 !important; color: #DC2626 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:hover {
  background: var(--color-muted) !important;
  transform: translateX(3px) !important;
}
.user-dropdown-menu .el-dropdown-menu__item:hover .el-icon {
  transform: scale(1.12) rotate(-6deg) !important;
}
.user-dropdown-menu .el-dropdown-menu__item--divided {
  border-top: 2px solid var(--color-border) !important;
  margin-top: 4px !important;
  padding-top: 12px !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5):hover {
  background: #FEE2E2 !important; color: #DC2626 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5):hover .el-icon {
  transform: scale(1.12) rotate(6deg) !important;
}
</style>
