<template>
  <div class="nf-page">
    <span class="blob blob-1"></span>
    <span class="blob blob-2"></span>

    <div class="nf-card ch-fade-up">
      <p class="code">404</p>
      <h1>页面走丢了</h1>
      <p class="sub">
        你访问的页面不存在，或者已经被移动。检查一下地址，或者回到首页重新开始。
      </p>
      <div class="nf-actions">
        <el-button type="primary" size="large" round @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon> 返回首页
        </el-button>
        <el-button size="large" round @click="$router.push('/courses')">
          浏览课程库
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 404 页面
 *
 * 用渐变数字 + 光晕营造“迷路”的观感，同时给出两个明确的出口，
 * 避免用户卡在死页面。
 */
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.nf-page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ch-gradient-hero);
  padding: 24px;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.blob-1 {
  width: 420px;
  height: 420px;
  top: -140px;
  right: -80px;
  background: rgba(168, 85, 247, 0.5);
  animation: ch-float 10s ease-in-out infinite;
}

.blob-2 {
  width: 360px;
  height: 360px;
  bottom: -160px;
  left: -80px;
  background: rgba(56, 189, 248, 0.42);
  animation: ch-float 12s ease-in-out infinite reverse;
}

.nf-card {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 520px;
}

.code {
  margin: 0;
  font-size: 128px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: -0.04em;
  background: linear-gradient(120deg, #a5b4fc 0%, #c4b5fd 45%, #f0abfc 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.nf-card h1 {
  margin: 12px 0 14px;
  font-size: 26px;
  font-weight: 800;
  color: #fff;
}

.sub {
  margin: 0 0 30px;
  font-size: 14.5px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.72);
}

.nf-actions {
  display: flex;
  gap: 14px;
  justify-content: center;
  flex-wrap: wrap;
}

.nf-actions :deep(.el-button:not(.el-button--primary)) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.28);
  color: #fff;
}

.nf-actions :deep(.el-button:not(.el-button--primary):hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.45);
  color: #fff;
}
</style>
