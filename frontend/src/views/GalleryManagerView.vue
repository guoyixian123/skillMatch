<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">← 返回</button>
      <h1 class="page-title">个人相册</h1>
      <p class="page-subtitle">上传照片展示你的生活</p>
    </header>

    <!-- Upload -->
    <div class="brutal-card accent-green" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-green)"></span> 上传图片</div>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept="image/jpeg,image/png,image/webp"
        drag
      >
        <div class="upload-area">
          <div style="font-size:40px;">📸</div>
          <div style="font-weight:700;margin-top:8px;">点击或拖拽上传</div>
          <div style="font-size:12px;color:#888;">JPG/PNG/WebP, ≤10MB</div>
        </div>
      </el-upload>
      <div v-if="pendingFile" style="margin-top:12px;display:flex;align-items:center;gap:12px;">
        <span style="font-weight:700;font-size:13px;">{{ pendingFile.name }}</span>
        <button class="brutal-btn primary small" @click="doUpload" :disabled="uploading">
          {{ uploading ? '上传中...' : '确认上传' }}
        </button>
      </div>
    </div>

    <!-- Gallery Grid -->
    <div class="brutal-grid-3">
      <div v-for="img in gallery" :key="img.id" class="gallery-item brutal-card">
        <img :src="img.imageUrl" class="gallery-image" />
        <div class="gallery-overlay">
          <button class="brutal-btn danger small" @click="handleDelete(img.id)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="!gallery.length && !uploading" class="empty-block" style="margin-top:20px;">
      <div class="icon">🖼️</div>
      <div style="font-weight:800;">相册为空</div>
      <div style="color:#888;">上传你的第一张照片吧</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const gallery = computed(() => userStore.gallery)

const pendingFile = ref(null)
const uploading = ref(false)

onMounted(() => userStore.fetchGallery())

function handleFileChange(file) {
  pendingFile.value = file.raw
}

async function doUpload() {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    await userStore.doUploadGallery(pendingFile.value)
    ElMessage.success('上传成功')
    pendingFile.value = null
  } catch { /* handled */ } finally {
    uploading.value = false
  }
}

async function handleDelete(imageId) {
  await ElMessageBox.confirm('确定删除这张图片吗？', '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await userStore.doDeleteGalleryImage(imageId)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.upload-area {
  padding: 40px;
  text-align: center;
}
.gallery-item {
  padding: 0;
  overflow: hidden;
  position: relative;
}
.gallery-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  display: block;
}
.gallery-overlay {
  position: absolute;
  top: 0;
  right: 0;
  padding: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
.gallery-item:hover .gallery-overlay {
  opacity: 1;
}
</style>
