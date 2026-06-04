<template>
  <div class="user-list">
    <!-- 筛选区 -->
    <section class="filter-section nb-card">
      <div class="filter-grid">
        <div class="filter-col-2">
          <label class="filter-label">地理筛选</label>
          <div class="filter-row">
            <input v-model="q.centerLat" class="nb-input" placeholder="lat (e.g. 39.9)" type="number" />
            <input v-model="q.centerLng" class="nb-input" placeholder="lng (e.g. 116.4)" type="number" />
            <div class="radius-wrap">
              <span class="material-symbols-outlined" style="font-size:18px;">distance</span>
              <input v-model="q.radiusKm" class="nb-input radius-input" type="number" placeholder="10" />
              <span class="radius-unit">KM</span>
            </div>
          </div>
        </div>
        <div class="filter-col">
          <label class="filter-label">状态与标签</label>
          <select v-model="q.status" class="nb-input filter-select" @change="search">
            <option :value="null">全部状态</option>
            <option :value="1">正常</option>
            <option :value="2">冻结</option>
          </select>
        </div>
        <div class="filter-col">
          <label class="filter-label">注册时间</label>
          <input v-model="q.dateStart" type="date" class="nb-input" />
        </div>
      </div>
      <!-- 快捷筛选 + 操作按钮 -->
      <div class="filter-actions">
        <div class="quick-btns">
          <button v-for="b in quickBtns" :key="b.val" class="nb-btn sm" :class="{ primary: q.quickFilter === b.val }" @click="q.quickFilter = q.quickFilter===b.val ? '' : b.val; search()">{{ b.label }}</button>
        </div>
        <div class="action-btns">
          <button class="nb-btn primary sm" @click="openCreate()"><span class="material-symbols-outlined">add_circle</span> 新增用户</button>
          <button class="nb-btn danger sm" @click="handleDeleteSelected" :disabled="!selectedRows.length"><span class="material-symbols-outlined">delete_sweep</span> 删除选中</button>
          <button class="nb-btn danger sm" @click="handleDeleteAllRobots"><span class="material-symbols-outlined">delete_forever</span> 删除全部机器人</button>
          <button class="nb-btn sm" @click="exportExcel" title="导出Excel"><span class="material-symbols-outlined">ios_share</span></button>
        </div>
      </div>
      <!-- 搜索框 -->
      <div class="search-row">
        <input v-model="q.keyword" class="nb-input" placeholder="搜索昵称、ID、技能..." @keyup.enter="search" />
        <button class="nb-btn dark sm" @click="search"><span class="material-symbols-outlined">search</span> 搜索</button>
        <button class="nb-btn sm" @click="reset"><span class="material-symbols-outlined">refresh</span> 重置</button>
      </div>
    </section>

    <!-- 数据表格 -->
    <div class="table-wrap nb-card" style="padding:0;">
      <DataTable
        :value="users" v-model:selection="selectedRows" dataKey="userId"
        :loading="loading" lazy :paginator="true" :rows="q.size"
        :totalRecords="total" :rowsPerPageOptions="[10,20,50]"
        :first="firstOffset"
        @page="onPage" selectionMode="multiple"
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
        currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"
      >
        <Column selectionMode="multiple" headerStyle="width:40px" />
        <Column header="用户" style="min-width:200px">
          <template #body="{ data: u }">
            <div class="user-cell">
              <img v-if="!u.robot" :src="u.avatarUrl || defaultAvatar(u.userId)" class="cell-avatar" />
              <span v-else class="material-symbols-outlined cell-bot-icon">smart_toy</span>
              <div>
                <div class="cell-name">{{ u.name }}</div>
                <div class="cell-id">{{ u.robot ? '#BOT-' : '#USR-' }}{{ (u.userId||'').slice(0,8) }}</div>
              </div>
            </div>
          </template>
        </Column>
        <Column header="类型" style="width:80px">
          <template #body="{ data: u }"><span :class="['nb-tag', u.robot?'robot':'real']">{{ u.robot?'机器人':'真人' }}</span></template>
        </Column>
        <Column header="坐标" style="min-width:140px">
          <template #body="{ data: u }"><span class="mono-text">{{ u.latitude?.toFixed(4) || '—' }}, {{ u.longitude?.toFixed(4) || '—' }}</span></template>
        </Column>
        <Column header="注册时间" style="min-width:140px">
          <template #body="{ data: u }"><span class="mono-text">{{ u.createdAt?.substring(0,16) || '—' }}</span></template>
        </Column>
        <Column header="状态" style="width:90px">
          <template #body="{ data: u }">
            <span :class="['nb-tag', u.status===2?'frozen':'normal']">{{ u.status===2?'冻结':'正常' }}</span>
            <div v-if="u.status===2&&u.unfreezeDate" class="unfreeze-hint">至: {{ u.unfreezeDate?.substring(0,10) }}</div>
          </template>
        </Column>
        <Column header="操作" style="min-width:160px;text-align:right">
          <template #body="{ data: u }">
            <div class="row-actions">
              <button class="nb-btn xs" @click="openDetail(u)"><span class="material-symbols-outlined" style="font-size:18px;">visibility</span></button>
              <button class="nb-btn xs" @click="openEdit(u)"><span class="material-symbols-outlined" style="font-size:18px;">edit</span></button>
              <button v-if="u.status!==2" class="nb-btn danger xs" @click="openFreeze(u)"><span class="material-symbols-outlined" style="font-size:18px;">block</span></button>
              <button v-else class="nb-btn xs" style="background:var(--green)" @click="handleUnfreeze(u)"><span class="material-symbols-outlined" style="font-size:18px;">lock_open</span></button>
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- ===== 详情弹窗 ===== -->
    <Dialog v-model:visible="dlg.detail" header="用户详情" :modal="true" :style="{ width:'500px' }">
      <div class="detail-grid" v-if="dlg.data">
        <div><strong>ID:</strong> {{ dlg.data.userId }}</div>
        <div><strong>昵称:</strong> {{ dlg.data.name }}</div>
        <div><strong>类型:</strong> {{ dlg.data.robot?'🤖 机器人':'👤 真人' }}</div>
        <div><strong>状态:</strong> {{ dlg.data.status===2?'❄ 冻结':'✅ 正常' }}</div>
        <div><strong>城市:</strong> {{ dlg.data.city||'-' }}</div>
        <div><strong>坐标:</strong> {{ dlg.data.latitude?.toFixed(4) }}, {{ dlg.data.longitude?.toFixed(4) }}</div>
        <div><strong>技能:</strong> {{ dlg.data.skillTags||'-' }}</div>
        <div><strong>兴趣:</strong> {{ dlg.data.hobbyTags||'-' }}</div>
        <div><strong>点赞:</strong> {{ dlg.data.likeCount||0 }} <strong>帖子:</strong> {{ dlg.data.postCount||0 }}</div>
        <div><strong>注册:</strong> {{ dlg.data.createdAt||'-' }}</div>
      </div>
    </Dialog>

    <!-- ===== Ban 确认弹窗 ===== -->
    <Dialog v-model:visible="dlg.ban" header="冻结用户" :modal="true" :style="{ width:'480px' }">
      <div class="ban-form">
        <label class="filter-label">冻结原因</label>
        <textarea v-model="banReason" class="nb-input" rows="3" placeholder="输入原因（如：违反服务条款...）"></textarea>
      </div>
      <template #footer>
        <button class="nb-btn" @click="dlg.ban=false">取消</button>
        <button class="nb-btn dark" @click="handleFreeze">确认冻结</button>
      </template>
    </Dialog>

    <!-- ===== 新增/编辑弹窗 ===== -->
    <Dialog v-model:visible="dlg.form" :header="dlg.editing?'编辑用户':'新增用户'" :modal="true" :style="{ width:'500px' }" @hide="resetForm">
      <form @submit.prevent="handleSave" class="edit-form">
        <div class="gf"><label class="filter-label">昵称</label><input v-model="f.name" class="nb-input" required /></div>
        <div class="gf" v-if="!dlg.editing"><label class="filter-label">用户ID</label><input v-model="f.userId" class="nb-input" placeholder="留空自动生成" /></div>
        <div class="gf" v-if="!dlg.editing && !f.robot"><label class="filter-label">密码</label><input v-model="f.password" type="password" class="nb-input" placeholder="6-20字符" /></div>
        <div class="gf"><label class="filter-label">联系方式</label><input v-model="f.contactInfo" class="nb-input" /></div>
        <div class="gf"><label class="filter-label">头像</label>
          <div class="avatar-upload-row">
            <input v-model="f.avatarUrl" class="nb-input" placeholder="头像URL" />
            <label class="nb-btn sm" style="cursor:pointer;flex-shrink:0;">
              <span class="material-symbols-outlined" style="font-size:16px;">upload</span> 上传
              <input type="file" accept="image/*" hidden @change="handleAvatarUpload" />
            </label>
          </div>
        </div>
        <div class="gf"><label class="filter-label">简介</label><textarea v-model="f.bio" class="nb-input" rows="2"></textarea></div>
        <div class="gf" v-if="!dlg.editing"><label class="filter-label">能教的技能</label>
          <div class="tag-checkbox-list">
            <label v-for="s in tagPool.skills" :key="'can-'+s" class="tag-checkbox" :class="{ dimmed: f.selectedWantSkills.includes(s) }">
              <input type="checkbox" :value="s" v-model="f.selectedCanSkills" :disabled="f.selectedWantSkills.includes(s)" /> {{ s }}
            </label>
          </div>
        </div>
        <div class="gf" v-if="!dlg.editing"><label class="filter-label">想学的技能</label>
          <div class="tag-checkbox-list">
            <label v-for="s in tagPool.skills" :key="'want-'+s" class="tag-checkbox" :class="{ dimmed: f.selectedCanSkills.includes(s) }">
              <input type="checkbox" :value="s" v-model="f.selectedWantSkills" :disabled="f.selectedCanSkills.includes(s)" /> {{ s }}
            </label>
          </div>
        </div>
        <div class="gf" v-if="!dlg.editing"><label class="filter-label">兴趣爱好</label>
          <div class="tag-checkbox-list">
            <label v-for="h in tagPool.hobbies" :key="h.name" class="tag-checkbox">
              <input type="checkbox" :value="h" v-model="f.selectedHobbies" />
              <span class="material-symbols-outlined" style="font-size:14px;">{{ h.icon }}</span> {{ h.name }}
            </label>
          </div>
        </div>
        <div class="gf-row">
          <div class="gf"><label class="filter-label">纬度</label><input v-model.number="f.latitude" class="nb-input" placeholder="如 39.9" /></div>
          <div class="gf"><label class="filter-label">经度</label><input v-model.number="f.longitude" class="nb-input" placeholder="如 116.4" /></div>
          <div class="gf"><label class="filter-label">城市</label>
            <div class="city-input-row">
              <input v-model="f.city" class="nb-input" placeholder="或选择城市" />
              <button type="button" class="nb-btn xs" @click="showCityPicker = true"><span class="material-symbols-outlined" style="font-size:16px;">location_on</span></button>
            </div>
          </div>
        </div>
        <div class="gf"><label class="filter-label">类型</label>
          <div class="radio-row">
            <label><input type="radio" :value="false" v-model="f.robot" @change="onTypeChange" /> 真人</label>
            <label><input type="radio" :value="true" v-model="f.robot" @change="onTypeChange" /> 机器人</label>
          </div>
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
          <button type="button" class="nb-btn" @click="dlg.form=false">取消</button>
          <button type="submit" class="nb-btn primary">{{ dlg.editing?'保存':'创建' }}</button>
        </div>
      </form>
    </Dialog>

    <!-- ===== 城市选择弹窗 ===== -->
    <Dialog v-model:visible="showCityPicker" header="选择城市" :modal="true" :style="{ width:'420px' }">
      <div class="city-picker-grid">
        <button v-for="c in cityList" :key="c.name" type="button" class="city-pick-btn" @click="pickCity(c)">{{ c.name }}</button>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import { listUsers, getUserDetail, createUser, updateUser, updateUserStatus,
  batchDeleteRobots, getSkillTags, getHobbyTags } from '@/api/admin'
