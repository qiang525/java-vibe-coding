<template>
  <div class="home">
    <!-- ========== 顶部导航 ========== -->
    <header class="site-header">
      <div class="ch-container header-inner">
        <div class="brand">
          <div class="logo">CH</div>
          <div class="brand-text">
            <h1>CourseHub</h1>
            <p>让每一份资料都有出处</p>
          </div>
        </div>
        <nav class="nav">
          <a href="#/" class="nav-link active">首页</a>
          <a href="#/courses" class="nav-link">课程</a>
          <a href="#/resources" class="nav-link">资料库</a>
          <el-button type="primary" size="default" @click="goLogin">登录</el-button>
        </nav>
      </div>
    </header>

    <!-- ========== 主体 ========== -->
    <main class="ch-container main">
      <!-- 欢迎区 -->
      <section class="hero ch-card">
        <h2>课程资料协作平台</h2>
        <p class="hero-sub">
          按「学院 → 学期 → 课程 → 章节」组织资料，解决找不着、不敢信、重复劳动的问题。
        </p>

        <!-- 后端连通性检测卡片 -->
        <div class="status-box" :class="statusClass">
          <div class="status-head">
            <span class="dot"></span>
            <strong>{{ statusText }}</strong>
          </div>
          <p class="status-desc">{{ statusDesc }}</p>

          <el-descriptions
            v-if="healthData"
            :column="2"
            border
            size="small"
            class="status-detail"
          >
            <el-descriptions-item label="应用名称">
              {{ healthData.application }}
            </el-descriptions-item>
            <el-descriptions-item label="运行环境">
              {{ healthData.profile }}
            </el-descriptions-item>
            <el-descriptions-item label="Java 版本">
              {{ healthData.javaVersion }}
            </el-descriptions-item>
            <el-descriptions-item label="服务器时间">
              {{ healthData.serverTime }}
            </el-descriptions-item>
          </el-descriptions>

          <el-button
            class="retry-btn"
            :loading="loading"
            @click="checkHealth"
            type="primary"
            plain
          >
            重新检测
          </el-button>
        </div>
      </section>

      <!-- 功能模块预览 -->
      <section class="features">
        <div
          v-for="item in features"
          :key="item.title"
          class="feature-card"
        >
          <el-icon class="feature-icon" :size="26">
            <component :is="item.icon" />
          </el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </section>

      <!-- 里程碑进度 -->
      <section class="ch-card milestone">
        <h3>项目里程碑</h3>
        <el-steps :active="currentStage" finish-status="success" align-center>
          <el-step title="M0 项目骨架" description="前后端框架搭建" />
          <el-step title="M1 用户与课程" description="注册登录、课程管理" />
          <el-step title="M2 资料核心" description="上传下载、检索" />
          <el-step title="M3 协作与治理" description="版本、权限、审核" />
          <el-step title="M4 评分与上线" description="可信度、部署" />
        </el-steps>
      </section>
    </main>

    <footer class="site-footer">
      <div class="ch-container">
        CourseHub · 南京信息工程大学 25 级课程实践项目 · MIT License
      </div>
    </footer>
  </div>
</template>

