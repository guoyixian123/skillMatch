<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">← 返回</button>
      <h1 class="page-title">兴趣爱好</h1>
      <p class="page-subtitle">展示你的兴趣爱好</p>
    </header>

    <div class="brutal-card accent-yellow">
      <div class="section-title"><span class="dot" style="background:var(--color-yellow)"></span> 我的爱好</div>

      <div class="flex-wrap" style="margin-bottom:20px;">
        <div v-for="hobby in hobbies" :key="hobby.id" class="brutal-tag hobby" style="font-size:14px;padding:6px 14px;">
          {{ hobby.icon }} {{ hobby.hobbyName }}
          <span class="tag-remove" @click="handleDelete(hobby.id)">×</span>
        </div>
        <div v-if="!hobbies.length" style="color:#888;">暂无爱好，添加几个吧</div>
      </div>

      <div class="add-row">
        <el-input
          v-model="newIcon"
          placeholder="Emoji"
          maxlength="8"
          style="width:80px;flex-shrink:0;"
        />
        <el-input
          v-model="newName"
          placeholder="爱好名称"
          maxlength="32"
          class="add-input"
          @keyup.enter="handleAdd"
        />
        <button class="brutal-btn primary small" @click="handleAdd" :disabled="loading">
          添加
        </button>
      </div>

      <div style="margin-top:12px;font-size:12px;color:#888;">
        常用 Emoji: 🏀 ⚽ 🎸 🎹 🎮 📚 ✈️ 🍳 📷 🎨 🏃 🧘 🎿 🏄 🎬 🎵 ♟️ 🧩
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const hobbies = computed(() => userStore.hobbies)

const newName = ref('')
const newIcon = ref('')
const loading = ref(false)

onMounted(() => userStore.fetchHobbies())

async function handleAdd() {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入爱好名称')
    return
  }
  loading.value = true
  try {
    await userStore.doAddHobby({
      hobbyName: newName.value.trim(),
      icon: newIcon.value.trim() || '✨',
    })
    newName.value = ''
    newIcon.value = ''
    ElMessage.success('添加成功')
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleDelete(hobbyId) {
  await ElMessageBox.confirm('确定删除这个爱好吗？', '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await userStore.doDeleteHobby(hobbyId)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.tag-remove {
  cursor: pointer;
  margin-left: 4px;
  font-weight: 900;
  opacity: 0.6;
}
.tag-remove:hover { opacity: 1; }
.add-row {
  display: flex;
  gap: 8px;
}
.add-input { flex: 1; }
</style>
