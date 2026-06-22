<template>
  <div class="auth-page">
    <div class="auth-decor">
      <div class="decor-circle decor-1"></div>
      <div class="decor-circle decor-2"></div>
      <div class="decor-triangle decor-3"></div>
      <div class="decor-squiggle decor-4"></div>
      <div class="decor-dot decor-5"></div>
      <div class="decor-dot decor-6"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card geo-card accent-violet">
        <div class="auth-logo">
          <svg class="auth-logo-svg" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <polygon points="28,3 10,26 22,26 18,45 38,22 26,22 30,3" fill="#FBBF24" stroke="#1E293B" stroke-width="2.5" stroke-linejoin="round"/>
            <circle cx="8" cy="8" r="4" fill="#F472B6" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="40" cy="8" r="4" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="8" cy="40" r="4" fill="#34D399" stroke="#1E293B" stroke-width="1.5"/>
            <circle cx="40" cy="40" r="4" fill="#8B5CF6" stroke="#1E293B" stroke-width="1.5"/>
          </svg>
          <h1><span>Skill</span><span style="color:#8B5CF6;">Match</span></h1>
          <p>技能匹配 · 轻社交</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <button type="submit" class="geo-btn primary login-btn" :disabled="loading">
              {{ loading ? '登录中...' : '登 录' }}
              <span class="btn-arrow">→</span>
            </button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from '@/utils/message'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.doLogin({ userId: form.username, password: form.password })
    ElMessage.success('登录成功')
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
  background: var(--color-bg);
  position: relative;
  overflow: hidden;
  background-image: radial-gradient(circle, #E2E8F0 1px, transparent 1px);
  background-size: 24px 24px;
}

/* Playful Geometric Decorations */
.auth-decor { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.decor-circle {
  position: absolute;
  border-radius: 9999px;
}
.decor-1 {
  top: -80px; right: -80px;
  width: 280px; height: 280px;
  background: var(--color-tertiary);
  opacity: 0.3;
  animation: floatSlow 8s ease-in-out infinite;
}
.decor-2 {
  bottom: -60px; left: -60px;
  width: 220px; height: 220px;
  background: var(--color-secondary);
  opacity: 0.2;
  animation: floatSlow 10s ease-in-out infinite reverse;
}
.decor-3 {
  top: 15%; left: 8%;
  width: 0; height: 0;
  border-left: 20px solid transparent;
  border-right: 20px solid transparent;
  border-bottom: 35px solid var(--color-quaternary);
  opacity: 0.5;
  animation: wiggle 4s ease-in-out infinite;
}
.decor-4 {
  bottom: 20%; right: 10%;
  width: 80px; height: 20px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 20'%3E%3Cpath d='M0 10 Q10 0 20 10 T40 10 T60 10 T80 10' fill='none' stroke='%238B5CF6' stroke-width='3'/%3E%3C/svg%3E") no-repeat center;
  opacity: 0.6;
  animation: floatSlow 6s ease-in-out infinite;
}
.decor-5 {
  top: 30%; right: 15%;
  width: 16px; height: 16px;
  background: var(--color-accent);
  opacity: 0.4;
  animation: popIn 2s ease-in-out infinite;
}
.decor-6 {
  bottom: 35%; left: 12%;
  width: 12px; height: 12px;
  background: var(--color-secondary);
  opacity: 0.4;
  animation: popIn 2.5s ease-in-out infinite 0.5s;
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
  padding: 20px;
}
.auth-card {
  padding: 40px 32px;
  animation: popEntry 0.5s var(--ease-bounce) both;
}
.auth-logo {
  text-align: center;
  margin-bottom: 32px;
}
.auth-logo-svg {
  width: 72px;
  height: 72px;
  margin-bottom: 12px;
  transition: transform 0.4s var(--ease-bounce);
  display: inline-block;
}
.auth-logo-svg:hover {
  transform: rotate(-12deg) scale(1.1);
}
.auth-logo h1 {
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
  margin: 4px 0;
  color: var(--color-fg);
}
.auth-logo p {
  font-size: 14px;
  color: var(--color-muted-fg);
  font-weight: 500;
}
.auth-form { margin-top: 8px; }
.login-btn {
  width: 100%;
  justify-content: center;
  font-size: 16px;
  padding: 14px 24px;
}
.btn-arrow {
  display: inline-block;
  transition: transform 0.3s var(--ease-bounce);
}
.login-btn:hover .btn-arrow {
  transform: translateX(4px);
}
.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--color-muted-fg);
  font-weight: 500;
}
.auth-footer a {
  color: var(--color-accent);
  font-weight: 700;
}

@keyframes floatSlow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}
@keyframes wiggle {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(8deg); }
  75% { transform: rotate(-8deg); }
}
@keyframes popIn {
  0%, 100% { transform: scale(1); opacity: 0.4; }
  50% { transform: scale(1.5); opacity: 0.7; }
}
@keyframes popEntry {
  0% { opacity: 0; transform: scale(0.8) translateY(20px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
