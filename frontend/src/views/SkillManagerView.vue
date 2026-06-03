<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <i class="pi pi-arrow-left"></i> 返回
      </button>
      <h1 class="page-title">技能标签管理</h1>
      <p class="page-subtitle">从标签池中选取，每类最多 10 个</p>
    </header>

    <!-- Segmented Toggle -->
    <div class="segment-bar">
      <div class="segment-bg" :class="editMode"></div>
      <button class="segment-btn" :class="{ active: editMode === 'can' }" @click="editMode = 'can'">
        <i class="pi pi-bullseye"></i> 我会的
      </button>
      <button class="segment-btn" :class="{ active: editMode === 'want' }" @click="editMode = 'want'">
        <i class="pi pi-chart-line"></i> 想学的
      </button>
    </div>

    <!-- Selected Skills -->
    <div class="selected-area" :class="editMode">
      <div class="selected-label">已选 {{ currentSelected.length }}/10</div>
      <div class="selected-tags">
        <TransitionGroup name="tag">
          <span v-for="name in currentSelected" :key="name" class="selected-tag" @click="toggleSkill(name)">
            {{ name }} <span class="tag-x">&times;</span>
          </span>
        </TransitionGroup>
        <span v-if="!currentSelected.length" class="empty-hint">点击下方标签添加</span>
      </div>
    </div>

    <!-- Save Bar -->
    <div v-if="hasChanges" class="save-bar">
      <span>{{ changesMessage }}</span>
      <div style="display:flex;gap:8px;">
        <button class="brutal-btn outline small" @click="resetChanges">取消</button>
        <button class="brutal-btn primary small" @click="handleSave" :disabled="saving">
          <i class="pi pi-check"></i> {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>

    <!-- Tag Pool -->
    <div v-for="(skills, cat) in tagPool" :key="cat" class="tag-category">
      <div class="cat-title">{{ catIcons[cat] || '' }} {{ cat }}</div>
      <div class="flex-wrap">
        <span
          v-for="s in skills"
          :key="s"
          class="pool-chip"
          :class="{ picked: isPicked(s), other: isOtherModePicked(s) }"
          @click="toggleSkill(s)"
        >
          {{ s }}
          <i v-if="isPicked(s)" class="pi pi-check" style="font-size:10px;margin-left:4px;"></i>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useToast } from 'primevue/usetoast'
import { getSkillTags } from '@/api/tags'

const userStore = useUserStore()
const toast = useToast()

const editMode = ref('can')
const selectedCan = ref([])
const selectedWant = ref([])
const originalCan = ref([])
const originalWant = ref([])
const tagPool = ref({})
const saving = ref(false)

const catIcons = {
  '编程语言': '💻', '前端开发': '🎨', '后端开发': '⚙️', '数据与AI': '🤖',
  '设计': '✏️', '音乐': '🎵', '语言': '🗣️', '运动健身': '🏃', '其他': '📌',
}

const currentSelected = computed(() => editMode.value === 'can' ? selectedCan.value : selectedWant.value)

const hasChanges = computed(() => {
  return JSON.stringify(selectedCan.value.sort()) !== JSON.stringify(originalCan.value.sort()) ||
         JSON.stringify(selectedWant.value.sort()) !== JSON.stringify(originalWant.value.sort())
})

const changesMessage = computed(() => {
  const diff = (selectedCan.value.length - originalCan.value.length) + (selectedWant.value.length - originalWant.value.length)
  return '有未保存的更改'
})

function isPicked(name) {
  return currentSelected.value.includes(name)
}

function isOtherModePicked(name) {
  return editMode.value === 'can' ? selectedWant.value.includes(name) : selectedCan.value.includes(name)
}

function toggleSkill(name) {
  const arr = currentSelected.value
  const idx = arr.indexOf(name)
  if (idx >= 0) {
    arr.splice(idx, 1)
  } else {
    if (arr.length >= 10) {
      toast.add({ severity: 'warn', summary: '提示', detail: '最多选择 10 个', life: 3000 })
      return
    }
    arr.push(name)
  }
}

