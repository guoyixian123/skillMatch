<template>
  <div class="tag-manage">
    <header class="page-header">
      <h1 class="pg-title">标签管理</h1>
      <p class="pg-sub">管理技能标签和爱好标签，用户将从这里选择</p>
    </header>

    <TabView v-model:activeIndex="activeTab">
      <!-- ===== 技能标签 ===== -->
      <TabPanel header="技能标签">
        <div class="action-bar">
          <button class="nb-btn primary" @click="openAddSkill">
            <span class="material-symbols-outlined">add</span> 新增技能标签
          </button>
        </div>

        <div class="table-wrap nb-card" style="padding:0;">
          <DataTable :value="skillTags" :loading="skillLoading" :paginator="true" :rows="20">
            <Column field="id" header="ID" style="width:60px" />
            <Column field="name" header="标签名称" style="min-width:140px">
              <template #body="{ data }"><span class="cell-name">{{ data.name }}</span></template>
            </Column>
            <Column field="category" header="分类" style="min-width:140px">
              <template #body="{ data }"><span class="nb-tag admin">{{ data.category }}</span></template>
            </Column>
            <Column field="sortOrder" header="排序" style="width:80px" />
            <Column header="操作" style="width:140px;text-align:right">
              <template #body="{ data }">
                <div class="row-actions">
                  <button class="nb-btn xs" @click="openEditSkill(data)">编辑</button>
                  <button class="nb-btn danger xs" @click="handleDeleteSkill(data)">删除</button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>

      <!-- ===== 爱好标签 ===== -->
      <TabPanel header="爱好标签">
        <div class="action-bar">
          <button class="nb-btn primary" @click="openAddHobby">
            <span class="material-symbols-outlined">add</span> 新增爱好标签
          </button>
        </div>

        <div class="table-wrap nb-card" style="padding:0;">
          <DataTable :value="hobbyTags" :loading="hobbyLoading" :paginator="true" :rows="20">
            <Column field="id" header="ID" style="width:60px" />
            <Column field="name" header="标签名称" style="min-width:120px">
              <template #body="{ data }"><span class="cell-name">{{ data.name }}</span></template>
            </Column>
            <Column header="图标" style="width:70px">
              <template #body="{ data }"><span style="font-size:22px;">{{ data.icon }}</span></template>
            </Column>
            <Column field="category" header="分类" style="min-width:120px">
              <template #body="{ data }"><span class="nb-tag admin">{{ data.category }}</span></template>
            </Column>
            <Column field="sortOrder" header="排序" style="width:80px" />
            <Column header="操作" style="width:140px;text-align:right">
              <template #body="{ data }">
                <div class="row-actions">
                  <button class="nb-btn xs" @click="openEditHobby(data)">编辑</button>
                  <button class="nb-btn danger xs" @click="handleDeleteHobby(data)">删除</button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>
    </TabView>

    <!-- 技能标签弹窗 -->
    <Dialog v-model:visible="skillDialog" :header="skillDialogTitle" :modal="true" :style="{ width: '420px' }">
      <form @submit.prevent="handleSaveSkill" class="edit-form">
        <div class="gf">
          <label class="filter-label">标签名称</label>
          <input v-model="skillForm.name" class="nb-input" required placeholder="如: Python" maxlength="20" />
        </div>
        <div class="gf">
          <label class="filter-label">分类</label>
          <input v-model="skillForm.category" class="nb-input" required placeholder="如: 编程语言" maxlength="20" />
        </div>
        <div class="gf">
          <label class="filter-label">排序（数字越小越靠前）</label>
          <input v-model.number="skillForm.sortOrder" class="nb-input" type="number" min="0" max="9999" />
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:16px;">
          <button type="button" class="nb-btn" @click="skillDialog = false">取消</button>
          <button type="submit" class="nb-btn primary" :disabled="saving">{{ saving ? '保存中...' : '确认' }}</button>
        </div>
      </form>
    </Dialog>

    <!-- 爱好标签弹窗 -->
    <Dialog v-model:visible="hobbyDialog" :header="hobbyDialogTitle" :modal="true" :style="{ width: '420px' }">
      <form @submit.prevent="handleSaveHobby" class="edit-form">
        <div class="gf">
          <label class="filter-label">标签名称</label>
          <input v-model="hobbyForm.name" class="nb-input" required placeholder="如: 篮球" maxlength="20" />
        </div>
        <div class="gf">
          <label class="filter-label">图标（emoji）</label>
          <input v-model="hobbyForm.icon" class="nb-input" placeholder="如: 🏀" maxlength="10" />
        </div>
        <div class="gf">
          <label class="filter-label">分类</label>
          <input v-model="hobbyForm.category" class="nb-input" required placeholder="如: 运动" maxlength="20" />
        </div>
        <div class="gf">
          <label class="filter-label">排序（数字越小越靠前）</label>
          <input v-model.number="hobbyForm.sortOrder" class="nb-input" type="number" min="0" max="9999" />
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:16px;">
          <button type="button" class="nb-btn" @click="hobbyDialog = false">取消</button>
          <button type="submit" class="nb-btn primary" :disabled="saving">{{ saving ? '保存中...' : '确认' }}</button>
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import {
  listSkillTags, createSkillTag, updateSkillTag, deleteSkillTag,
  listHobbyTags, createHobbyTag, updateHobbyTag, deleteHobbyTag,
} from '@/api/admin'