import * as XLSX from 'xlsx'

const toast = useToast()
const confirmDialog = useConfirm()
const loading = ref(false)
const users = ref([])
const total = ref(0)
const selectedRows = ref([])
const firstOffset = ref(0)
const selectedIds = computed(() => selectedRows.value.map(u => u.userId))

const q = reactive({ page:1, size:20, keyword:'', status:null, centerLat:null, centerLng:null, radiusKm:null, quickFilter:'', dateStart:'' })
const quickBtns = [
  { label:'全部真人', val:'all_real' }, { label:'全部机器人', val:'all_robot' },
  { label:'已冻结', val:'all_frozen' }, { label:'待解冻', val:'pending_unfreeze' },
  { label:'无坐标', val:'no_coords' },
]

const dlg = reactive({ detail:false, data:null, ban:false, banUserId:'', form:false, editing:false, robot:false })
const banReason = ref('')
const f = reactive({ userId:'', name:'', password:'', contactInfo:'', avatarUrl:'', signature:'', bio:'', latitude:null, longitude:null, city:'', robot:false, selectedCanSkills:[], selectedWantSkills:[], selectedHobbies:[] })
const tagPool = reactive({ skills:[], hobbies:[] })
const showCityPicker = ref(false)
const saving = ref(false)
const cityList = [
  { name:'北京', lat:39.9, lng:116.4 },{ name:'上海', lat:31.2, lng:121.5 },{ name:'广州', lat:23.1, lng:113.3 },
  { name:'深圳', lat:22.5, lng:114.1 },{ name:'杭州', lat:30.3, lng:120.2 },{ name:'成都', lat:30.7, lng:104.1 },
  { name:'武汉', lat:30.6, lng:114.3 },{ name:'南京', lat:32.1, lng:118.8 },{ name:'重庆', lat:29.6, lng:106.6 },
  { name:'天津', lat:39.1, lng:117.2 },{ name:'苏州', lat:31.3, lng:120.6 },{ name:'西安', lat:34.3, lng:108.9 },
  { name:'长沙', lat:28.2, lng:113.0 },{ name:'郑州', lat:34.8, lng:113.6 },{ name:'青岛', lat:36.1, lng:120.4 },
  { name:'沈阳', lat:41.8, lng:123.4 },{ name:'哈尔滨', lat:45.8, lng:126.5 },{ name:'昆明', lat:25.0, lng:102.7 },
  { name:'福州', lat:26.1, lng:119.3 },{ name:'厦门', lat:24.5, lng:118.1 },{ name:'合肥', lat:31.8, lng:117.3 },
  { name:'济南', lat:36.7, lng:117.0 },{ name:'南宁', lat:22.8, lng:108.4 },{ name:'贵阳', lat:26.6, lng:106.6 },
]

