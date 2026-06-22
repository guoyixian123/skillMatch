<template>
  <div id="app-root" :class="{ 'is-login': authStore.isLoggedIn }">
    <AppNavbar v-if="authStore.isLoggedIn" />
    <main class="main-content">
      <router-view v-slot="{ Component, route }">
        <transition name="page-slide" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>
    <AppMobileNav v-if="authStore.isLoggedIn" />
  </div>
</template>

<script setup>
import { watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import AppNavbar from '@/components/AppNavbar.vue'
import AppMobileNav from '@/components/AppMobileNav.vue'
import ws from '@/utils/ws'

const authStore = useAuthStore()
const chatStore = useChatStore()

function connectWs() {
  chatStore.bindWebSocket()
  ws.connect()
}

function disconnectWs() {
  chatStore.unbindWebSocket()
  ws.disconnect()
}

// 监听登录状态变化
watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    connectWs()
  } else {
    disconnectWs()
  }
}, { immediate: true })

// 页面关闭时断开
onUnmounted(() => {
  disconnectWs()
})
</script>
