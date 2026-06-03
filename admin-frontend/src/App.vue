<template>
  <div id="admin-app" v-if="authStore.isLoggedIn">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <h1 class="sidebar-title">SkillMatch<br/><span class="sidebar-badge">Admin</span></h1>
        <p class="sidebar-subtitle">管理控制台</p>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/dashboard" class="nav-item" active-class="nav-active">
          <span class="material-symbols-outlined">dashboard</span>
          <span>数据看板</span>
        </router-link>
        <router-link to="/users" class="nav-item" active-class="nav-active">
          <span class="material-symbols-outlined">group</span>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admins" class="nav-item" active-class="nav-active">
          <span class="material-symbols-outlined">shield_person</span>
          <span>管理员</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button class="nav-item logout-btn" @click="handleLogout">
          <span class="material-symbols-outlined">logout</span>
          <span>退出登录</span>
        </button>
        <div class="sidebar-user">
          <span class="material-symbols-outlined" style="font-size:32px;">account_circle</span>
          <div>
            <p class="sidebar-user-name">{{ authStore.admin?.name || 'Admin' }}</p>
            <p class="sidebar-user-role">{{ authStore.admin?.role || 'Administrator' }}</p>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <span class="topbar-title">管理<span class="topbar-accent">控制台</span></span>
          <div class="topbar-search">
            <span class="material-symbols-outlined search-icon">search</span>
            <input class="search-input" type="text" placeholder="搜索用户、技能..." />
          </div>
        </div>
        <div class="topbar-right">
          <div class="topbar-avatar">
            <span class="material-symbols-outlined">account_circle</span>
            <span class="topbar-avatar-name">{{ authStore.admin?.name || 'Admin' }}</span>
          </div>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="main-content">
        <router-view />
      </main>

      <!-- 页脚 -->
      <footer class="app-footer">
        <div class="footer-left">
          <span class="footer-dot"></span>
          <span>系统运行中</span>
          <span class="footer-divider"></span>
          <span class="footer-dim">同步: 2分钟前</span>
        </div>
        <div class="footer-version">V2.4.1-STABLE</div>
      </footer>
    </div>

    <Toast position="top-right" />
    <ConfirmDialog />
  </div>

  <!-- 登录页（无侧边栏） -->
  <div v-else class="admin-guest">
    <router-view />
    <Toast position="top-center" />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import { useToast } from 'primevue/usetoast'
import { setToastInstance } from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()
const primeToast = useToast()
setToastInstance(primeToast)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style>
#admin-app {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* 登录/注册页容器 — 纵向弹性，撑满全屏 */
.admin-guest {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

/* ===== 侧边栏 ===== */
.sidebar {
  position: fixed; left: 0; top: 0; height: 100vh; width: var(--sidebar-w);
  background: #000; color: #fff; display: flex; flex-direction: column;
  padding: 16px; z-index: 100; border-right: 2px solid #000;
  overflow-y: auto;
}
.sidebar-brand { margin-bottom: 32px; padding: 8px; }
.sidebar-title {
  font-family: var(--font-headline); font-size: 24px; font-weight: 800;
  text-transform: uppercase; line-height: 1.1; color: var(--yellow);
}
.sidebar-badge {
  background: var(--yellow); color: #000; padding: 0 4px;
}
.sidebar-subtitle {
  font-size: 11px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 2px; opacity: 0.5; margin-top: 4px;
}

.sidebar-nav { flex: 1; display: flex; flex-direction: column; gap: 4px; }

.nav-item {
  display: flex; align-items: center; gap: 8px; padding: 10px 14px;
  color: #ccc; font-weight: 700; font-size: 13px; text-transform: uppercase;
  text-decoration: none; transition: all 0.15s; border: 2px solid transparent;
}
.nav-item:hover { color: #fff; background: rgba(255,255,255,0.06); }
.nav-active {
  background: var(--yellow); color: #000; border-color: #000;
  box-shadow: var(--shadow-sm);
}
.nav-active:hover { background: var(--yellow); color: #000; }
.nav-active .material-symbols-outlined { color: #000; }

.sidebar-footer { margin-top: auto; border-top: 2px solid #333; padding-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.logout-btn {
  background: none; border: 2px solid transparent; cursor: pointer;
  font-family: var(--font-body); text-align: left; width: 100%;
}
.logout-btn:hover { color: #FF6B6B; border-color: transparent; background: rgba(255,0,0,0.1); }
.sidebar-user {
  display: flex; align-items: center; gap: 10px; padding: 10px;
  border: 2px solid #333; margin-top: 4px;
}
.sidebar-user-name { font-family: var(--font-headline); font-weight: 700; font-size: 13px; text-transform: uppercase; }
.sidebar-user-role { font-size: 10px; font-weight: 700; opacity: 0.5; text-transform: uppercase; }

/* ===== 主内容区 ===== */
.main-wrapper {
  margin-left: var(--sidebar-w); flex: 1; display: flex; flex-direction: column;
  min-height: 100vh;
}

/* ===== 顶栏 ===== */
.topbar {
  display: flex; align-items: center; justify-content: space-between;
  height: 64px; padding: 0 24px; background: #fff;
  border-bottom: var(--border-thick); position: sticky; top: 0; z-index: 50;
}
.topbar-left { display: flex; align-items: center; gap: 24px; }
.topbar-title {
  font-family: var(--font-headline); font-size: 26px; font-weight: 900;
  text-transform: uppercase; letter-spacing: -1px;
}
.topbar-accent { background: var(--yellow); padding: 0 6px; font-style: normal; }
.topbar-search { position: relative; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: #000; font-size: 20px !important; }
.search-input {
  width: 300px; padding: 8px 12px 8px 36px; font-family: var(--font-body);
  font-weight: 700; font-size: 13px; border: var(--border-thick); background: #fff; outline: none;
  transition: background 0.15s, transform 0.15s;
}
.search-input:focus { background: var(--yellow); transform: scale(1.05); }
.search-input::placeholder { color: #999; }

.topbar-right { display: flex; align-items: center; gap: 12px; }
.topbar-avatar {
  display: flex; align-items: center; gap: 8px; padding: 4px 12px 4px 4px;
  border: var(--border-thick); cursor: pointer; margin-left: 8px;
}
.topbar-avatar-name { font-weight: 700; font-size: 13px; text-transform: uppercase; }

/* ===== 主内容 ===== */
.main-content { flex: 1; padding: 24px; }

/* ===== 页脚 ===== */
.app-footer {
  margin-top: auto; padding: 16px 24px; border-top: var(--border-thick);
  background: #fff; display: flex; justify-content: space-between; align-items: center;
}
.footer-left { display: flex; align-items: center; gap: 12px; }
.footer-dot { width: 12px; height: 12px; background: var(--green); border: 2px solid #000; }
.footer-left span { font-weight: 700; font-size: 12px; text-transform: uppercase; }
.footer-divider { width: 3px; height: 16px; background: #000; transform: rotate(12deg); }
.footer-dim { opacity: 0.5; }
.footer-version {
  font-family: var(--font-mono); font-weight: 700; font-size: 12px;
  background: #000; color: #fff; padding: 4px 10px; font-style: italic;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .search-input { width: 180px; }
}
@media (max-width: 768px) {
  .sidebar { display: none; }
  .main-wrapper { margin-left: 0; }
  .topbar { padding: 0 12px; }
  .topbar-search { display: none; }
  .topbar-title { font-size: 18px; }
  .main-content { padding: 12px; }
}
</style>