const toast = useToast()
const confirmDialog = useConfirm()
const activeTab = ref(0)
const saving = ref(false)

// ===== 技能标签 =====
const skillTags = ref([])
const skillLoading = ref(false)
const skillDialog = ref(false)
const skillDialogTitle = ref('新增技能标签')
const skillForm = ref({ name: '', category: '', sortOrder: 0 })
const editingSkillId = ref(null)

async function fetchSkillTags() {
  skillLoading.value = true
  try {
    const r = await listSkillTags()
    skillTags.value = r.data || []
  } catch { /* */ } finally { skillLoading.value = false }
}

function openAddSkill() {
  editingSkillId.value = null
  skillDialogTitle.value = '新增技能标签'
  skillForm.value = { name: '', category: '', sortOrder: 0 }
  skillDialog.value = true
}

function openEditSkill(row) {
  editingSkillId.value = row.id
  skillDialogTitle.value = '编辑技能标签'
  skillForm.value = { name: row.name, category: row.category, sortOrder: row.sortOrder }
  skillDialog.value = true
}

async function handleSaveSkill() {
  if (!skillForm.value.name?.trim() || !skillForm.value.category?.trim()) {
    toast.add({ severity: 'warn', summary: '必填', detail: '请填写标签名称和分类', life: 3000 })
    return
  }
  saving.value = true
  try {
    if (editingSkillId.value) {
      await updateSkillTag(editingSkillId.value, skillForm.value)
      toast.add({ severity: 'success', summary: '完成', detail: '技能标签已修改', life: 3000 })
    } else {
      await createSkillTag(skillForm.value)
      toast.add({ severity: 'success', summary: '完成', detail: '技能标签已新增', life: 3000 })
    }
    skillDialog.value = false
    fetchSkillTags()
  } catch { /* */ } finally { saving.value = false }
}

function handleDeleteSkill(row) {
  confirmDialog.require({
    message: `确定删除技能标签「${row.name}」？`,
    header: '确认删除',
    rejectLabel: '取消',
    acceptLabel: '删除',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await deleteSkillTag(row.id)
        toast.add({ severity: 'success', summary: '完成', detail: '技能标签已删除', life: 3000 })
        fetchSkillTags()
      } catch { /* */ }
    },
  })
}

// ===== 爱好标签 =====
const hobbyTags = ref([])
const hobbyLoading = ref(false)
const hobbyDialog = ref(false)
const hobbyDialogTitle = ref('新增爱好标签')
const hobbyForm = ref({ name: '', icon: '', category: '', sortOrder: 0 })
const editingHobbyId = ref(null)

async function fetchHobbyTags() {
  hobbyLoading.value = true
  try {
    const r = await listHobbyTags()
    hobbyTags.value = r.data || []
  } catch { /* */ } finally { hobbyLoading.value = false }
}

function openAddHobby() {
  editingHobbyId.value = null
  hobbyDialogTitle.value = '新增爱好标签'
  hobbyForm.value = { name: '', icon: '', category: '', sortOrder: 0 }
  hobbyDialog.value = true
}

function openEditHobby(row) {
  editingHobbyId.value = row.id
  hobbyDialogTitle.value = '编辑爱好标签'
  hobbyForm.value = { name: row.name, icon: row.icon, category: row.category, sortOrder: row.sortOrder }
  hobbyDialog.value = true
}

async function handleSaveHobby() {
  if (!hobbyForm.value.name?.trim() || !hobbyForm.value.category?.trim()) {
    toast.add({ severity: 'warn', summary: '必填', detail: '请填写标签名称和分类', life: 3000 })
    return
  }
  saving.value = true
  try {
    if (editingHobbyId.value) {
      await updateHobbyTag(editingHobbyId.value, hobbyForm.value)
      toast.add({ severity: 'success', summary: '完成', detail: '爱好标签已修改', life: 3000 })
    } else {
      await createHobbyTag(hobbyForm.value)
      toast.add({ severity: 'success', summary: '完成', detail: '爱好标签已新增', life: 3000 })
    }
    hobbyDialog.value = false
    fetchHobbyTags()
  } catch { /* */ } finally { saving.value = false }
}

function handleDeleteHobby(row) {
  confirmDialog.require({
    message: `确定删除爱好标签「${row.name}」？`,
    header: '确认删除',
    rejectLabel: '取消',
    acceptLabel: '删除',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await deleteHobbyTag(row.id)
        toast.add({ severity: 'success', summary: '完成', detail: '爱好标签已删除', life: 3000 })
        fetchHobbyTags()
      } catch { /* */ }
    },
  })
}

onMounted(() => {
  fetchSkillTags()
  fetchHobbyTags()
})
</script>

<style scoped>
.tag-manage { display: flex; flex-direction: column; gap: 16px; }
.page-header { margin-bottom: 4px; }
.pg-title { font-family: var(--font-headline); font-size: 28px; font-weight: 900; text-transform: uppercase; font-style: italic; }
.pg-sub { font-size: 13px; font-weight: 700; opacity: 0.5; text-transform: uppercase; letter-spacing: 1px; }
.action-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.table-wrap { overflow-x: auto; }
.mono-text { font-family: var(--font-mono); font-size: 11px; font-weight: 700; }
.cell-name { font-family: var(--font-headline); font-weight: 700; font-size: 14px; text-transform: uppercase; }
.row-actions { display: flex; gap: 4px; justify-content: flex-end; }
.edit-form { display: flex; flex-direction: column; gap: 12px; }
.gf { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
</style>
