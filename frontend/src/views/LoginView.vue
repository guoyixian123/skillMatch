<template>
  <div class="auth-page">
    <div class="auth-decor">
      <div class="decor-shape shape-1"></div>
      <div class="decor-shape shape-2"></div>
      <div class="decor-shape shape-3"></div>
      <div class="decor-shape shape-4"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card brutal-card accent-yellow">
        <div class="auth-logo">
          <span class="logo-icon">⚡</span>
          <h1>SkillMatch</h1>
          <p>技能匹配 · 轻社交</p>
        </div>

        <form class="auth-form" @submit.prevent="handleLogin">
          <!-- 账号 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-user input-icon"></i>
              <input
                v-model="form.username"
                class="brutal-input has-icon"
                placeholder="请输入账号"
                :class="{ 'input-error': errors.username }"
              />
            </div>
            <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
          </div>

          <!-- 密码 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-lock input-icon"></i>
              <input
                v-model="form.password"
                type="password"
                class="brutal-input has-icon"
                placeholder="请输入密码"
                :class="{ 'input-error': errors.password }"
                @keyup.enter="handleLogin"
              />
            </div>
            <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
          </div>

          <button type="submit" class="brutal-btn primary" style="width:100%;justify-content:center;margin-top:8px;" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="auth-footer">
          还没有账号？
          <router-link to="/register">立即注册 →</router-link>
        </div>
      </div>
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

const form = reactive({
  username: '',
  password: '',
})

const errors = reactive({
  username: '',
  password: '',
})

function validate() {
  errors.username = !form.username.trim() ? '请输入账号' : ''
  errors.password = !form.password ? '请输入密码' : ''
  return !errors.username && !errors.password
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  try {
    await authStore.doLogin({ userId: form.username, password: form.password })
    toast.add({ severity: 'success', summary: '成功', detail: '登录成功', life: 3000 })
    router.push('/discover')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FAFAFA;
  position: relative;
  overflow: hidden;
}
.auth-decor { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.decor-shape { position: absolute; border: 3px solid #1A1A1A; }
.shape-1 { top: -60px; right: -60px; width: 250px; height: 250px; background: var(--color-yellow); transform: rotate(15deg); }
.shape-2 { bottom: -40px; left: -40px; width: 200px; height: 200px; background: var(--color-pink); transform: rotate(-10deg); }
.shape-3 { top: 20%; left: 5%; width: 80px; height: 80px; background: var(--color-cyan); border-radius: 50%; }
.shape-4 { bottom: 25%; right: 8%; width: 60px; height: 60px; background: var(--color-purple); transform: rotate(45deg); }
.auth-container { position: relative; z-index: 1; width: 100%; max-width: 420px; padding: 20px; }
.auth-card { padding: 40px 32px; }
.auth-logo { text-align: center; margin-bottom: 32px; }
.auth-logo .logo-icon { font-size: 48px; }
.auth-logo h1 { font-size: 32px; font-weight: 900; text-transform: uppercase; letter-spacing: -1px; margin: 4px 0; }
.auth-logo p { font-size: 13px; color: #888; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
.auth-form { display: flex; flex-direction: column; gap: 16px; }

/* 表单字段 */
.form-field { display: flex; flex-direction: column; gap: 4px; }
.input-icon-wrap { position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: #aaa; font-size: 16px; z-index: 1; }
.has-icon { padding-left: 40px !important; }
.input-error { border-color: var(--color-pink) !important; }
.field-error {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-pink);
  padding-left: 2px;
}

.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #888;
  font-weight: 600;
}
</style>
