<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        ← 返回
      </button>
      <h1 class="page-title">编辑资料</h1>
    </header>

    <!-- Avatar Upload -->
    <div class="brutal-card accent-yellow" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 头像</div>
      <div style="display:flex;align-items:center;gap:20px;">
        <img
          :src="avatarPreview || authStore.user?.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
          class="brutal-avatar xl"
        />
        <div>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
            accept="image/jpeg,image/png,image/webp"
          >
            <button class="brutal-btn outline small" type="button">选择图片</button>
          </el-upload>
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
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="昵称" prop="name">
          <el-input v-model="form.name" placeholder="你的昵称" maxlength="16" show-word-limit />
        </el-form-item>

        <el-form-item label="个性签名" prop="signature">
          <el-input v-model="form.signature" placeholder="一句话介绍自己" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="form.bio"
            type="textarea"
            :rows="4"
            placeholder="介绍一下你自己..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contactInfo">
          <el-input v-model="form.contactInfo" placeholder="交换同意后对方可见" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item>
          <button type="submit" class="brutal-btn primary" :disabled="submitting">
            {{ submitting ? '保存中...' : '保存修改' }}
          </button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  name: '',
  signature: '',
  bio: '',
  contactInfo: '',
})

const rules = {
  name: [
    { min: 1, max: 16, message: '1-16字符', trigger: 'blur' },
  ],
  signature: [
    { max: 64, message: '不超过64字符', trigger: 'blur' },
  ],
  bio: [
    { max: 500, message: '不超过500字符', trigger: 'blur' },
  ],
  contactInfo: [
    { max: 64, message: '不超过64字符', trigger: 'blur' },
  ],
}

const avatarFile = ref(null)
const avatarPreview = ref(null)
const avatarUploading = ref(false)

onMounted(async () => {
  const userId = authStore.user?.id || authStore.user?.userId
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

function handleAvatarChange(file) {
  avatarFile.value = file.raw
  avatarPreview.value = URL.createObjectURL(file.raw)
}

async function uploadAvatar() {
  if (!avatarFile.value) return
  avatarUploading.value = true
  try {
    await userStore.doUploadAvatar(avatarFile.value)
    // update auth store avatar too
    ElMessage.success('头像已更新')
    avatarFile.value = null
  } catch { /* handled */ } finally {
    avatarUploading.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userStore.doUpdateProfile({
      name: form.name,
      signature: form.signature,
      bio: form.bio,
      contactInfo: form.contactInfo,
    })
    // sync to auth store
    authStore.setUser({ ...authStore.user, name: form.name })
    ElMessage.success('资料已更新')
    router.back()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}
</script>