function defaultAvatar(id) { return `https://ui-avatars.com/api/?name=${encodeURIComponent(id||'U')}&background=000&color=fff&size=64` }

function onPage(e) { firstOffset.value = e.first; q.page = e.page + 1; search(false) }

async function search(rp=true) {
  if (rp) { q.page = 1; firstOffset.value = 0 }
  loading.value = true
  try {
    const p = {}
    Object.keys(q).forEach(k => { if (q[k] !== '' && q[k] !== null) p[k] = q[k] })
    const r = await listUsers(p)
    users.value = r.data?.list || []
    total.value = r.data?.total || 0
    console.log('[UserList] 查询结果:', 'total=' + total.value, 'list=' + users.value.length, 'params=', p)
  } catch (e) { console.error('[UserList] 查询失败:', e) } finally { loading.value = false }
}

function reset() {
  Object.assign(q, { page:1, size:20, keyword:'', status:null, centerLat:null, centerLng:null, radiusKm:null, quickFilter:'', dateStart:'' })
  firstOffset.value = 0; search()
}
function resetForm() { dlg.editing = false; Object.assign(f, { userId:'', name:'', password:'', contactInfo:'', avatarUrl:'', signature:'', bio:'', latitude:null, longitude:null, city:'', robot:false, selectedCanSkills:[], selectedWantSkills:[], selectedHobbies:[] }) }

