<template>
  <div class="home">
    <!-- ========== 顶部导航（公用组件） ========== -->
    <AppHeader active="home" />

    <main>
      <!-- ============================================================
           一、Hero 区：深色渐变 + 光晕
           ============================================================ -->
      <section class="hero-wrap">
        <div class="ch-container">
          <div class="hero">
            <!-- 装饰光晕 -->
            <span class="blob blob-1"></span>
            <span class="blob blob-2"></span>
            <span class="grid-overlay"></span>

            <div class="hero-grid">
              <!-- 左：文案 + 行动按钮 -->
              <div class="hero-copy">
                <span class="ch-pill hero-pill ch-fade-up">
                  <el-icon><Reading /></el-icon>
                  南京信息工程大学 · 课程实践项目
                </span>

                <h1 class="ch-fade-up ch-delay-1">
                  让课程资料
                  <span class="ch-gradient-text-light">有序、可信、可追溯</span>
                </h1>

                <p class="hero-sub ch-fade-up ch-delay-2">
                  按「学院 → 学期 → 课程 → 章节」组织资料，用可信度评分帮你挑出好内容，
                  用版本树让多人协作不再互相覆盖。
                </p>

                <div class="hero-actions ch-fade-up ch-delay-3">
                  <router-link to="/courses" class="hero-btn hero-btn--light">
                    <el-icon><Compass /></el-icon> 浏览课程
                  </router-link>
                  <router-link to="/resources" class="hero-btn hero-btn--ghost">
                    <el-icon><Files /></el-icon> 进入资料库
                  </router-link>
                </div>

                <div class="hero-points ch-fade-up ch-delay-4">
                  <span><el-icon><Check /></el-icon> 中文全文检索</span>
                  <span><el-icon><Check /></el-icon> 分片秒传</span>
                  <span><el-icon><Check /></el-icon> 三级可见性</span>
                </div>
              </div>

              <!-- 右：后端连通性卡片 -->
              <div class="hero-status ch-fade-up ch-delay-3" :class="statusClass">
                <div class="hs-head">
                  <span class="hs-dot"></span>
                  <strong>{{ statusText }}</strong>
                  <el-tag
                    v-if="healthData"
                    size="small"
                    effect="dark"
                    class="hs-badge"
                  >
                    {{ healthData.profile }}
                  </el-tag>
                </div>
                <p class="hs-desc">{{ statusDesc }}</p>

                <ul v-if="healthData" class="hs-list">
                  <li>
                    <span>应用</span>
                    <b>{{ healthData.application }}</b>
                  </li>
                  <li>
                    <span>Java</span>
                    <b>{{ healthData.javaVersion }}</b>
                  </li>
                  <li>
                    <span>服务器时间</span>
                    <b>{{ healthData.serverTime }}</b>
                  </li>
                </ul>

                <button class="hs-retry" :disabled="loading" @click="checkHealth">
                  <el-icon :class="{ spinning: loading }"><Refresh /></el-icon>
                  {{ loading ? '检测中…' : '重新检测' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============================================================
           二、数据统计
           ============================================================ -->
      <section class="ch-container">
        <div class="stats">
          <div
            v-for="(s, i) in stats"
            :key="s.label"
            class="stat-card ch-card ch-fade-up"
            :class="`ch-delay-${i + 1}`"
          >
            <div class="stat-icon" :style="{ background: s.bg }">
              <el-icon :size="20"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-body">
              <p class="stat-value">{{ s.value }}</p>
              <p class="stat-label">{{ s.label }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- ============================================================
           三、核心能力
           ============================================================ -->
      <section class="ch-container section">
        <div class="section-head">
          <span class="ch-pill"><el-icon><MagicStick /></el-icon> 核心能力</span>
          <h2>一个平台，解决资料协作的全部痛点</h2>
          <p>找不着、不敢信、重复劳动、互相覆盖 —— 每一个都有对应的设计。</p>
        </div>

        <div class="features">
          <article
            v-for="(item, i) in features"
            :key="item.title"
            class="feature-card ch-card ch-card--interactive ch-fade-up"
            :class="`ch-delay-${(i % 3) + 1}`"
          >
            <div class="feature-icon" :style="{ background: item.bg }">
              <el-icon :size="22"><component :is="item.icon" /></el-icon>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </article>
        </div>
      </section>

      <!-- ============================================================
           四、里程碑
           ============================================================ -->
      <section class="ch-container section">
        <div class="milestone ch-card">
          <div class="milestone-head">
            <div>
              <span class="ch-pill"><el-icon><Flag /></el-icon> 项目进度</span>
              <h2>里程碑全部完成</h2>
            </div>
            <p class="milestone-note">
              从工程骨架到部署方案，五个阶段均已交付并通过实测。
            </p>
          </div>

          <el-steps :active="5" finish-status="success" align-center class="steps">
            <el-step title="M0 项目骨架" description="前后端框架 + 数据库" />
            <el-step title="M1 用户与课程" description="认证、课程管理" />
            <el-step title="M2 资料核心" description="上传、检索、权限" />
            <el-step title="M3 协作与治理" description="版本、评论、审核" />
            <el-step title="M4 评分与上线" description="可信度、部署" />
          </el-steps>
        </div>
      </section>

      <!-- ============================================================
           五、行动号召
           ============================================================ -->
      <section class="ch-container section">
        <div class="cta">
          <span class="blob blob-3"></span>
          <div>
            <h2>准备好开始了吗？</h2>
            <p>用测试账号 <code>admin / admin123</code> 登录，体验完整流程。</p>
          </div>
          <div class="cta-actions">
            <router-link to="/login" class="hero-btn hero-btn--light">
              <el-icon><User /></el-icon> 立即登录
            </router-link>
            <a href="/doc.html" target="_blank" rel="noopener" class="hero-btn hero-btn--ghost">
              <el-icon><Document /></el-icon> 查看接口文档
            </a>
          </div>
        </div>
      </section>
    </main>

    <AppFooter />
  </div>
</template>

<script setup>
/**
 * 首页
 *
 * 职责有两层：
 *   1. 作为「产品落地页」向访问者介绍平台能力
 *   2. 作为「链路探针」—— 加载时自动请求后端 /api/health，
 *      成功即说明前后端链路已打通（这也是开发阶段最常用的自检手段）
 */
import { ref, computed, onMounted } from 'vue'
import { getHealth } from '@/api/health'
import { getCourses } from '@/api/course'
import { getResources } from '@/api/resource'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'

const loading = ref(false)
const healthData = ref(null)
const errorMsg = ref('')

/** 实时统计（课程数 / 资料数来自真实接口） */
const courseTotal = ref(null)
const resourceTotal = ref(null)

const stats = computed(() => [
  {
    label: '门课程',
    value: courseTotal.value === null ? '—' : courseTotal.value,
    icon: 'Notebook',
    bg: 'linear-gradient(135deg, #4f46e5, #7c3aed)'
  },
  {
    label: '份资料',
    value: resourceTotal.value === null ? '—' : resourceTotal.value,
    icon: 'Files',
    bg: 'linear-gradient(135deg, #06b6d4, #0ea5e9)'
  },
  {
    label: '支持文件格式',
    value: '10+',
    icon: 'Document',
    bg: 'linear-gradient(135deg, #f59e0b, #f97316)'
  },
  {
    label: '单文件上限',
    value: '500MB',
    icon: 'UploadFilled',
    bg: 'linear-gradient(135deg, #10b981, #059669)'
  }
])

const features = [
  {
    icon: 'FolderOpened',
    title: '课程资料归档',
    desc: '按学院、学期、课程、章节四级组织，资料不再散落群聊。',
    bg: 'linear-gradient(135deg, #4f46e5, #7c3aed)'
  },
  {
    icon: 'Star',
    title: '可信度评分',
    desc: '综合上传者信用、社区评价与时效性五因子加权，优质资料自动置顶。',
    bg: 'linear-gradient(135deg, #f59e0b, #f97316)'
  },
  {
    icon: 'Search',
    title: '中文全文检索',
    desc: '基于 MySQL ngram 分词，搜「数据结构」能命中「数据结构与算法」。',
    bg: 'linear-gradient(135deg, #06b6d4, #0ea5e9)'
  },
  {
    icon: 'Share',
    title: '版本树管理',
    desc: '支持分支、合并与回滚，每一次修改都留痕，多人维护不再覆盖。',
    bg: 'linear-gradient(135deg, #ec4899, #db2777)'
  },
  {
    icon: 'UploadFilled',
    title: '大文件分片上传',
    desc: '秒传、断点续传、合并哈希校验，500MB 课件也能稳定上传。',
    bg: 'linear-gradient(135deg, #10b981, #059669)'
  },
  {
    icon: 'Lock',
    title: '三级可见性',
    desc: '全平台公开 / 仅本课程 / 完全私密，功能权限与数据权限双重校验。',
    bg: 'linear-gradient(135deg, #8b5cf6, #6d28d9)'
  }
]

const statusClass = computed(() => ({
  'hs-ok': healthData.value && !errorMsg.value,
  'hs-error': !!errorMsg.value,
  'hs-pending': !healthData.value && !errorMsg.value
}))

const statusText = computed(() => {
  if (errorMsg.value) return '后端未连接'
  if (healthData.value) return '服务运行正常'
  return '正在检测…'
})

const statusDesc = computed(() => {
  if (errorMsg.value) {
    return `${errorMsg.value}。请确认后端已启动（默认 8080 端口）后重试。`
  }
  if (healthData.value) {
    return '前后端链路已打通，可以开始联调与体验。'
  }
  return '正在请求 GET /api/health …'
})

/** 调用后端健康接口 */
async function checkHealth() {
  loading.value = true
  errorMsg.value = ''
  try {
    healthData.value = await getHealth()
  } catch (e) {
    healthData.value = null
    errorMsg.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

/** 拉取真实统计数字（失败保持 — 占位，不影响页面） */
async function fetchStats() {
  try {
    const [courses, resources] = await Promise.all([
      getCourses({ page: 1, pageSize: 1 }),
      getResources({ page: 1, pageSize: 1 })
    ])
    courseTotal.value = courses?.total ?? 0
    resourceTotal.value = resources?.total ?? 0
  } catch (e) {
    // 统计失败无关紧要，静默处理
  }
}

onMounted(() => {
  checkHealth()
  fetchStats()
})
</script>

<style scoped>
/* =============================================================================
   Hero
   ============================================================================= */
.hero-wrap {
  padding-top: 28px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  background: var(--ch-gradient-hero);
  padding: 56px 52px;
  box-shadow: 0 30px 70px rgba(49, 27, 146, 0.32);
}

/* 背景光晕 */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}

.blob-1 {
  width: 340px;
  height: 340px;
  top: -120px;
  right: -60px;
  background: rgba(168, 85, 247, 0.55);
  animation: ch-float 9s ease-in-out infinite;
}

.blob-2 {
  width: 280px;
  height: 280px;
  bottom: -140px;
  left: -60px;
  background: rgba(56, 189, 248, 0.45);
  animation: ch-float 11s ease-in-out infinite reverse;
}

/* 细网格纹理，增加科技质感 */
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.055) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(circle at 30% 20%, #000 0%, transparent 72%);
  -webkit-mask-image: radial-gradient(circle at 30% 20%, #000 0%, transparent 72%);
  pointer-events: none;
}

.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 44px;
  align-items: center;
}

.hero-pill {
  color: #e9d5ff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.hero-copy h1 {
  margin: 18px 0 16px;
  font-size: 44px;
  line-height: 1.18;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
}

.ch-gradient-text-light {
  background: linear-gradient(120deg, #a5b4fc 0%, #c4b5fd 45%, #f0abfc 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-sub {
  margin: 0 0 26px;
  max-width: 520px;
  font-size: 15px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.76);
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

/* 自定义按钮：在深色背景上用白色/幽灵样式，避免与全局渐变按钮混淆 */
.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 24px;
  border-radius: 13px;
  font-size: 14.5px;
  font-weight: 600;
  transition: var(--ch-transition);
  border: 1px solid transparent;
}

.hero-btn--light {
  background: #fff;
  color: #4c1d95;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.22);
}

.hero-btn--light:hover {
  color: #4c1d95;
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.28);
}

.hero-btn--ghost {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.28);
}

.hero-btn--ghost:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.45);
  transform: translateY(-2px);
}

