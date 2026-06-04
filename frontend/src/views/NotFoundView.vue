<template>
  <div class="flex-center" style="flex-direction:column;height:70vh;gap:16px;">
    <h1 style="font-size:72px;font-weight:900;color:var(--color-yellow);">404</h1>
    <p style="color:#666;">页面不存在</p>
    <el-button type="primary" @click="$router.push('/discover')">返回首页</el-button>
  </div>
</template>
