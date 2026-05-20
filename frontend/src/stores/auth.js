import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import { updateLocation } from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const latitude = ref(parseFloat(localStorage.getItem('lat') || '') || null)
  const longitude = ref(parseFloat(localStorage.getItem('lng') || '') || null)

  const isLoggedIn = computed(() => !!token.value)

  async function fetchIPLocation() {
    try {
      const coords = await getIPCoords()
      if (coords) {
        latitude.value = coords.lat
        longitude.value = coords.lng
        localStorage.setItem('lat', String(coords.lat))
        localStorage.setItem('lng', String(coords.lng))
        await updateLocation({ latitude: coords.lat, longitude: coords.lng })
      }
    } catch {
      // location update failed, ignore
    }
  }

  async function getIPCoords() {
    const services = [
      async () => {
        const res = await fetch('https://api.vore.top/api/IPdata')
        const data = await res.json()
        if (data.code === 200 && data.data) {
          return { lat: data.data.latitude, lng: data.data.longitude }
        }
        throw new Error('Invalid response')
      },
      async () => {
        const res = await fetch('https://ipapi.co/json/')
        const data = await res.json()
        if (data.latitude && data.longitude) {
          return { lat: data.latitude, lng: data.longitude }
        }
        throw new Error('Invalid response')
      },
    ]
    for (const fn of services) {
      try { return await fn() } catch { /* try next */ }
    }
    return null
  }

  async function doLogin(credentials) {
    const res = await loginApi(credentials)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    fetchIPLocation()
    return res
  }

  async function doRegister(data) {
    const res = await registerApi(data)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    fetchIPLocation()
    return res
  }

  async function doLogout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      user.value = null
      latitude.value = null
      longitude.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('lat')
      localStorage.removeItem('lng')
    }
  }

  function setUser(u) {
    user.value = u
    localStorage.setItem('user', JSON.stringify(u))
  }

  return { token, user, latitude, longitude, isLoggedIn, doLogin, doRegister, doLogout, setUser }
}, {
  persist: true,
})