.hero-points {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 26px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.hero-points span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hero-points .el-icon {
  color: #6ee7b7;
}

/* ---------- Hero 右侧状态卡 ---------- */
.hero-status {
  border-radius: 20px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.09);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 20px 44px rgba(0, 0, 0, 0.22);
  transition: var(--ch-transition);
}

.hs-head {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #fff;
  font-size: 15px;
}

.hs-badge {
  margin-left: auto;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.hs-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.hs-ok .hs-dot {
  background: #34d399;
  animation: ch-pulse-ring 2s infinite;
}

.hs-error .hs-dot {
  background: #f87171;
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.28);
}

.hs-desc {
  margin: 10px 0 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.7);
}

.hs-list {
  list-style: none;
  margin: 0 0 16px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hs-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.14);
  font-size: 12.5px;
}

.hs-list li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hs-list span {
  color: rgba(255, 255, 255, 0.6);
}

.hs-list b {
  color: #fff;
  font-weight: 600;
}

.hs-retry {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  justify-content: center;
  height: 40px;
  border-radius: 11px;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 600;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.24);
  transition: var(--ch-transition);
}

.hs-retry:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.22);
}

.hs-retry:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* =============================================================================
   统计卡
   ============================================================================= */
.stats {
  position: relative;
  z-index: 2;
  margin-top: -36px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  box-shadow: var(--ch-shadow-md);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.18);
}

.stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.stat-label {
  margin: 3px 0 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

/* =============================================================================
   通用小节标题
   ============================================================================= */
.section {
  margin-top: 56px;
}

.section-head {
  text-align: center;
  margin-bottom: 26px;
}

.section-head h2 {
  margin: 16px 0 8px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.section-head p {
  margin: 0;
  font-size: 14px;
  color: var(--ch-text-secondary);
}

/* =============================================================================
   能力卡
   ============================================================================= */
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 18px;
}

.feature-card {
  padding: 24px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
  transition: var(--ch-transition);
}

.feature-card:hover .feature-icon {
  transform: scale(1.08) rotate(-4deg);
}

.feature-card h3 {
  margin: 16px 0 8px;
  font-size: 16.5px;
  font-weight: 700;
}

.feature-card p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ch-text-secondary);
}

/* =============================================================================
   里程碑
   ============================================================================= */
.milestone {
  padding: 30px 32px 34px;
}

.milestone-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.milestone-head h2 {
  margin: 14px 0 0;
  font-size: 22px;
  font-weight: 800;
}

.milestone-note {
  margin: 0;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.steps {
  margin-top: 6px;
}

/* =============================================================================
   CTA
   ============================================================================= */
.cta {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  padding: 40px 44px;
  background: var(--ch-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  box-shadow: 0 24px 56px rgba(79, 70, 229, 0.3);
}

.blob-3 {
  width: 320px;
  height: 320px;
  top: -160px;
  right: 12%;
  background: rgba(255, 255, 255, 0.2);
}

.cta h2 {
  position: relative;
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 800;
  color: #fff;
}

.cta p {
  position: relative;
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.cta code {
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.22);
  font-size: 13px;
}

.cta-actions {
  position: relative;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.cta .hero-btn--ghost:hover {
  background: rgba(255, 255, 255, 0.22);
}

/* =============================================================================
   响应式
   ============================================================================= */
@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .hero {
    padding: 44px 32px;
  }

  .hero-copy h1 {
    font-size: 36px;
  }

  .stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .hero {
    padding: 34px 22px;
    border-radius: 22px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .stats {
    grid-template-columns: 1fr;
    margin-top: -24px;
  }

  .section-head h2 {
    font-size: 23px;
  }

  .cta {
    padding: 30px 24px;
  }

  .milestone {
    padding: 24px 20px 28px;
  }
}
</style>
