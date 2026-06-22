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
  background: rgba(255, 253, 245, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 2px solid var(--color-border);
  padding: 0 20px;
}
.navbar-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  gap: 32px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-icon {
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--color-tertiary);
  border-radius: var(--radius-full);
  border: 2px solid var(--color-fg);
  box-shadow: 3px 3px 0 var(--color-fg);
  transition: transform 0.3s var(--ease-bounce);
}
.logo:hover .logo-icon {
  transform: rotate(-12deg) scale(1.1);
}
.logo-text {
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 800;
  color: var(--color-fg);
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
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  color: var(--color-muted-fg);
  text-decoration: none;
  border-radius: var(--radius-full);
  transition: all 0.3s var(--ease-bounce);
}
.nav-link:hover {
  color: var(--color-fg);
  background: var(--color-muted);
}
.nav-link.active {
  color: var(--color-accent);
  background: #EDE9FE;
}
.nav-actions { flex-shrink: 0; }
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px 4px 4px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  cursor: pointer;
  box-shadow: 3px 3px 0 var(--color-border);
  transition: all 0.3s var(--ease-bounce);
}
.user-chip:hover {
  transform: translate(-1px, -1px);
  box-shadow: 4px 4px 0 var(--color-fg);
  border-color: var(--color-fg);
}
.user-chip-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-fg);
  object-fit: cover;
}
.user-chip-name {
  font-family: var(--font-heading);
  font-weight: 600;
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

<!-- 下拉菜单样式（teleport 到 body，不能 scoped） -->
<style>
/* 用户下拉菜单容器 */
.user-dropdown-menu {
  border-radius: var(--radius-lg) !important;
  border: 2px solid var(--color-fg) !important;
  box-shadow: 6px 6px 0 var(--color-fg) !important;
  padding: 8px !important;
  min-width: 180px !important;
  background: #fff !important;
}

/* 菜单项 */
.user-dropdown-menu .el-dropdown-menu__item {
  border-radius: var(--radius-md) !important;
  padding: 10px 14px !important;
  font-family: var(--font-body) !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  color: var(--color-fg) !important;
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
  line-height: 1.4 !important;
}

/* 图标圆形底色 */
.user-dropdown-menu .el-dropdown-menu__item .el-icon {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 28px !important;
  height: 28px !important;
  border-radius: 9999px !important;
  font-size: 14px !important;
  flex-shrink: 0 !important;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
}

/* 各菜单项图标颜色 */
.user-dropdown-menu .el-dropdown-menu__item:nth-child(1) .el-icon {
  background: #EDE9FE !important;
  color: #7C3AED !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(2) .el-icon {
  background: #D1FAE5 !important;
  color: #059669 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(3) .el-icon {
  background: #FEF3C7 !important;
  color: #D97706 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5) .el-icon {
  background: #FEE2E2 !important;
  color: #DC2626 !important;
}

/* hover 效果 */
.user-dropdown-menu .el-dropdown-menu__item:hover {
  background: var(--color-muted) !important;
  color: var(--color-fg) !important;
  transform: translateX(4px) !important;
}
.user-dropdown-menu .el-dropdown-menu__item:hover .el-icon {
  transform: scale(1.15) rotate(-8deg) !important;
}

/* 分割线 */
.user-dropdown-menu .el-dropdown-menu__item--divided {
  border-top: 2px solid var(--color-border) !important;
  margin-top: 4px !important;
  padding-top: 12px !important;
}

/* 退出登录 hover 红色 */
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5):hover {
  background: #FEE2E2 !important;
  color: #DC2626 !important;
}
.user-dropdown-menu .el-dropdown-menu__item:nth-child(5):hover .el-icon {
  transform: scale(1.15) rotate(8deg) !important;
}
</style>
