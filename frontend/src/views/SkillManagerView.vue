<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
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
  background: #f0f0f0;
  border: 2px solid #1A1A1A;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 20px;
  overflow: hidden;
}
.segment-bg {
  position: absolute;
  top: 4px; bottom: 4px;
  width: calc(50% - 4px);
  border-radius: 8px;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}
.segment-bg.can { transform: translateX(0); background: var(--color-cyan); }
.segment-bg.want { transform: translateX(calc(100% + 8px)); background: var(--color-pink); }
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
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #888;
  cursor: pointer;
  transition: color 0.2s;
}
.segment-btn.active { color: #1A1A1A; }
.seg-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: rgba(0,0,0,0.15);
  font-size: 11px;
  font-weight: 900;
}
.segment-btn.active .seg-badge {
  background: #1A1A1A;
  color: #fff;
}

/* ===== Selected Area ===== */
.selected-area {
  padding: 16px 20px;
  border: 2px solid #1A1A1A;
  border-radius: 12px;
  margin-bottom: 20px;
  min-height: 80px;
  transition: background 0.3s, border-color 0.3s;
}
.selected-area.can { background: linear-gradient(135deg, #E0F7FA, #fff); border-color: var(--color-cyan); }
.selected-area.want { background: linear-gradient(135deg, #FCE4EC, #fff); border-color: var(--color-pink); }
.selected-label {
  font-size: 12px;
  font-weight: 800;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
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
  border: 2px solid #1A1A1A;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.selected-area.can .selected-tag { background: var(--color-cyan); color: #1A1A1A; }
.selected-area.want .selected-tag { background: var(--color-pink); color: #1A1A1A; }
.selected-tag:hover { transform: scale(0.95); opacity: 0.8; }
.tag-x { font-size: 16px; font-weight: 900; line-height: 1; }
.empty-hint { font-size: 13px; color: #aaa; font-weight: 600; }

/* TransitionGroup */
.tag-enter-active { transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1); }
.tag-leave-active { transition: all 0.2s ease; }
.tag-enter-from { opacity: 0; transform: scale(0.6); }
.tag-leave-to { opacity: 0; transform: scale(0.6); }

/* ===== Tag Pool ===== */
.pool-section { display: flex; flex-direction: column; gap: 16px; }
.pool-group {
  padding: 16px 20px;
  border: 2px solid #eee;
  border-radius: 12px;
  background: #fafafa;
}
.pool-cat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 800;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}
.pool-cat-icon { font-size: 16px; }
.pool-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pool-chip {
  padding: 7px 16px;
  border: 2px solid #ddd;
  border-radius: 8px;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  color: #444;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
}
.pool-chip:hover {
  border-color: #1A1A1A;
  transform: translateY(-2px);
  box-shadow: 3px 3px 0 rgba(0,0,0,0.08);
}
.pool-chip:active { transform: scale(0.95); }
.pool-chip.picked {
  background: #1A1A1A;
  color: #fff;
  border-color: #1A1A1A;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.15);
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
  background: linear-gradient(transparent, #fff 40%);
  z-index: 10;
}
.save-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 48px;
  border: 3px solid #1A1A1A;
  border-radius: 12px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-family: inherit;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 4px 4px 0 rgba(0,0,0,0.15);
}
.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 6px 6px 0 rgba(0,0,0,0.15);
}
.save-btn:active {
  transform: scale(0.97);
  box-shadow: 2px 2px 0 rgba(0,0,0,0.15);
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.save-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.save-leave-active { transition: all 0.2s ease; }
.save-enter-from { opacity: 0; transform: translateY(20px); }
.save-leave-to { opacity: 0; transform: translateY(20px); }
</style>