async function openDetail(u) { try { dlg.data = (await getUserDetail(u.userId)).data; dlg.detail = true } catch { /* */ } }
function openFreeze(u) { dlg.banUserId = u.userId; banReason.value = ''; dlg.ban = true }
async function handleFreeze() {
  try { await updateUserStatus({ userId: dlg.banUserId, status: 2, reason: banReason.value }); toast.add({ severity:'success', summary:'完成', detail:'用户已冻结', life:3000 }); dlg.ban = false; search() } catch { /* */ }
}
async function handleUnfreeze(u) {
  try { await updateUserStatus({ userId: u.userId, status: 1 }); toast.add({ severity:'success', summary:'完成', detail:'用户已解冻', life:3000 }); search() } catch { /* */ }
}

async function openCreate() {
  resetForm()
  try {
    const [sr, hr] = await Promise.all([getSkillTags(), getHobbyTags()])
    tagPool.skills = Object.values(sr.data || {}).flat()
    tagPool.hobbies = Object.values(hr.data || {}).flat().map(h => ({ name: h.name || h, icon: h.icon || 'interests' }))
  } catch { /* */ }
  dlg.form = true
}
function openEdit(u) {
  dlg.editing = true
  Object.assign(f, { userId:u.userId, name:u.name||'', contactInfo:u.contactInfo||'', avatarUrl:u.avatarUrl||'', signature:u.signature||'', bio:u.bio||'', latitude:u.latitude, longitude:u.longitude, city:u.city||'', robot:u.robot||false })
  dlg.form = true
}
async function handleSave() {
  if (!f.name) { toast.add({ severity:'warn', summary:'必填', detail:'请输入昵称', life:3000 }); return }
  saving.value = true
  try {
    const payload = { ...f }
    if (!dlg.editing) {
      payload.skillTags = f.selectedCanSkills
      payload.wantSkillTags = f.selectedWantSkills
      payload.hobbyTags = f.selectedHobbies.map(h => ({ name: h.name, icon: h.icon }))
    }
    if (dlg.editing) await updateUser(f.userId, payload)
    else await createUser(payload)
    toast.add({ severity:'success', summary:'完成', detail: dlg.editing?'已更新':'已创建', life:3000 })
    dlg.form = false; search()
  } catch { /* */ } finally { saving.value = false }
}

