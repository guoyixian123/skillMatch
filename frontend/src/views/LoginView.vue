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
            <button type="submit" class="brutal-btn primary" style="width:100%;justify-content:center;" :disabled="loading">
              {{ loading ? '登录中...' : '登 录' }}
            </button>
          </el-form-item>
        </el-form>

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
import { ElMessage } from 'element-plus'
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
  background: #FAFAFA;
  position: relative;
  overflow: hidden;
}
/* Pop Art Decor */
.auth-decor { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.decor-shape {
  position: absolute;
  border: 3px solid #1A1A1A;
}
.shape-1 {
  top: -60px; right: -60px;
  width: 250px; height: 250px;
  background: var(--color-yellow);
  transform: rotate(15deg);
}
.shape-2 {
  bottom: -40px; left: -40px;
  width: 200px; height: 200px;
  background: var(--color-pink);
  transform: rotate(-10deg);
}
.shape-3 {
  top: 20%; left: 5%;
  width: 80px; height: 80px;
  background: var(--color-cyan);
  border-radius: 50%;
}
.shape-4 {
  bottom: 25%; right: 8%;
  width: 60px; height: 60px;
  background: var(--color-purple);
  transform: rotate(45deg);
}
.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}
.auth-card { padding: 40px 32px; }
.auth-logo {
  text-align: center;
  margin-bottom: 32px;
}
.auth-logo .logo-icon { font-size: 48px; }
.auth-logo h1 {
  font-size: 32px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: -1px;
  margin: 4px 0;
}
.auth-logo p {
  font-size: 13px;
  color: #888;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 2px;
}
.auth-form { margin-top: 8px; }
.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #888;
  font-weight: 600;
}
</style>
