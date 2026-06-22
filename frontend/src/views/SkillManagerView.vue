<template>
  <div class="page-container">
    <header class="page-header">
      <button class="geo-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <h1 class="page-title">技能标签管理</h1>
      <p class="page-subtitle">从标签池中选取，每类最多 10 个</p>
    </header>

    <!-- Segmented Toggle -->
    <div class="segment-bar">
      <div class="segment-bg" :class="editMode"></div>
      <button class="segment-btn" :class="{ active: editMode === 'can' }" @click="editMode = 'can'">
        <el-icon><Aim /></el-icon> 我会的
        <span class="seg-badge" v-if="selectedCan.length">{{ selectedCan.length }}</span>
      </button>
      <button class="segment-btn" :class="{ active: editMode === 'want' }" @click="editMode = 'want'">
        <el-icon><TrendCharts /></el-icon> 想学的
        <span class="seg-badge" v-if="selectedWant.length">{{ selectedWant.length }}</span>
      </button>
    </div>

    <!-- Selected Skills -->
    <div class="selected-area" :class="editMode">
      <div class="selected-label">已选 {{ currentSelected.length }}/10</div>
      <div class="selected-tags">
        <TransitionGroup name="tag">
          <span
            v-for="name in currentSelected"
            :key="name"
            class="selected-tag"
            @click="toggleTag(name)"
          >
            {{ name }}
            <span class="tag-x">&times;</span>
          </span>
        </TransitionGroup>
        <span v-if="!currentSelected.length" class="empty-hint">点击下方标签添加</span>
      </div>
    </div>

    <!-- Tag Pool -->
    <div v-if="loading" class="loading-block">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中...
    </div>

    <div v-else class="pool-section">
      <div v-for="(tags, category) in tagPool" :key="category" class="pool-group">
        <div class="pool-cat">
          <span class="pool-cat-icon">{{ catIcon(category) }}</span>
          {{ category }}
        </div>
        <div class="pool-grid">
          <button
            v-for="tag in tags"
            :key="tag"
            class="pool-chip"
            :class="{ picked: isSelected(tag), other: isInOtherMode(tag) }"
            @click="toggleTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>
    </div>

    <!-- Save Bar -->
    <Transition name="save">
      <div class="save-bar" v-if="hasChanges">
        <button class="save-btn" @click="handleSave" :disabled="saving">
          <el-icon v-if="!saving"><Check /></el-icon>
          <el-icon v-else class="is-loading"><Loading /></el-icon>
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/message'
import { ArrowLeft, Loading, Aim, TrendCharts, Check } from '@element-plus/icons-vue'
import { getSkillTags } from '@/api/tags'

const userStore = useUserStore()
const skills = computed(() => userStore.skills)

const loading = ref(true)
const saving = ref(false)
const editMode = ref('can')
const tagPool = ref({})

const selectedCan = ref([])
const selectedWant = ref([])
const originalCan = ref([])
const originalWant = ref([])

const currentSelected = computed(() =>
  editMode.value === 'can' ? selectedCan.value : selectedWant.value
)

const hasChanges = computed(() =>
  JSON.stringify(selectedCan.value) !== JSON.stringify(originalCan.value) ||
  JSON.stringify(selectedWant.value) !== JSON.stringify(originalWant.value)
)

const catIcons = {
  '编程语言': '💻', '前端开发': '🌐', '后端开发': '⚙️',
  '数据与AI': '🤖', '设计': '🎨', '音乐': '🎵',
  '语言': '🌍', '运动健身': '💪', '其他': '📌',
}

function catIcon(cat) { return catIcons[cat] || '📂' }

function isSelected(name) {
  return selectedCan.value.includes(name) || selectedWant.value.includes(name)
}

function isInOtherMode(name) {
  if (editMode.value === 'can') return selectedWant.value.includes(name)
  return selectedCan.value.includes(name)
}

