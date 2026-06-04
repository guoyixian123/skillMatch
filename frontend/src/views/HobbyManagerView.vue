<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">
        <el-icon><ArrowLeft /></el-icon> 返回
      </button>
      <h1 class="page-title">兴趣爱好</h1>
      <p class="page-subtitle">从标签池中选取你的兴趣爱好</p>
    </header>

    <!-- Selected Hobbies -->
    <div class="selected-area">
      <div class="selected-label">已选 {{ hobbies.length }} 个</div>
      <div class="selected-tags">
        <TransitionGroup name="tag">
          <span
            v-for="hobby in hobbies"
            :key="hobby.id"
            class="selected-tag"
            @click="handleRemove(hobby)"
          >
            {{ hobby.icon }} {{ hobby.hobbyName }}
            <span class="tag-x">&times;</span>
          </span>
        </TransitionGroup>
        <span v-if="!hobbies.length" class="empty-hint">点击下方标签添加</span>
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
            :key="tag.name"
            class="pool-chip"
            :class="{ picked: isHobbySelected(tag.name) }"
            @click="handleToggle(tag)"
          >
            <span class="chip-icon">{{ tag.icon }}</span>
            <span class="chip-name">{{ tag.name }}</span>
            <span class="chip-check" v-if="isHobbySelected(tag.name)">
              <el-icon><Check /></el-icon>
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from '@/utils/message'
import { ArrowLeft, Loading, Check } from '@element-plus/icons-vue'
import { getHobbyTags } from '@/api/tags'

const userStore = useUserStore()
const hobbies = computed(() => userStore.hobbies)

const loading = ref(true)
const tagPool = ref({})

const catIcons = {
  '运动': '🏃', '艺术': '🎨', '音乐': '🎵',
  '科技': '🔬', '生活': '☕', '学术': '📖',
}

function catIcon(cat) { return catIcons[cat] || '📂' }

function isHobbySelected(name) {
  return hobbies.value.some(h => h.hobbyName === name)
}

async function handleToggle(tag) {
  if (isHobbySelected(tag.name)) {
    const hobby = hobbies.value.find(h => h.hobbyName === tag.name)
    if (hobby) {
      try {
        const res = await userStore.doDeleteHobby(hobby.id)
        ElMessage.success(res.message || '已移除')
      } catch { /* handled */ }
    }
  } else {
    try {
      const res = await userStore.doAddHobby({ hobbyName: tag.name, icon: tag.icon })
      ElMessage.success(res.message || '添加成功')
    } catch { /* handled */ }
  }
}

async function handleRemove(hobby) {
  try {
    const res = await userStore.doDeleteHobby(hobby.id)
    ElMessage.success(res.message || '已移除')
  } catch { /* handled */ }
}

onMounted(async () => {
  try {
    const [tagsRes] = await Promise.all([getHobbyTags(), userStore.fetchHobbies()])
    tagPool.value = tagsRes.data || {}
  } catch { /* handled */ } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ===== Selected Area ===== */
.selected-area {
  padding: 16px 20px;
  border: 2px solid #1A1A1A;
  border-radius: 12px;
  margin-bottom: 20px;
  min-height: 80px;
  background: linear-gradient(135deg, #FFF8E1, #fff);
  border-color: var(--color-yellow);
}
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
  background: var(--color-yellow);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
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
  gap: 10px;
}
.pool-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 2px solid #ddd;
  border-radius: 10px;
  background: #fff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #444;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
  position: relative;
}
.pool-chip:hover {
  border-color: #1A1A1A;
  transform: translateY(-2px);
  box-shadow: 3px 3px 0 rgba(0,0,0,0.08);
}
.pool-chip:active { transform: scale(0.95); }
.pool-chip.picked {
  background: linear-gradient(135deg, #FFF8E1, #FFD700);
  border-color: #1A1A1A;
  color: #1A1A1A;
  box-shadow: 2px 2px 0 rgba(0,0,0,0.12);
}
.chip-icon { font-size: 18px; }
.chip-name { line-height: 1; }
.chip-check {
  display: inline-flex;
  align-items: center;
  margin-left: 2px;
  color: #16a34a;
  font-size: 14px;
  font-weight: 900;
}
</style>