function onTypeChange() { if (f.robot) f.password = '' }

async function handleAvatarUpload(e) {
  const file = e.target.files?.[0]; if (!file) return
  try { const r = await uploadAvatar(file); f.avatarUrl = r.data || ''; toast.add({ severity:'success', summary:'完成', detail:'头像上传成功', life:3000 }) } catch { /* */ }
}

function pickCity(c) { f.latitude = c.lat; f.longitude = c.lng; f.city = c.name; showCityPicker.value = false }

// 删除选中的用户
async function handleDeleteSelected() {
  const ids = selectedIds.value
  if (!ids.length) { toast.add({ severity:'warn', summary:'提示', detail:'请先勾选用户', life:3000 }); return }
  confirmDialog.require({
    message: `确定永久删除 ${ids.length} 个选中的用户？此操作不可恢复。`, header: '危险操作',
    rejectLabel:'取消', acceptLabel:'确认删除', acceptClass:'p-button-danger',
    accept: async () => {
      try { await batchDeleteRobots(ids); toast.add({ severity:'success', summary:'完成', detail:`已删除 ${ids.length} 个`, life:3000 }); selectedRows.value=[]; search() } catch { /* */ }
    },
  })
}

// 一键删除全部机器人
async function handleDeleteAllRobots() {
  confirmDialog.require({
    message: '确定要删除全部机器人吗？此操作不可恢复，建议先导出数据备份。', header: '⚠️ 危险操作',
    rejectLabel:'取消', acceptLabel:'全部删除', acceptClass:'p-button-danger',
    accept: async () => {
      try {
        // 先拉取全部机器人 ID
        const r = await listUsers({ quickFilter: 'all_robot', page: 1, size: 99999 })
        const ids = (r.data?.list || []).map(u => u.userId)
        if (!ids.length) { toast.add({ severity:'info', summary:'提示', detail:'没有可删除的机器人', life:3000 }); return }
        await batchDeleteRobots(ids)
        toast.add({ severity:'success', summary:'完成', detail:`已删除 ${ids.length} 个机器人`, life:3000 })
        search()
      } catch { /* */ }
    },
  })
}