function toggleTag(name) {
  const list = editMode.value === 'can' ? selectedCan : selectedWant
  const idx = list.value.indexOf(name)
  if (idx >= 0) {
    list.value = list.value.filter(n => n !== name)
  } else {
    if (list.value.length >= 10) {
      ElMessage.warning('最多选择 10 个')
      return
    }
    list.value = [...list.value, name]
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await userStore.doUpdateSkillList({
      canSkills: selectedCan.value,
      wantSkills: selectedWant.value,
    })
    originalCan.value = [...selectedCan.value]
    originalWant.value = [...selectedWant.value]
    ElMessage.success(res.message || '保存成功')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const [tagsRes] = await Promise.all([getSkillTags(), userStore.fetchSkills()])
    tagPool.value = tagsRes.data || {}
    const can = (skills.value.canSkills || []).map(s => s.skillName)
    const want = (skills.value.wantSkills || []).map(s => s.skillName)
    selectedCan.value = can
    selectedWant.value = want
    originalCan.value = [...can]
    originalWant.value = [...want]
  } catch { /* handled */ } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ===== Segmented Toggle ===== */
.segment-bar {
  position: relative;
  display: flex;
  background: var(--color-muted);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  padding: 4px;
  margin-bottom: 20px;
  overflow: hidden;
}
.segment-bg {
  position: absolute;
  top: 4px; bottom: 4px;
  width: calc(50% - 4px);
  border-radius: var(--radius-full);
  transition: transform 0.4s var(--ease-bounce);
  z-index: 0;
}
.segment-bg.can { transform: translateX(0); background: #D1FAE5; }
.segment-bg.want { transform: translateX(calc(100% + 8px)); background: #FCE7F3; }
.segment-btn {
  flex: 1;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 0;
  border: none;
  background: transparent;
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-muted-fg);
  cursor: pointer;
  transition: color 0.3s;
}
.segment-btn.active { color: var(--color-fg); }
.seg-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: var(--radius-full);
  background: rgba(0,0,0,0.1);
  font-size: 11px;
  font-weight: 800;
}
.segment-btn.active .seg-badge {
  background: var(--color-fg);
  color: #fff;
}

/* ===== Selected Area ===== */
.selected-area {
  padding: 16px 20px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  min-height: 80px;
  transition: background 0.3s, border-color 0.3s;
}
.selected-area.can { background: linear-gradient(135deg, #ECFDF5, #fff); border-color: #6EE7B7; }
.selected-area.want { background: linear-gradient(135deg, #FDF2F8, #fff); border-color: #F9A8D4; }
.selected-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-muted-fg);
  margin-bottom: 10px;
  font-family: var(--font-heading);
}
.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 2px solid var(--color-fg);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--ease-bounce);
  user-select: none;
}
.selected-area.can .selected-tag { background: #D1FAE5; color: #065F46; }
.selected-area.want .selected-tag { background: #FCE7F3; color: #9D174D; }
.selected-tag:hover { transform: scale(0.95); opacity: 0.8; }
.tag-x { font-size: 16px; font-weight: 900; line-height: 1; }
.empty-hint { font-size: 13px; color: var(--color-muted-fg); font-weight: 500; }

/* TransitionGroup */
.tag-enter-active { transition: all 0.25s var(--ease-bounce); }
.tag-leave-active { transition: all 0.2s ease; }
.tag-enter-from { opacity: 0; transform: scale(0.6); }
.tag-leave-to { opacity: 0; transform: scale(0.6); }

/* ===== Tag Pool ===== */
.pool-section { display: flex; flex-direction: column; gap: 16px; }
.pool-group {
  padding: 16px 20px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fff;
}
.pool-cat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-muted-fg);
  margin-bottom: 12px;
  font-family: var(--font-heading);
}
.pool-cat-icon { font-size: 16px; }
.pool-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pool-chip {
  padding: 7px 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-full);
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-fg);
  cursor: pointer;
  transition: all 0.3s var(--ease-bounce);
  user-select: none;
}
.pool-chip:hover {
  border-color: var(--color-accent);
  transform: translateY(-2px);
  box-shadow: 3px 3px 0 rgba(139, 92, 246, 0.15);
}
.pool-chip:active { transform: scale(0.95); }
.pool-chip.picked {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
  box-shadow: 3px 3px 0 var(--color-fg);
}
.pool-chip.other {
  opacity: 0.35;
}

/* ===== Save Bar ===== */
.save-bar {
  position: sticky;
  bottom: 0;
  padding: 20px 0 12px;
  text-align: center;
  background: linear-gradient(transparent, var(--color-bg) 40%);
  z-index: 10;
}
.save-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 48px;
  border: 2px solid var(--color-fg);
  border-radius: var(--radius-full);
  background: var(--color-quaternary);
  color: #fff;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s var(--ease-bounce);
  box-shadow: 4px 4px 0 var(--color-fg);
}
.save-btn:hover {
  transform: translate(-2px, -2px);
  box-shadow: 6px 6px 0 var(--color-fg);
}
.save-btn:active {
  transform: translate(2px, 2px);
  box-shadow: 2px 2px 0 var(--color-fg);
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.save-enter-active { transition: all 0.3s var(--ease-bounce); }
.save-leave-active { transition: all 0.2s ease; }
.save-enter-from { opacity: 0; transform: translateY(20px); }
.save-leave-to { opacity: 0; transform: translateY(20px); }
</style>
