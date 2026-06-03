<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1>SkillMatch<br/><span class="login-badge">Admin</span></h1>
        <p>管理控制台</p>
      </div>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="gf">
          <label class="fl">账号</label>
          <input v-model="form.userId" class="nb-input login-input" placeholder="管理员账号" autocomplete="username" />
        </div>
        <div class="gf">
          <label class="fl">密码</label>
          <input v-model="form.password" type="password" class="nb-input login-input" placeholder="密码" autocomplete="current-password" @keyup.enter="handleLogin" />
        </div>
        <button type="submit" class="nb-btn dark login-btn" :disabled="loading">
          {{ loading ? '验证中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const loading = ref(false)

const form = reactive({ userId: '', password: '' })

async function handleLogin() {
  if (!form.userId || !form.password) {
    toast.add({ severity:'warn', summary:'必填', detail:'请输入账号密码', life:3000 }); return
  }
  loading.value = true
  try {
    await authStore.login({ userId: form.userId, password: form.password })
    router.push('/dashboard')
  } catch {
    // 错误已在 request 拦截器中统一提示，此处仅静默
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--yellow);
  background-image: radial-gradient(circle, #000 1px, transparent 1px);
  background-size: 30px 30px;
}
.login-card {
  width: 420px; max-width: 90vw; background: #fff;
  border: var(--border-thick); box-shadow: 12px 12px 0 #000;
  padding: 40px 32px;
}
.login-header { text-align: center; margin-bottom: 32px; }
.login-header h1 {
  font-family: var(--font-headline); font-size: 36px; font-weight: 900;
  text-transform: uppercase; line-height: 1; color: #000;
}
.login-badge { background: var(--yellow); padding: 0 6px; display: inline-block; margin-top: 4px; }
.login-header p { font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 3px; opacity: 0.4; margin-top: 8px; }

.login-form { display: flex; flex-direction: column; gap: 16px; }
.gf { display: flex; flex-direction: column; gap: 4px; }
.fl { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
.login-input { padding: 12px 14px; font-size: 15px; font-weight: 700; }
.login-btn { width: 100%; justify-content: center; padding: 14px; font-size: 15px; }
</style>
