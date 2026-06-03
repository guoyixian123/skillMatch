<template>
  <div class="auth-page">
    <div class="auth-decor">
      <div class="decor-shape shape-1"></div>
      <div class="decor-shape shape-2"></div>
      <div class="decor-shape shape-3"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card brutal-card accent-cyan">
        <div class="auth-logo">
          <span class="logo-icon">⚡</span>
          <h1>加入我们</h1>
          <p>创建你的技能名片</p>
        </div>

        <form class="auth-form" @submit.prevent="handleRegister">
          <!-- 账号 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-user input-icon"></i>
              <input
                v-model="form.username"
                class="brutal-input has-icon"
                placeholder="账号 (4-16位字母/数字/下划线)"
                :class="{ 'input-error': errors.username }"
                @blur="validateField('username')"
              />
            </div>
            <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
          </div>

          <!-- 昵称 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-user-edit input-icon"></i>
              <input
                v-model="form.nickname"
                class="brutal-input has-icon"
                placeholder="昵称 (1-16字符)"
                :class="{ 'input-error': errors.nickname }"
                @blur="validateField('nickname')"
              />
            </div>
            <span v-if="errors.nickname" class="field-error">{{ errors.nickname }}</span>
          </div>

          <!-- 密码 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-lock input-icon"></i>
              <input
                v-model="form.password"
                type="password"
                class="brutal-input has-icon"
                placeholder="密码 (6-20字符)"
                :class="{ 'input-error': errors.password }"
                @blur="validateField('password')"
              />
            </div>
            <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
          </div>

          <!-- 确认密码 -->
          <div class="form-field">
            <div class="input-icon-wrap">
              <i class="pi pi-lock input-icon"></i>
              <input
                v-model="form.confirmPassword"
                type="password"
                class="brutal-input has-icon"
                placeholder="确认密码"
                :class="{ 'input-error': errors.confirmPassword }"
                @blur="validateField('confirmPassword')"
                @keyup.enter="handleRegister"
              />
            </div>
            <span v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</span>
          </div>

          <button type="submit" class="brutal-btn dark" style="width:100%;justify-content:center;margin-top:4px;" :disabled="loading">
            {{ loading ? '注册中...' : '注 册' }}
          </button>
        </form>

        <div class="auth-footer">
          已有账号？
          <router-link to="/login">立即登录 →</router-link>
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
  nickname: '',
  password: '',
  confirmPassword: '',
})

const errors = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const usernameRe = /^[a-zA-Z0-9_]{4,16}$/

function validateField(field) {
  switch (field) {
    case 'username':
      if (!form.username.trim()) errors.username = '请输入账号'
      else if (!usernameRe.test(form.username)) errors.username = '4-16位字母/数字/下划线'
      else errors.username = ''
      break
    case 'nickname':
      if (!form.nickname.trim()) errors.nickname = '请输入昵称'
      else if (form.nickname.length > 16) errors.nickname = '1-16字符'
      else errors.nickname = ''
      break
    case 'password':
      if (!form.password) errors.password = '请输入密码'
      else if (form.password.length < 6 || form.password.length > 20) errors.password = '6-20字符'
      else errors.password = ''
      if (form.confirmPassword) validateField('confirmPassword')
      break
    case 'confirmPassword':
      if (!form.confirmPassword) errors.confirmPassword = '请确认密码'
      else if (form.confirmPassword !== form.password) errors.confirmPassword = '两次密码输入不一致'
      else errors.confirmPassword = ''
      break
  }
}

function validateAll() {
  validateField('username')
  validateField('nickname')
  validateField('password')
  validateField('confirmPassword')
  return !Object.values(errors).some(Boolean)
}

async function handleRegister() {
  if (!validateAll()) return
  loading.value = true
  try {
    await authStore.doRegister({
      userId: form.username,
      name: form.nickname,
      password: form.password,
    })
    toast.add({ severity: 'success', summary: '成功', detail: '注册成功', life: 3000 })
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
.shape-1 { top: -80px; left: -80px; width: 300px; height: 300px; background: var(--color-cyan); transform: rotate(20deg); }
.shape-2 { bottom: -50px; right: -50px; width: 220px; height: 220px; background: var(--color-yellow); transform: rotate(-15deg); }
.shape-3 { bottom: 30%; right: 10%; width: 70px; height: 70px; background: var(--color-pink); border-radius: 50%; }
.auth-container { position: relative; z-index: 1; width: 100%; max-width: 420px; padding: 20px; }
.auth-card { padding: 40px 32px; }
.auth-logo { text-align: center; margin-bottom: 28px; }
.auth-logo .logo-icon { font-size: 48px; }
.auth-logo h1 { font-size: 28px; font-weight: 900; text-transform: uppercase; letter-spacing: -1px; margin: 4px 0; }
.auth-logo p { font-size: 13px; color: #888; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
.auth-form { display: flex; flex-direction: column; gap: 14px; }

.form-field { display: flex; flex-direction: column; gap: 4px; }
.input-icon-wrap { position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: #aaa; font-size: 16px; z-index: 1; }
.has-icon { padding-left: 40px !important; }
.input-error { border-color: var(--color-pink) !important; }
.field-error { font-size: 12px; font-weight: 700; color: var(--color-pink); padding-left: 2px; }

.auth-footer { text-align: center; margin-top: 20px; font-size: 14px; color: #888; font-weight: 600; }
</style>
