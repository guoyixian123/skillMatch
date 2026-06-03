import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '@/api/auth'
import { updateLocation } from '@/api/user'

function safeParseUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(safeParseUser())
  const latitude = ref(parseFloat(localStorage.getItem('lat') || '') || null)
  const longitude = ref(parseFloat(localStorage.getItem('lng') || '') || null)
  const locationStatus = ref('idle') // idle | loading | granted | denied | error

  const isLoggedIn = computed(() => !!token.value)

  function requestBrowserLocation() {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        const err = new Error('浏览器不支持定位')
        err.code = -1
        reject(err)
        return
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          })
        },
        (geoErr) => {
          let message = '定位失败'
          switch (geoErr.code) {
            case geoErr.PERMISSION_DENIED: message = '用户拒绝了定位请求'; break
            case geoErr.POSITION_UNAVAILABLE: message = '位置信息不可用'; break
            case geoErr.TIMEOUT: message = '定位请求超时'; break
          }
          const err = new Error(message)
          err.code = geoErr.code
          reject(err)
        },
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
      )
    })
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

  async function fetchLocation() {
    locationStatus.value = 'loading'
    try {
      const coords = await requestBrowserLocation()
      latitude.value = coords.lat
      longitude.value = coords.lng
      locationStatus.value = 'granted'
      localStorage.setItem('lat', String(coords.lat))
      localStorage.setItem('lng', String(coords.lng))
      await updateLocation({ latitude: coords.lat, longitude: coords.lng })
      return coords
    } catch (browserErr) {
      // browser geolocation failed, try IP fallback
      try {
        const coords = await getIPCoords()
        if (coords) {
          latitude.value = coords.lat
          longitude.value = coords.lng
          locationStatus.value = browserErr.code === 1 ? 'denied' : 'error'
          localStorage.setItem('lat', String(coords.lat))
          localStorage.setItem('lng', String(coords.lng))
          await updateLocation({ latitude: coords.lat, longitude: coords.lng })
          return coords
        }
      } catch {
        locationStatus.value = 'error'
      }
    }
    return null
  }

  async function doLogin(credentials) {
    const res = await loginApi(credentials)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    // 等待位置获取完成，确保后续页面请求时后端已有位置数据
    await fetchLocation()
    return res
  }

  async function doRegister(data) {
    const res = await registerApi(data)
    token.value = res.data.token
    user.value = res.data.user
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data.user))
    // 等待位置获取完成，确保后续页面请求时后端已有位置数据
    await fetchLocation()
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
      locationStatus.value = 'idle'
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

  return {
    token, user, latitude, longitude, locationStatus, isLoggedIn,
    doLogin, doRegister, doLogout, setUser,
    fetchLocation, requestBrowserLocation,
  }
})
