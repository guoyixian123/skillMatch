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
import { useAuthStore } from '@/stores/auth'
import AppNavbar from '@/components/AppNavbar.vue'
import AppMobileNav from '@/components/AppMobileNav.vue'

const authStore = useAuthStore()
</script>