async function exportExcel() {
  try {
    // 导出当前筛选后的用户列表
    const allP = { ...q, page: 1, size: 99999 }  // 获取全部数据
    const r = await listUsers(allP)
    const rows = (r.data?.list || []).map(u => ({
      ID: u.userId, 昵称: u.name, 类型: u.robot ? '机器人' : '真人',
      状态: u.status === 2 ? '冻结' : '正常',
      技能: u.skillTags || '', 兴趣: u.hobbyTags || '',
      城市: u.city || '', 纬度: u.latitude, 经度: u.longitude,
      注册时间: u.createdAt || '',
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, 'Users')
    XLSX.writeFile(wb, `users_${new Date().toISOString().substring(0,10)}.xlsx`)
    toast.add({ severity:'success', summary:'完成', detail:`已导出 ${rows.length} 条记录`, life:3000 })
  } catch { /* */ }
}

search()
</script>

<style scoped>
.user-list { display: flex; flex-direction: column; gap: 16px; }

/* ===== 筛选区 ===== */
.filter-section { padding: 16px; }
.filter-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 16px; margin-bottom: 12px; }
.filter-col, .filter-col-2 { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; }
.filter-row { display: flex; gap: 8px; align-items: center; }
.radius-wrap { display: flex; align-items: center; border: var(--border); background: #fff; padding: 0 8px; gap: 4px; }
.radius-input { border: none !important; width: 60px; text-align: center; font-weight: 700; }
.radius-input:focus { background: transparent !important; }
.radius-unit { font-size: 12px; font-weight: 700; }
.filter-select { appearance: none; font-weight: 700; }

.filter-actions {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 12px; border-top: 2px solid #000; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;
}
.quick-btns { display: flex; gap: 6px; flex-wrap: wrap; }
.action-btns { display: flex; gap: 6px; }
.search-row { display: flex; gap: 8px; }
.search-row .nb-input { flex: 1; }

/* ===== 表格 ===== */
.table-wrap { overflow-x: auto; }
.user-cell { display: flex; align-items: center; gap: 12px; }
.cell-avatar { width: 40px; height: 40px; border: 2px solid #000; object-fit: cover; }
.cell-bot-icon { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #000; color: var(--yellow); font-size: 24px !important; border: 2px solid #000; }
.cell-name { font-family: var(--font-headline); font-weight: 700; font-size: 14px; text-transform: uppercase; }
.cell-id { font-size: 11px; font-weight: 600; opacity: 0.5; }
.mono-text { font-family: var(--font-mono); font-size: 11px; font-weight: 700; }
.unfreeze-hint { font-size: 9px; font-weight: 700; color: var(--red); text-transform: uppercase; }
.row-actions { display: flex; gap: 4px; justify-content: flex-end; }

/* ===== 弹窗表单 ===== */
.edit-form { display: flex; flex-direction: column; gap: 12px; }
.gf { display: flex; flex-direction: column; gap: 4px; }
.gf-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
.radio-row { display: flex; gap: 20px; }
.radio-row label { display: flex; align-items: center; gap: 4px; font-weight: 700; font-size: 14px; cursor: pointer; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; font-size: 13px; }
.detail-grid strong { opacity: 0.6; }
.ban-form { display: flex; flex-direction: column; gap: 4px; }
.tag-checkbox-list { max-height: 150px; overflow-y: auto; border: var(--border); padding: 8px; display: flex; flex-wrap: wrap; gap: 6px; }
.tag-checkbox { font-size: 12px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 3px; white-space: nowrap; }
.tag-checkbox input { accent-color: var(--yellow); }
.tag-checkbox.dimmed { opacity: 0.35; text-decoration: line-through; }
.avatar-upload-row { display: flex; gap: 8px; align-items: center; }
.avatar-upload-row .nb-input { flex: 1; }
.city-input-row { display: flex; gap: 4px; align-items: center; }
.city-input-row .nb-input { flex: 1; }
.city-picker-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; max-height: 300px; overflow-y: auto; }
.city-pick-btn { padding: 6px 4px; font-size: 12px; font-weight: 700; cursor: pointer; border: var(--border); background: #fff; text-align: center; }
.city-pick-btn:hover { background: var(--yellow); }

@media (max-width: 1024px) { .filter-grid { grid-template-columns: 1fr 1fr; } }
@media (max-width: 768px) { .filter-grid { grid-template-columns: 1fr; } .gf-row { grid-template-columns: 1fr; } .filter-actions { flex-direction: column; } }
</style>
