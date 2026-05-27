<template>
  <div class="page-container">
    <header class="page-header">
      <button class="brutal-btn outline small" @click="$router.back()" style="margin-bottom:12px;">← 返回</button>
      <h1 class="page-title">技能标签管理</h1>
      <p class="page-subtitle">每类最多20个技能标签</p>
    </header>

    <!-- Quick batch set -->
    <div class="brutal-card accent-purple" style="margin-bottom:20px;">
      <div class="section-title"><span class="dot" style="background:var(--color-purple)"></span> 批量设置</div>
      <el-form label-position="top" @submit.prevent="handleBatchSet">
        <el-form-item label="我会的技能 (用逗号分隔)">
          <el-input
            v-model="batchCan"
            placeholder="Python, Java, 摄影, 烹饪..."
          />
        </el-form-item>
        <el-form-item label="想学的技能 (用逗号分隔)">
          <el-input
            v-model="batchWant"
            placeholder="吉他, 日语, 滑板..."
          />
        </el-form-item>
        <button type="submit" class="brutal-btn primary small" :disabled="batchLoading">
          {{ batchLoading ? '设置中...' : '批量替换' }}
        </button>
      </el-form>
    </div>

    <div class="brutal-grid-2">
      <!-- Can Skills -->
      <div class="brutal-card accent-cyan">
        <div class="section-title">
          <span class="dot" style="background:var(--color-cyan)"></span>
          我会的技能 ({{ skills.canSkills?.length || 0 }}/20)
        </div>

        <div class="flex-wrap" style="margin-bottom:16px;">
          <div v-for="skill in skills.canSkills" :key="skill.id" class="brutal-tag can">
            {{ skill.skillName }}
            <span class="tag-remove" @click="handleDelete(skill.id)">×</span>
          </div>
          <div v-if="!skills.canSkills?.length" style="color:#888;font-size:13px;">暂无，快去添加吧</div>
        </div>

        <div class="add-row">
          <el-input
            v-model="newCanSkill"
            placeholder="技能名称"
            maxlength="20"
            class="add-input"
            @keyup.enter="handleAdd('can')"
          />
          <button class="brutal-btn primary small" @click="handleAdd('can')" :disabled="addLoading">
            添加
          </button>
        </div>
      </div>

      <!-- Want Skills -->
      <div class="brutal-card accent-pink">
        <div class="section-title">
          <span class="dot" style="background:var(--color-pink)"></span>
          想学的技能 ({{ skills.wantSkills?.length || 0 }}/20)
        </div>

        <div class="flex-wrap" style="margin-bottom:16px;">
          <div v-for="skill in skills.wantSkills" :key="skill.id" class="brutal-tag want">
            {{ skill.skillName }}
            <span class="tag-remove" @click="handleDelete(skill.id)">×</span>
          </div>
          <div v-if="!skills.wantSkills?.length" style="color:#888;font-size:13px;">暂无，快去添加吧</div>
        </div>

        <div class="add-row">
          <el-input
            v-model="newWantSkill"
            placeholder="技能名称"
            maxlength="20"
            class="add-input"
            @keyup.enter="handleAdd('want')"
          />
          <button class="brutal-btn primary small" @click="handleAdd('want')" :disabled="addLoading">
            添加
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const skills = computed(() => userStore.skills)

const newCanSkill = ref('')
const newWantSkill = ref('')
const batchCan = ref('')
const batchWant = ref('')
const addLoading = ref(false)
const batchLoading = ref(false)

onMounted(() => userStore.fetchSkills())

async function handleAdd(type) {
  const name = type === 'can' ? newCanSkill.value.trim() : newWantSkill.value.trim()
  if (!name) {
    ElMessage.warning('请输入技能名称')
    return
  }
  addLoading.value = true
  try {
    await userStore.doAddSkill({ skillName: name, skillType: type === 'can' ? 1 : 2 })
    if (type === 'can') newCanSkill.value = ''
    else newWantSkill.value = ''
    ElMessage.success('添加成功')
  } catch { /* handled */ } finally {
    addLoading.value = false
  }
}

async function handleDelete(skillId) {
  await ElMessageBox.confirm('确定删除这个技能标签吗？', '确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await userStore.doDeleteSkill(skillId)
  ElMessage.success('已删除')
}

async function handleBatchSet() {
  const canList = batchCan.value.split(',').map(s => s.trim()).filter(Boolean)
  const wantList = batchWant.value.split(',').map(s => s.trim()).filter(Boolean)

  if (!canList.length && !wantList.length) {
    ElMessage.warning('至少输入一个技能')
    return
  }

  batchLoading.value = true
  try {
    await userStore.doUpdateSkillList({ canSkills: canList, wantSkills: wantList })
    ElMessage.success('技能标签已更新')
  } catch { /* handled */ } finally {
    batchLoading.value = false
  }
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
