import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminLogin } from '@/api/auth'

export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  let saved = null; try { saved = JSON.parse(localStorage.getItem('admin_info')) } catch(_){}
const admin = ref(saved)

  const isLoggedIn = computed(() => !!token.value)

  async function login(credentials) {
    const res = await adminLogin(credentials)
    const info = res.data.admin
    token.value = res.data.token
    admin.value = info
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_info', JSON.stringify(info))
    return res
  }

  function logout() {
    token.value = ''
    admin.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_info')
  }

  return { token, admin, isLoggedIn, login, logout }
})
