<template>
  <nav class="mobile-nav">
    <router-link to="/discover" class="mobile-nav-item" active-class="active">
      <div class="nav-icon-wrap">
        <el-icon :size="22"><Compass /></el-icon>
      </div>
      <span>发现</span>
    </router-link>
    <router-link to="/community" class="mobile-nav-item" active-class="active">
      <div class="nav-icon-wrap">
        <el-icon :size="22"><ChatDotRound /></el-icon>
      </div>
      <span>社区</span>
    </router-link>
    <router-link to="/friends" class="mobile-nav-item" active-class="active">
      <div class="nav-icon-wrap">
        <el-badge :value="chatStore.totalUnread" :hidden="!chatStore.totalUnread">
          <el-icon :size="22"><ChatLineRound /></el-icon>
        </el-badge>
      </div>
      <span>好友</span>
    </router-link>
    <router-link to="/notifications" class="mobile-nav-item" active-class="active">
      <div class="nav-icon-wrap">
        <el-badge :value="unreadCount" :hidden="!unreadCount">
          <el-icon :size="22"><Bell /></el-icon>
        </el-badge>
      </div>
      <span>通知</span>
    </router-link>
    <router-link to="/profile" class="mobile-nav-item" active-class="active">
      <div class="nav-icon-wrap">
        <el-icon :size="22"><UserFilled /></el-icon>
      </div>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { getUnreadCount, getLikeUnreadCount } from '@/api/notification'
import { Compass, ChatDotRound, ChatLineRound, Bell, UserFilled } from '@element-plus/icons-vue'
import ws from '@/utils/ws'

const route = useRoute()
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

onMounted(() => {
  fetchUnread()
  unsubNotif = ws.on('notification_update', () => fetchUnread())
  unsubFriend = ws.on('friend_update', () => fetchUnread())
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
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255, 253, 245, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 2px solid var(--color-border);
  z-index: 100;
  padding: 6px 0 env(safe-area-inset-bottom);
}
.mobile-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 6px 0;
  color: var(--color-muted-fg);
  text-decoration: none;
  font-size: 11px;
  font-family: var(--font-heading);
  font-weight: 600;
  position: relative;
  transition: color 0.3s, transform 0.3s var(--ease-bounce);
  -webkit-tap-highlight-color: transparent;
}
.nav-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  transition: all 0.3s var(--ease-bounce);
}
.mobile-nav-item.active {
  color: var(--color-accent);
}
.mobile-nav-item.active .nav-icon-wrap {
  background: #EDE9FE;
  transform: scale(1.1);
}
/* Active indicator pill */
.mobile-nav-item.active::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 4px;
  background: var(--color-accent);
  border-radius: var(--radius-full);
}
.mobile-nav-item:active {
  transform: scale(0.92);
}
@media (max-width: 768px) {
  .mobile-nav {
    display: flex;
    justify-content: space-around;
  }
  .page-container {
    padding-bottom: 80px;
  }
}
</style>
