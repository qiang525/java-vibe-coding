<template>
  <div class="nf-page">
    <div class="nf-card">
      <h1>404</h1>
      <p>你访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 404 页面
 */
</script>

<style scoped>
.nf-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ch-bg-page);
}

.nf-card {
  text-align: center;
}

.nf-card h1 {
  margin: 0;
  font-size: 72px;
  font-weight: 700;
  color: var(--ch-border);
  line-height: 1;
}

.nf-card p {
  margin: 12px 0 24px;
  color: var(--ch-text-secondary);
}
</style>
