<template>
  <div class="page-container">
    <header class="page-header">
      <button class="geo-btn outline small" @click="$router.back()" style="margin-bottom:12px;"><el-icon><ArrowLeft /></el-icon> 返回</button>
      <h1 class="page-title">个人相册</h1>
      <p class="page-subtitle">上传照片展示你的生活</p>
    </header>

    <!-- Upload -->
    <div class="geo-card accent-green" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-quaternary)"></span> 上传图片</div>
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        accept="image/jpeg,image/png,image/webp"
        drag
      >
        <div class="upload-area">
          <div class="upload-icon-circle"><el-icon :size="28" color="#065F46"><Camera /></el-icon></div>
          <div style="font-weight:600;margin-top:8px;">点击或拖拽上传</div>
          <div style="font-size:12px;color:var(--color-muted-fg);">JPG/PNG/WebP, ≤10MB</div>
        </div>
      </el-upload>
      <div v-if="pendingFile" style="margin-top:12px;display:flex;align-items:center;gap:12px;">
        <span style="font-weight:600;font-size:13px;">{{ pendingFile.name }}</span>
        <button class="geo-btn primary small" @click="doUpload" :disabled="uploading">
          {{ uploading ? '上传中...' : '确认上传' }}
        </button>
      </div>
    </div>

    <!-- Gallery Grid -->
    <div class="geo-grid-3">
      <div v-for="img in gallery" :key="img.id" class="gallery-item geo-card">
        <img :src="img.imageUrl" class="gallery-image" />
        <div class="gallery-overlay">
          <button class="geo-btn danger small" @click="handleDelete(img.id)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="!gallery.length && !uploading" class="empty-block" style="margin-top:20px;">
      <div class="icon"><el-icon :size="48"><Picture /></el-icon></div>
      <div style="font-weight:700;">相册为空</div>
      <div style="color:var(--color-muted-fg);">上传你的第一张照片吧</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { ElMessage } from '@/utils/message'
import { ArrowLeft, Camera, Picture } from '@element-plus/icons-vue'

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
    const res = await userStore.doUploadGallery(pendingFile.value)
    ElMessage.success(res.message || '上传成功')
    pendingFile.value = null
  } catch { /* handled */ } finally {
    uploading.value = false
  }
}

async function handleDelete(imageId) {
  try {
    await ElMessageBox.confirm('确定删除这张图片吗？', '确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      showClose: false,
    })
    const res = await userStore.doDeleteGalleryImage(imageId)
    ElMessage.success(res.message || '已删除')
  } catch { /* cancel or error */ }
}
</script>

<style scoped>
.upload-area {
  padding: 40px;
  text-align: center;
}
.upload-icon-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: #D1FAE5;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-fg);
  margin-bottom: 4px;
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
  transition: opacity 0.3s;
}
.gallery-item:hover .gallery-overlay {
  opacity: 1;
}
</style>