<script setup>
/**
 * 首页
 *
 * 这个页面承担一个关键职责：**验证前后端链路是否打通**。
 * 加载时会自动调用后端 /api/health，成功则显示绿色状态卡与后端信息。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHealth } from '@/api/health'

const router = useRouter()

const loading = ref(false)
const healthData = ref(null)
const errorMsg = ref('')

// 功能模块预览数据
const features = [
  {
    icon: 'FolderOpened',
    title: '课程资料归档',
    desc: '按学院、学期、课程、章节四级组织，资料不再散落群聊'
  },
  {
    icon: 'Star',
    title: '可信度评分',
    desc: '综合上传者信用、社区评价与时效性，优质资料自动置顶'
  },
  {
    icon: 'Search',
    title: '中文全文检索',
    desc: '基于 MySQL ngram 分词，搜「数据结构」能命中「数据结构与算法」'
  },
  {
    icon: 'Files',
    title: '版本树管理',
    desc: '支持分支、合并与回滚，多人维护不再互相覆盖'
  },
  {
    icon: 'UploadFilled',
    title: '大文件分片上传',
    desc: '秒传、断点续传、合并校验，500MB 课件也能稳定上传'
  },
  {
    icon: 'Lock',
    title: '三级可见性',
    desc: '全平台公开 / 仅本课程 / 完全私密，数据权限双重校验'
  }
]

// 里程碑当前进度（M0 已完成）
const currentStage = ref(1)

const statusClass = computed(() => ({
  'status-ok': healthData.value && !errorMsg.value,
  'status-error': !!errorMsg.value,
  'status-pending': !healthData.value && !errorMsg.value
}))

const statusText = computed(() => {
  if (errorMsg.value) return '后端未连接'
  if (healthData.value) return '后端连接正常'
  return '正在检测后端连接…'
})

const statusDesc = computed(() => {
  if (errorMsg.value) {
    return `${errorMsg.value}。请确认后端已启动（默认 8080 端口），然后点击「重新检测」。`
  }
  if (healthData.value) {
    return '前后端链路已打通，可以开始联调开发。'
  }
  return '正在请求 GET /api/health …'
})

/**
 * 调用后端健康接口
 */
async function checkHealth() {
  loading.value = true
  errorMsg.value = ''
  try {
    // request.js 的响应拦截器已经把统一响应体解包，这里直接拿到 data
    healthData.value = await getHealth()
  } catch (e) {
    healthData.value = null
    errorMsg.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}

onMounted(checkHealth)
</script>

<style scoped>
/* ---------- 顶部导航 ---------- */
.site-header {
  background: #fff;
  border-bottom: 1px solid var(--ch-border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  height: var(--ch-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
}

.brand-text h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.2;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.nav {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-link {
  color: var(--ch-text-regular);
  font-size: 14px;
}

.nav-link.active,
.nav-link:hover {
  color: var(--ch-primary);
}

/* ---------- 主体 ---------- */
.main {
  padding: 32px 20px 48px;
}

.hero h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
}

.hero-sub {
  margin: 0 0 24px;
  color: var(--ch-text-secondary);
  line-height: 1.7;
}

/* ---------- 状态卡 ---------- */
.status-box {
  border-radius: var(--ch-radius);
  padding: 18px 20px;
  border: 1px solid var(--ch-border-light);
  background: #fafafa;
  transition: all 0.25s ease;
}

.status-box.status-ok {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.status-box.status-error {
  border-color: #fab6b6;
  background: #fef0f0;
}

.status-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  margin-bottom: 6px;
}

.status-head .dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #c0c4cc;
}

.status-ok .dot {
  background: var(--ch-success);
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.18);
}

.status-error .dot {
  background: var(--ch-danger);
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.18);
}

.status-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--ch-text-secondary);
  line-height: 1.7;
}

.status-detail {
  margin-bottom: 14px;
}

.retry-btn {
  margin-top: 4px;
}

/* ---------- 功能卡片 ---------- */
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin: 24px 0;
}

.feature-card {
  background: var(--ch-bg-card);
  border-radius: var(--ch-radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.09);
}

.feature-icon {
  color: var(--ch-primary);
}

.feature-card h3 {
  margin: 10px 0 6px;
  font-size: 15px;
  font-weight: 600;
}

.feature-card p {
  margin: 0;
  font-size: 13px;
  color: var(--ch-text-secondary);
  line-height: 1.7;
}

/* ---------- 里程碑 ---------- */
.milestone h3 {
  margin: 0 0 20px;
  font-size: 16px;
  font-weight: 600;
}

/* ---------- 页脚 ---------- */
.site-footer {
  border-top: 1px solid var(--ch-border-light);
  background: #fff;
  padding: 18px 0;
  text-align: center;
  font-size: 13px;
  color: var(--ch-text-secondary);
}
</style>
