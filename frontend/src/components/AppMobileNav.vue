<template>
  <nav class="mobile-nav">
    <router-link to="/discover" class="mobile-nav-item" active-class="active">
      <i class="pi pi-compass"></i>
      <span>发现</span>
    </router-link>
    <router-link to="/community" class="mobile-nav-item" active-class="active">
      <i class="pi pi-comments"></i>
      <span>社区</span>
    </router-link>
    <router-link to="/friends" class="mobile-nav-item" active-class="active">
      <span class="mob-icon-badge">
        <i class="pi pi-comment"></i>
        <Badge v-if="chatStore.totalUnread" :value="chatStore.totalUnread" severity="warn" class="mob-badge" />
      </span>
      <span>好友</span>
    </router-link>
    <router-link to="/notifications" class="mobile-nav-item" active-class="active">
      <span class="mob-icon-badge">
        <i class="pi pi-bell"></i>
        <Badge v-if="unreadCount" :value="unreadCount" severity="danger" class="mob-badge" />
      </span>
      <span>通知</span>
    </router-link>
    <router-link to="/profile" class="mobile-nav-item" active-class="active">
      <i class="pi pi-user"></i>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { getUnreadCount, getLikeUnreadCount } from '@/api/notification'
import Badge from 'primevue/badge'

const route = useRoute()
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

onMounted(fetchUnread)
watch(() => route.path, fetchUnread)
</script>

<style scoped>
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 3px solid #1A1A1A;
  z-index: 100;
  padding: 6px 0 env(safe-area-inset-bottom);
}
.mobile-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 0;
  color: #888;
  text-decoration: none;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
}
.mobile-nav-item.active {
  color: #1A1A1A;
}
.mobile-nav-item i { font-size: 22px; }
.mob-icon-badge {
  position: relative;
  display: inline-flex;
}
.mob-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  transform: scale(0.7);
}
.mob-badge :deep(.p-badge) {
  min-width: 16px;
  height: 16px;
  font-size: 9px;
  line-height: 16px;
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
