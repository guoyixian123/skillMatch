<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <i class="pi pi-arrow-left"></i> 返回
      </button>
      <h1 class="page-title">个人相册</h1>
      <p class="page-subtitle">上传照片展示你的生活</p>
    </header>

    <!-- Upload -->
    <div class="brutal-card accent-green" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-green)"></span> 上传图片</div>
      <div class="upload-area" @click="$refs.galleryInput?.click()" @dragover.prevent @drop.prevent="handleDrop">
        <i class="pi pi-camera" style="font-size:40px;"></i>
        <div style="font-weight:700;margin-top:8px;">点击或拖拽上传</div>
        <div style="font-size:12px;color:#888;">JPG/PNG/WebP, ≤10MB</div>
      </div>
      <input ref="galleryInput" type="file" accept="image/jpeg,image/png,image/webp" style="display:none;" @change="handleFileChange" />
      <div v-if="pendingFile" style="margin-top:12px;display:flex;align-items:center;gap:12px;">
        <span style="font-weight:700;font-size:13px;">{{ pendingFile.name }}</span>
        <button class="brutal-btn primary small" @click="doUpload" :disabled="uploading">
          {{ uploading ? '上传中...' : '确认上传' }}
        </button>
      </div>
    </div>

    <!-- Gallery Grid -->
    <div v-if="gallery.length === 0" class="empty-block">
      <div class="icon"><i class="pi pi-image" style="font-size:48px;"></i></div>
      <div style="font-weight:800;font-size:18px;">暂无图片</div>
    </div>

    <div v-else class="brutal-grid-3">
      <div v-for="img in gallery" :key="img.id" class="gallery-item">
        <img :src="img.imageUrl" class="gallery-img" />
        <div class="gallery-overlay">
          <button class="overlay-btn" @click="handleDelete(img)">
            <i class="pi pi-trash"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'

const userStore = useUserStore()
const toast = useToast()
const confirmDialog = useConfirm()

const galleryInput = ref(null)
const pendingFile = ref(null)
const uploading = ref(false)

const gallery = computed(() => userStore.gallery || [])

function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) setPendingFile(file)
}

function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (file) setPendingFile(file)
}

function setPendingFile(file) {
  if (!file.type.startsWith('image/')) {
    toast.add({ severity: 'warn', summary: '提示', detail: '请选择图片文件', life: 3000 })
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.add({ severity: 'warn', summary: '提示', detail: '图片不能超过10MB', life: 3000 })
    return
  }
  pendingFile.value = file
}

async function doUpload() {
  if (!pendingFile.value) return
  uploading.value = true
  try {
    const res = await userStore.doUploadGallery(pendingFile.value)
    toast.add({ severity: 'success', summary: '成功', detail: res.message || '上传成功', life: 3000 })
    pendingFile.value = null
  } catch { /* handled */ } finally { uploading.value = false }
}

async function handleDelete(img) {
  confirmDialog.require({
    message: '确定删除这张图片吗？',
    header: '确认',
    rejectLabel: '取消',
    acceptLabel: '删除',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        const res = await userStore.doDeleteGalleryImage(img.id)
        toast.add({ severity: 'success', summary: '成功', detail: res.message || '已删除', life: 3000 })
      } catch { /* handled */ }
    },
  })
}

onMounted(() => { userStore.fetchGallery() })
</script>

<style scoped>
.upload-area {
  border: 2px dashed #1A1A1A; padding: 32px; text-align: center;
  cursor: pointer; transition: background 0.15s;
}
.upload-area:hover { background: #f0fff0; }

.gallery-item {
  position: relative; overflow: hidden; border: 2px solid #1A1A1A;
}
.gallery-img { width: 100%; aspect-ratio: 1; object-fit: cover; display: block; }
.gallery-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s;
}
.gallery-item:hover .gallery-overlay { opacity: 1; }
.overlay-btn {
  padding: 8px 12px; border: 2px solid #fff; background: var(--color-pink);
  color: #fff; cursor: pointer; font-size: 16px;
}
</style>
