<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-icon">🔍</div>
      <h1 class="nf-title">404</h1>
      <p class="nf-desc">页面不存在</p>
      <button class="brutal-btn primary" @click="$router.push('/discover')">
        <i class="pi pi-compass"></i> 返回首页
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-primary);
}
.nf-content { text-align: center; }
.nf-icon { font-size: 64px; margin-bottom: 16px; }
.nf-title { font-size: 80px; font-weight: 900; color: #1A1A1A; line-height: 1; }
.nf-desc {
  font-size: 18px; font-weight: 700; color: #888; margin: 12px 0 24px;
  text-transform: uppercase; letter-spacing: 2px;
}
</style>
