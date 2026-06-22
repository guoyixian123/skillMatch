<template>
  <div class="auth-page">
    <div class="auth-decor">
      <div class="decor-circle decor-1"></div>
      <div class="decor-circle decor-2"></div>
      <div class="decor-dot decor-3"></div>
      <div class="decor-dot decor-4"></div>
      <div class="decor-triangle decor-5"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card geo-card accent-pink">
        <div class="auth-logo">
          <div class="logo-circle">
            <span class="logo-icon">✨</span>
          </div>
          <h1>加入我们</h1>
          <p>创建你的技能名片</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="auth-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="账号 (4-16位字母/数字/下划线)"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="昵称 (1-16字符)"
              :prefix-icon="UserFilled"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码 (6-20字符)"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item>
            <button type="submit" class="geo-btn primary login-btn" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
              <span class="btn-arrow">→</span>
            </button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          已有账号？
          <router-link to="/login">立即登录</router-link>
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
import { User, UserFilled, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,16}$/, message: '4-16位字母/数字/下划线', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 16, message: '1-16字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '6-20字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.doRegister({
      userId: form.username,
      name: form.nickname,
      password: form.password,
    })
    ElMessage.success('注册成功')
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

.auth-decor { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.decor-circle {
  position: absolute;
  border-radius: 9999px;
}
.decor-1 {
  top: -70px; left: -70px;
  width: 260px; height: 260px;
  background: var(--color-quaternary);
  opacity: 0.25;
  animation: floatSlow 9s ease-in-out infinite;
}
.decor-2 {
  bottom: -50px; right: -50px;
  width: 200px; height: 200px;
  background: var(--color-tertiary);
  opacity: 0.2;
  animation: floatSlow 7s ease-in-out infinite reverse;
}
.decor-3 {
  top: 25%; right: 12%;
  width: 14px; height: 14px;
  background: var(--color-accent);
  border-radius: 9999px;
  opacity: 0.5;
  animation: popIn 2s ease-in-out infinite;
}
.decor-4 {
  bottom: 30%; left: 10%;
  width: 10px; height: 10px;
  background: var(--color-secondary);
  border-radius: 9999px;
  opacity: 0.5;
  animation: popIn 2.5s ease-in-out infinite 0.3s;
}
.decor-5 {
  top: 60%; right: 8%;
  width: 0; height: 0;
  border-left: 14px solid transparent;
  border-right: 14px solid transparent;
  border-bottom: 24px solid var(--color-tertiary);
  opacity: 0.4;
  animation: wiggle 5s ease-in-out infinite;
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
.logo-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: var(--color-secondary);
  border-radius: 9999px;
  border: 2px solid var(--color-fg);
  box-shadow: 4px 4px 0 var(--color-fg);
  margin-bottom: 12px;
  transition: transform 0.4s var(--ease-bounce);
}
.logo-circle:hover {
  transform: rotate(15deg) scale(1.1);
}
.auth-logo .logo-icon { font-size: 32px; }
.auth-logo h1 {
  font-family: var(--font-heading);
  font-size: 28px;
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
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.5); opacity: 0.8; }
}
@keyframes popEntry {
  0% { opacity: 0; transform: scale(0.8) translateY(20px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
