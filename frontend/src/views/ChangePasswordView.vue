<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <i class="pi pi-arrow-left"></i> 返回
      </button>
      <h1 class="page-title">修改密码</h1>
    </header>

    <div class="brutal-card" style="max-width:480px;">
      <form @submit.prevent="handleSubmit" class="pwd-form">
        <div class="form-field">
          <label class="field-label">当前密码</label>
          <input v-model="form.oldPassword" type="password" class="brutal-input" placeholder="输入当前密码" />
          <span v-if="errors.oldPassword" class="field-error">{{ errors.oldPassword }}</span>
        </div>

        <div class="form-field">
          <label class="field-label">新密码</label>
          <input v-model="form.newPassword" type="password" class="brutal-input" placeholder="6-20字符" />
          <span v-if="errors.newPassword" class="field-error">{{ errors.newPassword }}</span>
        </div>

        <div class="form-field">
          <label class="field-label">确认新密码</label>
          <input v-model="form.confirmPassword" type="password" class="brutal-input" placeholder="再次输入新密码" />
          <span v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</span>
        </div>

        <div style="margin-top:8px;">
          <button type="submit" class="brutal-btn primary" :disabled="submitting">
            {{ submitting ? '修改中...' : '修改密码' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from 'primevue/usetoast'

const router = useRouter()
const userStore = useUserStore()
const toast = useToast()
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const errors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

function validate() {
  errors.oldPassword = !form.oldPassword ? '请输入当前密码' : ''
  errors.newPassword = !form.newPassword ? '请输入新密码'
    : (form.newPassword.length < 6 || form.newPassword.length > 20) ? '6-20字符'
    : ''
  errors.confirmPassword = !form.confirmPassword ? '请确认新密码'
    : form.confirmPassword !== form.newPassword ? '两次密码不一致'
    : ''
  return !Object.values(errors).some(Boolean)
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const res = await userStore.doChangePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '密码已修改，请重新登录', life: 3000 })
    router.push('/login')
  } catch { /* handled */ } finally { submitting.value = false }
}
</script>

<style scoped>
.pwd-form { display: flex; flex-direction: column; gap: 16px; }
.form-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #555; }
.field-error { font-size: 12px; font-weight: 700; color: var(--color-pink); }
</style>
