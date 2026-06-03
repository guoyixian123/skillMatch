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
    <!-- PrimeVue 全局服务组件 -->
    <Toast position="top-center" />
    <ConfirmDialog />
    <DynamicDialog />
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import AppNavbar from '@/components/AppNavbar.vue'
import AppMobileNav from '@/components/AppMobileNav.vue'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import DynamicDialog from 'primevue/dynamicdialog'
import { useToast } from 'primevue/usetoast'
import { setToastInstance } from '@/utils/toast'

const authStore = useAuthStore()
const primeToast = useToast()

// 注入全局 toast 实例，供 axios 拦截器等非组件环境使用
setToastInstance(primeToast)
</script>
