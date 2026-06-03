<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <i class="pi pi-arrow-left"></i> 返回
      </button>
      <h1 class="page-title">兴趣爱好</h1>
      <p class="page-subtitle">从标签池中选取你的兴趣爱好</p>
    </header>

    <!-- Selected Hobbies -->
    <div class="selected-area">
      <div class="selected-label">已选 {{ hobbies.length }} 个</div>
      <div class="selected-tags">
        <TransitionGroup name="tag">
          <span v-for="hobby in hobbies" :key="hobby.id" class="selected-tag" @click="handleRemove(hobby)">
            {{ hobby.icon }} {{ hobby.hobbyName }}
            <span class="tag-x">&times;</span>
          </span>
        </TransitionGroup>
        <span v-if="!hobbies.length" class="empty-hint">点击下方标签添加</span>
      </div>
    </div>

    <!-- Tag Pool -->
    <div v-for="(items, cat) in tagPool" :key="cat" class="tag-category">
      <div class="cat-title">{{ catIcons[cat] || '' }} {{ cat }}</div>
      <div class="flex-wrap">
        <span
          v-for="item in items"
          :key="item.id"
          class="pool-chip"
          :class="{ picked: isPicked(item.id) }"
          @click="handleToggle(item)"
        >
          {{ item.icon }} {{ item.hobbyName || item.name }}
          <i v-if="isPicked(item.id)" class="pi pi-check" style="font-size:10px;margin-left:4px;"></i>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToast } from 'primevue/usetoast'
import { getHobbyTags } from '@/api/tags'

const userStore = useUserStore()
const toast = useToast()
const tagPool = ref({})

const catIcons = {
  '运动': '⚽', '艺术': '🎨', '音乐': '🎵', '科技': '🔬', '生活': '🌟', '学术': '📚',
}

const hobbies = computed(() => userStore.hobbies || [])

function isPicked(id) {
  return hobbies.value.some(h => String(h.id) === String(id))
}

async function handleToggle(item) {
  if (isPicked(item.id)) {
    handleRemove({ id: item.id })
  } else {
    try {
      await userStore.doAddHobby(item.id)
      toast.add({ severity: 'success', summary: '成功', detail: '添加成功', life: 2000 })
    } catch { /* handled */ }
  }
}

async function handleRemove(hobby) {
  try {
    await userStore.doDeleteHobby(hobby.id)
    toast.add({ severity: 'success', summary: '成功', detail: '已移除', life: 2000 })
  } catch { /* handled */ }
}

onMounted(async () => {
  await userStore.fetchHobbies()
  try {
    const res = await getHobbyTags()
    const data = res.data || {}
    if (Array.isArray(data)) {
      const pool = {}
      data.forEach(tag => {
        const cat = tag.category || '生活'
        if (!pool[cat]) pool[cat] = []
        pool[cat].push(tag)
      })
      tagPool.value = pool
    } else {
      tagPool.value = data
    }
  } catch { /* handled */ }
})
</script>

<style scoped>
.selected-area {
  padding: 16px; border: 2px dashed #ccc; margin-bottom: 20px; min-height: 60px;
}
.selected-label { font-weight: 700; font-size: 13px; margin-bottom: 8px; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.selected-tag {
  padding: 6px 12px; border: 2px solid #1A1A1A; font-weight: 700; font-size: 13px;
  background: var(--color-yellow); cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
}
.tag-x { font-size: 14px; opacity: 0.5; }
.empty-hint { color: #aaa; font-size: 13px; }

.tag-category { margin-bottom: 20px; }
.cat-title { font-weight: 900; font-size: 15px; margin-bottom: 10px; }
.pool-chip {
  padding: 6px 14px; border: 2px solid #ddd; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; display: inline-flex; align-items: center;
}
.pool-chip:hover { border-color: #1A1A1A; }
.pool-chip.picked { border-color: #1A1A1A; background: var(--color-yellow); }

/* TransitionGroup */
.tag-enter-active, .tag-leave-active { transition: all 0.2s ease; }
.tag-enter-from { opacity: 0; transform: scale(0.8); }
.tag-leave-to { opacity: 0; transform: scale(0.8); }
</style>
