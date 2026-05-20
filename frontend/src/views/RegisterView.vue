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
            <button type="submit" class="brutal-btn dark" style="width:100%;justify-content:center;" :disabled="loading">
              {{ loading ? '注册中...' : '注 册' }}
            </button>
          </el-form-item>
        </el-form>

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
import { ElMessage } from 'element-plus'
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
    // backend DTO uses userId and name fields
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
  background: #FAFAFA;
  position: relative;
  overflow: hidden;
}
.auth-decor { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.decor-shape {
  position: absolute;
  border: 3px solid #1A1A1A;
}
.shape-1 {
  top: -80px; left: -80px;
  width: 300px; height: 300px;
  background: var(--color-cyan);
  transform: rotate(20deg);
}
.shape-2 {
  bottom: -50px; right: -50px;
  width: 220px; height: 220px;
  background: var(--color-yellow);
  transform: rotate(-15deg);
}
.shape-3 {
  bottom: 30%; right: 10%;
  width: 70px; height: 70px;
  background: var(--color-pink);
  border-radius: 50%;
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
  font-size: 28px;
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
