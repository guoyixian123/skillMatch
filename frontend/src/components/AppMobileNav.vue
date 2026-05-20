<template>
  <nav class="mobile-nav">
    <router-link to="/discover" class="mobile-nav-item" active-class="active">
      <el-icon :size="22"><Compass /></el-icon>
      <span>发现</span>
    </router-link>
    <router-link to="/community" class="mobile-nav-item" active-class="active">
      <el-icon :size="22"><ChatDotRound /></el-icon>
      <span>社区</span>
    </router-link>
    <router-link to="/notifications" class="mobile-nav-item" active-class="active">
      <el-badge :value="unreadCount" :hidden="!unreadCount">
        <el-icon :size="22"><Bell /></el-icon>
      </el-badge>
      <span>通知</span>
    </router-link>
    <router-link to="/profile" class="mobile-nav-item" active-class="active">
      <el-icon :size="22"><UserFilled /></el-icon>
      <span>我的</span>
    </router-link>
  </nav>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUnreadCount } from '@/api/notification'
import { Compass, ChatDotRound, Bell, UserFilled } from '@element-plus/icons-vue'

const unreadCount = ref(0)

onMounted(async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.pendingRequestCount || 0
  } catch { /* ignore */ }
})
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
