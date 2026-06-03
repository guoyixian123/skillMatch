import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import Aura from '@primeuix/themes/aura'
import 'primeicons/primeicons.css'
import App from './App.vue'
import router from './router'
import './assets/global.css'

// 全局提供 echarts 给 vue-echarts 使用
import * as echarts from 'echarts'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: { prefix: 'p', darkModeSelector: 'light', cssLayer: false },
  },
  ripple: false,
})
app.use(ToastService)
app.use(ConfirmationService)

// 全局注入 echarts，确保 vue-echarts 与 registerMap 使用同一实例
app.provide('ec', echarts)
app.config.globalProperties.$echarts = echarts

app.mount('#app')
