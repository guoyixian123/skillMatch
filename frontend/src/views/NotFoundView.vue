<template>
  <div class="not-found-page">
    <div class="decor-shapes">
      <div class="nf-circle nf-1"></div>
      <div class="nf-circle nf-2"></div>
      <div class="nf-triangle nf-3"></div>
      <div class="nf-dot nf-4"></div>
      <div class="nf-dot nf-5"></div>
    </div>
    <div class="nf-content">
      <div class="nf-number">404</div>
      <p class="nf-text">页面不存在</p>
      <button class="geo-btn primary" @click="$router.push('/discover')">
        返回首页 <span style="margin-left:4px;">→</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 70vh;
  position: relative;
  overflow: hidden;
}
.decor-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.nf-circle {
  position: absolute;
  border-radius: 9999px;
}
.nf-1 {
  top: 10%; left: 10%;
  width: 120px; height: 120px;
  background: var(--color-tertiary);
  opacity: 0.2;
  animation: floatSlow 7s ease-in-out infinite;
}
.nf-2 {
  bottom: 15%; right: 12%;
  width: 80px; height: 80px;
  background: var(--color-secondary);
  opacity: 0.2;
  animation: floatSlow 9s ease-in-out infinite reverse;
}
.nf-3 {
  top: 20%; right: 20%;
  width: 0; height: 0;
  border-left: 16px solid transparent;
  border-right: 16px solid transparent;
  border-bottom: 28px solid var(--color-quaternary);
  opacity: 0.3;
  animation: wiggle 4s ease-in-out infinite;
}
.nf-4 {
  top: 40%; left: 20%;
  width: 14px; height: 14px;
  background: var(--color-accent);
  border-radius: 9999px;
  opacity: 0.3;
  animation: popIn 2s ease-in-out infinite;
}
.nf-5 {
  bottom: 30%; left: 30%;
  width: 10px; height: 10px;
  background: var(--color-secondary);
  border-radius: 9999px;
  opacity: 0.3;
  animation: popIn 2.5s ease-in-out infinite 0.5s;
}

.nf-content {
  position: relative;
  z-index: 1;
  text-align: center;
  animation: popEntry 0.5s var(--ease-bounce) both;
}
.nf-number {
  font-family: var(--font-heading);
  font-size: 96px;
  font-weight: 800;
  color: var(--color-accent);
  line-height: 1;
  margin-bottom: 8px;
}
.nf-text {
  color: var(--color-muted-fg);
  font-size: 18px;
  margin-bottom: 24px;
}

@keyframes floatSlow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}
@keyframes wiggle {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(8deg); }
  75% { transform: rotate(-8deg); }
}
@keyframes popIn {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.5); opacity: 0.6; }
}
@keyframes popEntry {
  0% { opacity: 0; transform: scale(0.8) translateY(20px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
