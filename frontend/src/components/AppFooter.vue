<template>
  <footer class="site-footer">
    <div class="ch-container footer-inner">
      <div class="foot-brand">
        <span class="foot-logo">CH</span>
        <div>
          <p class="foot-name">CourseHub</p>
          <p class="foot-slogan">让每一份资料都有出处</p>
        </div>
      </div>

      <div class="foot-links">
        <router-link to="/courses">课程库</router-link>
        <router-link to="/resources">资料库</router-link>
        <a href="/doc.html" target="_blank" rel="noopener">接口文档</a>
      </div>

      <p class="foot-copy">
        © {{ year }} CourseHub · 南京信息工程大学 25 级课程实践项目 · MIT License
      </p>
    </div>
  </footer>
</template>

<script setup>
/**
 * 全站公用页脚
 *
 * 单独抽成组件，方便后续在多个页面复用（目前首页/列表页使用）。
 */
const year = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  margin-top: 48px;
  border-top: 1px solid var(--ch-border-light);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
}

.footer-inner {
  padding: 30px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.foot-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.foot-logo {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: var(--ch-gradient);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--ch-shadow-primary);
}

.foot-name {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  text-align: left;
  line-height: 1.2;
}

.foot-slogan {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
  text-align: left;
}

.foot-links {
  display: flex;
  gap: 22px;
  font-size: 13px;
}

.foot-links a {
  color: var(--ch-text-secondary);
}

.foot-links a:hover {
  color: var(--ch-primary);
}

.foot-copy {
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-placeholder);
}
</style>
