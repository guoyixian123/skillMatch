<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <i class="pi pi-arrow-left"></i> 返回
      </button>
      <h1 class="page-title">编辑资料</h1>
    </header>

    <!-- Avatar Upload -->
    <div class="brutal-card accent-yellow" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 头像</div>
      <div style="display:flex;align-items:center;gap:20px;">
        <img
          :src="avatarPreview || authStore.user?.avatarUrl || getDefaultAvatar(authStore.user?.userId || authStore.user?.name)"
          class="brutal-avatar xl"
        />
        <div>
          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style="display:none;"
            @change="handleAvatarChange"
          />
          <button class="brutal-btn outline small" type="button" @click="$refs.fileInput?.click()">选择图片</button>
          <div style="font-size:12px;color:#888;margin-top:8px;">JPG/PNG/WebP, ≤10MB</div>
          <button v-if="avatarFile" class="brutal-btn primary small" style="margin-top:8px;" @click="uploadAvatar" :disabled="avatarUploading">
            {{ avatarUploading ? '上传中...' : '上传头像' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Profile Form -->
    <div class="brutal-card accent-cyan">
      <div class="section-title"><span class="dot" style="background:var(--color-cyan)"></span> 基本信息</div>
      <form @submit.prevent="handleSubmit" class="edit-form">
        <div class="form-field">
          <label class="field-label">昵称</label>
          <input v-model="form.name" class="brutal-input" placeholder="你的昵称" maxlength="16" />
          <span v-if="errors.name" class="field-error">{{ errors.name }}</span>
        </div>

        <div class="form-field">
          <label class="field-label">个性签名</label>
          <input v-model="form.signature" class="brutal-input" placeholder="一句话介绍自己" maxlength="64" />
          <span v-if="errors.signature" class="field-error">{{ errors.signature }}</span>
        </div>

        <div class="form-field">
          <label class="field-label">个人简介</label>
          <textarea v-model="form.bio" class="brutal-input" rows="4" placeholder="介绍一下你自己..." maxlength="500"></textarea>
          <span v-if="errors.bio" class="field-error">{{ errors.bio }}</span>
          <div class="field-count">{{ form.bio.length }}/500</div>
        </div>

        <div class="form-field">
          <label class="field-label">联系方式</label>
          <input v-model="form.contactInfo" class="brutal-input" placeholder="交换同意后对方可见" maxlength="64" />
          <span v-if="errors.contactInfo" class="field-error">{{ errors.contactInfo }}</span>
        </div>

        <div style="margin-top:8px;">
          <button type="submit" class="brutal-btn primary" :disabled="submitting">
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { getDefaultAvatar } from '@/utils/avatar'
import { useToast } from 'primevue/usetoast'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const toast = useToast()

const fileInput = ref(null)
const submitting = ref(false)

const form = reactive({
  name: '', signature: '', bio: '', contactInfo: '',
})

const errors = reactive({
  name: '', signature: '', bio: '', contactInfo: '',
})

const avatarFile = ref(null)
const avatarPreview = ref(null)
const avatarUploading = ref(false)

onMounted(async () => {
  const userId = authStore.user?.userId
  if (userId) {
    await userStore.fetchProfile(userId)
    if (userStore.profile) {
      form.name = userStore.profile.name || ''
      form.signature = userStore.profile.signature || ''
      form.bio = userStore.profile.bio || ''
      form.contactInfo = userStore.profile.contactInfo || ''
    }
  }
})

function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (avatarPreview.value && avatarPreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreview.value)
  }
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file)
}

async function uploadAvatar() {
  if (!avatarFile.value) return
  avatarUploading.value = true
  try {
    const res = await userStore.doUploadAvatar(avatarFile.value)
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '头像已更新', life: 3000 })
    if (avatarPreview.value && avatarPreview.value.startsWith('blob:')) {
      URL.revokeObjectURL(avatarPreview.value)
    }
    avatarPreview.value = authStore.user?.avatarUrl || ''
    avatarFile.value = null
  } catch { /* handled */ } finally { avatarUploading.value = false }
}

onUnmounted(() => {
  if (avatarPreview.value && avatarPreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarPreview.value)
  }
})

function validate() {
  errors.name = form.name && form.name.length > 16 ? '不超过16字符' : ''
  errors.signature = form.signature && form.signature.length > 64 ? '不超过64字符' : ''
  errors.bio = form.bio && form.bio.length > 500 ? '不超过500字符' : ''
  errors.contactInfo = form.contactInfo && form.contactInfo.length > 64 ? '不超过64字符' : ''
  return !Object.values(errors).some(Boolean)
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const res = await userStore.doUpdateProfile({
      name: form.name, signature: form.signature, bio: form.bio, contactInfo: form.contactInfo,
    })
    authStore.setUser({ ...authStore.user, name: form.name })
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '资料已更新', life: 3000 })
    router.back()
  } catch { /* handled */ } finally { submitting.value = false }
}
</script>

<style scoped>
.edit-form { display: flex; flex-direction: column; gap: 16px; }
.form-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #555; }
.field-error { font-size: 12px; font-weight: 700; color: var(--color-pink); }
.field-count { text-align: right; font-size: 12px; color: #aaa; }
</style>