function resetChanges() {
  selectedCan.value = [...originalCan.value]
  selectedWant.value = [...originalWant.value]
}

async function handleSave() {
  saving.value = true
  try {
    await userStore.doUpdateSkillList({ canSkills: selectedCan.value, wantSkills: selectedWant.value })
    originalCan.value = [...selectedCan.value]
    originalWant.value = [...selectedWant.value]
    toast.add({ severity: 'success', summary: '成功', detail: '保存成功', life: 3000 })
  } catch { /* handled */ } finally { saving.value = false }
}

onMounted(async () => {
  // Load current skills
  await userStore.fetchSkills()
  const can = (userStore.skills?.canSkills || []).map(s => s.skillName || s).filter(Boolean)
  const want = (userStore.skills?.wantSkills || []).map(s => s.skillName || s).filter(Boolean)
  selectedCan.value = [...can]
  selectedWant.value = [...want]
  originalCan.value = [...can]
  originalWant.value = [...want]

  // Load tag pool
  try {
    const res = await getSkillTags()
    const data = res.data || {}
    // 转换为以分类为 key 的字典
    if (Array.isArray(data)) {
      const pool = {}
      data.forEach(tag => {
        const cat = tag.category || '其他'
        if (!pool[cat]) pool[cat] = []
        pool[cat].push(tag.name || tag)
      })
      tagPool.value = pool
    } else {
      tagPool.value = data
    }
  } catch { /* handled */ }
})
</script>

<style scoped>
.segment-bar {
  display: flex; position: relative;
  border: var(--border-thick); margin-bottom: 20px; overflow: hidden;
}
.segment-btn {
  flex: 1; padding: 14px; border: none; background: transparent;
  font-weight: 700; font-size: 14px; cursor: pointer; z-index: 1;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  transition: color 0.2s;
}
.segment-btn.active { color: #1A1A1A; }
.segment-bg {
  position: absolute; top: 0; left: 0; width: 50%; height: 100%;
  background: var(--color-yellow); transition: transform 0.3s ease;
}
.segment-bg.want { transform: translateX(100%); }

.selected-area {
  padding: 16px; border: 2px dashed #ccc; margin-bottom: 20px; min-height: 60px;
}
.selected-area.can { border-color: var(--color-cyan); background: #f0fdff; }
.selected-area.want { border-color: var(--color-pink); background: #fff0f5; }
.selected-label { font-weight: 700; font-size: 13px; margin-bottom: 8px; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.selected-tag {
  padding: 6px 12px; border: 2px solid #1A1A1A; font-weight: 700; font-size: 13px;
  background: var(--color-yellow); cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
}
.tag-x { font-size: 14px; opacity: 0.5; }
.empty-hint { color: #aaa; font-size: 13px; }
.save-bar {
  position: sticky; bottom: 16px; background: #fff; border: var(--border-thick);
  box-shadow: var(--shadow-hard); padding: 12px 16px; display: flex;
  justify-content: space-between; align-items: center; margin: 16px 0; z-index: 10;
}
.tag-category { margin-bottom: 20px; }
.cat-title { font-weight: 900; font-size: 15px; margin-bottom: 10px; }
.pool-chip {
  padding: 6px 14px; border: 2px solid #ddd; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; display: inline-flex; align-items: center;
}
.pool-chip:hover { border-color: #1A1A1A; }
.pool-chip.picked { border-color: #1A1A1A; background: var(--color-cyan); color: #000; }
.pool-chip.other { opacity: 0.4; }

/* TransitionGroup */
.tag-enter-active, .tag-leave-active { transition: all 0.2s ease; }
.tag-enter-from { opacity: 0; transform: scale(0.8); }
.tag-leave-to { opacity: 0; transform: scale(0.8); }
</style>
