<template>
  <div class="admin-manage">
    <header class="page-header">
      <h1 class="pg-title">管理员管理</h1>
      <p class="pg-sub">根管理员可添加 / 禁用 / 删除子管理员</p>
    </header>

    <div class="action-bar">
      <button class="nb-btn primary" @click="showAdd = true"><span class="material-symbols-outlined">person_add</span> 添加子管理员</button>
    </div>

    <div class="table-wrap nb-card" style="padding:0;">
      <DataTable :value="admins" :loading="loading">
        <Column field="id" header="ID" style="width:60px" />
        <Column header="用户ID" style="min-width:160px">
          <template #body="{ data: a }"><span class="mono-text">{{ a.userId }}</span></template>
        </Column>
        <Column field="name" header="姓名" style="min-width:120px">
          <template #body="{ data: a }"><span class="cell-name">{{ a.name }}</span></template>
        </Column>
        <Column header="角色" style="width:110px">
          <template #body="{ data: a }">
            <span :class="['nb-tag', a.role==='ROOT'?'root':'admin']">{{ a.role==='ROOT'?'根管理员':'管理员' }}</span>
          </template>
        </Column>
        <Column field="parentName" header="上级管理员" style="min-width:130px" />
        <Column header="状态" style="width:80px">
          <template #body="{ data: a }"><span :class="['nb-tag', a.status===0?'frozen':'normal']">{{ a.status===0?'禁用':'正常' }}</span></template>
        </Column>
        <Column header="创建时间" style="min-width:150px">
          <template #body="{ data: a }"><span class="mono-text">{{ a.createdAt?.substring(0,16) || '—' }}</span></template>
        </Column>
        <Column header="操作" style="min-width:160px;text-align:right" v-if="isRoot">
          <template #body="{ data: a }">
            <div class="row-actions" v-if="a.role !== 'ROOT'">
              <button class="nb-btn xs" @click="handleToggle(a)">{{ a.status===0?'启用':'禁用' }}</button>
              <button class="nb-btn danger xs" @click="handleRemove(a)">删除</button>
            </div>
            <span v-else style="opacity:0.3">—</span>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- 添加弹窗 -->
    <Dialog v-model:visible="showAdd" header="添加子管理员" :modal="true" :style="{ width:'420px' }">
      <form @submit.prevent="handleAdd" class="add-form">
        <div class="gf"><label class="filter-label">用户ID</label><input v-model="form.userId" class="nb-input" required placeholder="输入已有用户的ID" /></div>
        <div class="gf"><label class="filter-label">管理员姓名</label><input v-model="form.name" class="nb-input" required placeholder="显示名称" /></div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:16px;">
          <button type="button" class="nb-btn" @click="showAdd=false">取消</button>
          <button type="submit" class="nb-btn primary">确认</button>
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import { listAdmins, addAdmin, toggleAdminStatus, removeAdmin } from '@/api/admin'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const confirmDialog = useConfirm()
const loading = ref(false)
const admins = ref([])
const isRoot = ref(false)
const showAdd = ref(false)
const form = ref({ userId: '', name: '' })

async function fetchAdmins() {
  loading.value = true
  try {
    const r = await listAdmins()
    admins.value = r.data || []
    isRoot.value = admins.value.find(a => a.userId === authStore.admin?.userId)?.role === 'ROOT'
  } catch { /* */ } finally { loading.value = false }
}

async function handleAdd() {
  if (!form.value.userId || !form.value.name) {
    toast.add({ severity:'warn', summary:'必填', detail:'请填写完整信息', life:3000 }); return
  }
  try {
    await addAdmin({ userId: form.value.userId, name: form.value.name })
    toast.add({ severity:'success', summary:'完成', detail:'管理员已添加', life:3000 })
    showAdd.value = false; form.value = { userId:'', name:'' }; fetchAdmins()
  } catch { /* */ }
}

async function handleToggle(row) {
  const action = row.status === 0 ? 'enable' : 'disable'
  confirmDialog.require({
    message: `确定${action === 'enable' ? '启用' : '禁用'}管理员 "${row.name}"？`, header: '确认',
    rejectLabel:'取消', acceptLabel:'确定',
    accept: async () => {
      try { await toggleAdminStatus(row.id); toast.add({ severity:'success', summary:'完成', detail:`已${action === 'enable' ? '启用' : '禁用'}`, life:3000 }); fetchAdmins() } catch { /* */ }
    },
  })
}

async function handleRemove(row) {
  confirmDialog.require({
    message: `确定永久删除管理员 "${row.name}"？`, header: '危险操作',
    rejectLabel:'取消', acceptLabel:'删除', acceptClass:'p-button-danger',
    accept: async () => {
      try { await removeAdmin(row.id); toast.add({ severity:'success', summary:'完成', detail:'管理员已删除', life:3000 }); fetchAdmins() } catch { /* */ }
    },
  })
}

onMounted(fetchAdmins)
</script>

<style scoped>
.admin-manage { display: flex; flex-direction: column; gap: 16px; }
.page-header { margin-bottom: 4px; }
.pg-title { font-family: var(--font-headline); font-size: 28px; font-weight: 900; text-transform: uppercase; font-style: italic; }
.pg-sub { font-size: 13px; font-weight: 700; opacity: 0.5; text-transform: uppercase; letter-spacing: 1px; }
.action-bar { display: flex; gap: 8px; }
.table-wrap { overflow-x: auto; }
.mono-text { font-family: var(--font-mono); font-size: 11px; font-weight: 700; }
.cell-name { font-family: var(--font-headline); font-weight: 700; font-size: 14px; text-transform: uppercase; }
.row-actions { display: flex; gap: 4px; justify-content: flex-end; }
.add-form { display: flex; flex-direction: column; gap: 12px; }
.gf { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
</style>
