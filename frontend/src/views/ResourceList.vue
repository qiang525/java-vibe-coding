<template>
  <div class="resource-page">
    <AppHeader active="resources" />

    <main class="ch-container main">
      <!-- ========== 搜索区 ========== -->
      <section class="search-hero ch-fade-up">
        <span class="blob blob-1"></span>
        <span class="blob blob-2"></span>

        <div class="sh-inner">
          <span class="ch-pill sh-pill">
            <el-icon><Search /></el-icon> 全文检索
          </span>
          <h2>在 <span class="ch-gradient-text-light">资料库</span> 里找到你想要的</h2>
          <p class="sh-sub">讲义、真题与笔记，基于中文全文索引，秒级定位</p>

          <div class="search-bar">
            <el-input
              v-model="query.keyword"
              placeholder="输入关键词搜索，例如：数据结构、线性表、期末真题"
              size="large"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" size="large" round @click="handleSearch">
              搜索
            </el-button>
          </div>

          <!-- 搜索热词 -->
          <div v-if="hotKeywords.length" class="hot-words">
            <span class="hot-label">
              <el-icon><TrendCharts /></el-icon> 热门搜索
            </span>
            <span
              v-for="h in hotKeywords"
              :key="h.keyword"
              class="hot-tag"
              @click="searchByKeyword(h.keyword)"
            >
              {{ h.keyword }}
            </span>
          </div>
        </div>
      </section>

      <!-- ========== 筛选栏 ========== -->
      <div class="filter-bar ch-card ch-fade-up ch-delay-1">
        <el-select
          v-model="query.type"
          placeholder="全部类型"
          clearable
          class="filter-item"
          @change="handleSearch"
        >
          <el-option label="讲义" value="LECTURE" />
          <el-option label="教材" value="TEXTBOOK" />
          <el-option label="真题" value="EXAM" />
          <el-option label="笔记" value="NOTES" />
          <el-option label="作业" value="ASSIGNMENT" />
          <el-option label="视频" value="VIDEO" />
          <el-option label="其他" value="OTHER" />
        </el-select>

        <el-select
          v-model="query.courseId"
          placeholder="全部课程"
          clearable
          filterable
          class="filter-item wide"
          @change="handleSearch"
        >
          <el-option
            v-for="c in courses"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>

        <el-button @click="handleReset">重置</el-button>

        <span class="result-count" v-if="!loading">
          共 {{ total }} 条结果
          <template v-if="query.keyword">
            （关键词：<strong>{{ query.keyword }}</strong>）
          </template>
        </span>
      </div>

      <!-- ========== 资料列表 ========== -->
      <div class="resource-list">
        <!-- 骨架屏 -->
        <template v-if="loading && !list.length">
          <div v-for="n in 4" :key="'sk' + n" class="resource-card ch-card">
            <div class="ch-skeleton sk-icon"></div>
            <div class="sk-body">
              <div class="ch-skeleton sk-line" style="width: 40%"></div>
              <div class="ch-skeleton sk-line" style="width: 95%"></div>
              <div class="ch-skeleton sk-line" style="width: 65%"></div>
            </div>
          </div>
        </template>

        <template v-else-if="list.length">
          <div
            v-for="(r, i) in list"
            :key="r.id"
            class="resource-card ch-card ch-card--interactive ch-fade-up"
            :class="`ch-delay-${(i % 4) + 1}`"
            @click="goDetail(r.id)"
          >
            <div class="rc-icon" :class="'type-' + (r.type || 'OTHER')">
              {{ typeShort(r.type) }}
            </div>

            <div class="rc-body">
              <h3 class="ch-ellipsis">{{ r.title }}</h3>

              <p class="rc-meta">
                <span class="type-chip">{{ typeLabel(r.type) }}</span>
                <span class="meta-item">{{ r.courseName || '未分类课程' }}</span>
                <span class="meta-item" v-if="r.uploaderName">
                  {{ r.uploaderName }}
                </span>
                <span
                  v-if="r.visibility && r.visibility !== 'PUBLIC'"
                  class="vis-chip"
                  :class="r.visibility === 'PRIVATE' ? 'vis-private' : 'vis-course'"
                >
                  {{ r.visibility === 'PRIVATE' ? '仅自己' : '仅本课程' }}
                </span>
              </p>

              <p class="rc-desc ch-clamp-2">{{ r.description || '暂无描述' }}</p>

              <div class="rc-foot">
                <span class="stat"><el-icon><Document /></el-icon>{{ r.fileName || '无文件' }}</span>
                <span class="stat" v-if="r.readableSize">{{ r.readableSize }}</span>
                <span class="stat"><el-icon><View /></el-icon>{{ r.viewCount || 0 }}</span>
                <span class="stat"><el-icon><Download /></el-icon>{{ r.downloadCount || 0 }}</span>
                <span
                  class="stat score"
                  v-if="r.relevance !== null && r.relevance !== undefined"
                >
                  <el-icon><Aim /></el-icon>相关度 {{ (r.relevance || 0).toFixed(3) }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="empty-wrap">
          <el-empty
            :description="query.keyword ? `没有找到与「${query.keyword}」相关的资料` : '暂无资料'"
          >
            <el-button v-if="query.keyword || query.type || query.courseId" @click="handleReset">
              清空筛选条件
            </el-button>
          </el-empty>
        </div>
      </div>

      <!-- ========== 分页 ========== -->
      <div v-if="total > 0" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :current-page="query.page"
          :page-size="query.pageSize"
          @current-change="handlePageChange"
        />
      </div>
    </main>

    <AppFooter />
  </div>
</template>

<script setup>
/**
 * 资料库页面
 *
 * 两种数据来源：
 *   · 没输关键词 → GET /api/resources        （普通列表，按时间倒序）
 *   · 输了关键词 → GET /api/resources/search （全文检索，按相关度排序）
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, Document, View, Download, TrendCharts, Aim
} from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import {
  getResources,
  searchResources,
  getHotKeywords
} from '@/api/resource'
import { getCourses } from '@/api/course'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const courses = ref([])
const hotKeywords = ref([])

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  type: '',
  courseId: null
})

// ==================== 数据加载 ====================
async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      courseId: query.courseId || undefined
    }

    // 有关键词走全文检索，否则走普通列表
    const data = query.keyword
      ? await searchResources(params)
      : await getResources(params)

    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchHotKeywords() {
  try {
    hotKeywords.value = await getHotKeywords(8)
  } catch (e) {
    hotKeywords.value = []
  }
}

// ==================== 交互 ====================
function handleSearch() {
  query.page = 1
  fetchList()
}

function handleReset() {
  query.keyword = ''
  query.type = ''
  query.courseId = null
  query.page = 1
  fetchList()
}

function handlePageChange(page) {
  query.page = page
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function searchByKeyword(kw) {
  query.keyword = kw
  query.page = 1
  fetchList()
}

function goDetail(id) {
  router.push(`/resources/${id}`)
}

// ==================== 显示辅助 ====================
const TYPE_MAP = {
  TEXTBOOK: { short: '教', label: '教材' },
  LECTURE: { short: '讲', label: '讲义' },
  EXAM: { short: '题', label: '真题' },
  NOTES: { short: '记', label: '笔记' },
  ASSIGNMENT: { short: '业', label: '作业' },
  VIDEO: { short: '视', label: '视频' },
  OTHER: { short: '他', label: '其他' }
}

function typeShort(t) {
  return (TYPE_MAP[t] || TYPE_MAP.OTHER).short
}

function typeLabel(t) {
  return (TYPE_MAP[t] || TYPE_MAP.OTHER).label
}

// ==================== 初始化 ====================
onMounted(async () => {
  fetchList()
  fetchHotKeywords()
  try {
    const data = await getCourses({ page: 1, pageSize: 100 })
    courses.value = data.list || []
  } catch (e) {
    courses.value = []
  }
})
</script>

<style scoped>
.main {
  padding: 28px 24px 48px;
}

/* =============================================================================
   搜索区（渐变横幅）
   ============================================================================= */
.search-hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background: var(--ch-gradient-hero);
  padding: 40px 44px;
  margin-bottom: 20px;
  box-shadow: 0 24px 56px rgba(49, 27, 146, 0.28);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  pointer-events: none;
}

.blob-1 {
  width: 300px;
  height: 300px;
  top: -120px;
  right: -40px;
  background: rgba(168, 85, 247, 0.55);
  animation: ch-float 9s ease-in-out infinite;
}

.blob-2 {
  width: 240px;
  height: 240px;
  bottom: -130px;
  left: 8%;
  background: rgba(56, 189, 248, 0.42);
  animation: ch-float 11s ease-in-out infinite reverse;
}

.sh-inner {
  position: relative;
  z-index: 1;
}

.sh-pill {
  color: #e9d5ff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.search-hero h2 {
  margin: 16px 0 8px;
  font-size: 30px;
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

.sh-sub {
  margin: 0 0 22px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.75);
}

.search-bar {
  display: flex;
  gap: 12px;
  max-width: 720px;
}

.search-bar :deep(.el-input__wrapper) {
  height: 50px;
  border-radius: 13px;
  box-shadow: none;
}

.search-bar :deep(.el-input__inner) {
  font-size: 14.5px;
}

.search-bar .el-button {
  height: 50px;
  padding: 0 30px;
  font-size: 15px;
  font-weight: 600;
}

/* 热词 */
.hot-words {
  margin-top: 18px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
}

.hot-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.65);
}

.hot-tag {
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 12.5px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  transition: var(--ch-transition);
}

.hot-tag:hover {
  background: rgba(255, 255, 255, 0.22);
  transform: translateY(-1px);
}

/* =============================================================================
   筛选栏
   ============================================================================= */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.filter-item {
  width: 140px;
}

.filter-item.wide {
  width: 220px;
}

.result-count {
  margin-left: auto;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.result-count strong {
  color: var(--ch-primary);
}

/* =============================================================================
   列表
   ============================================================================= */
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.empty-wrap {
  padding: 40px 0;
}

.resource-card {
  display: flex;
  gap: 18px;
  padding: 20px;
}

.rc-icon {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
  transition: var(--ch-transition);
}

.resource-card:hover .rc-icon {
  transform: scale(1.06) rotate(-4deg);
}

.type-EXAM { background: linear-gradient(135deg, #f97316, #ef4444); }
.type-NOTES { background: linear-gradient(135deg, #10b981, #059669); }
.type-TEXTBOOK { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
.type-VIDEO { background: linear-gradient(135deg, #f59e0b, #f97316); }
.type-ASSIGNMENT { background: linear-gradient(135deg, #06b6d4, #0ea5e9); }

.rc-body {
  flex: 1;
  min-width: 0;
}

.rc-body h3 {
  margin: 4px 0 10px;
  font-size: 16.5px;
  font-weight: 700;
}

.rc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 0 10px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.type-chip {
  padding: 2px 10px;
  border-radius: 999px;
  font-weight: 500;
  color: var(--ch-primary-dark);
  background: rgba(79, 70, 229, 0.08);
}

.vis-chip {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
}

.vis-course {
  color: #b45309;
  background: rgba(245, 158, 11, 0.13);
}

.vis-private {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.rc-desc {
  margin: 0 0 12px;
  font-size: 13.5px;
  color: var(--ch-text-regular);
  line-height: 1.7;
}

.rc-foot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.stat {
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat.score {
  color: var(--ch-primary);
  font-weight: 500;
}

/* ---------- 骨架屏 ---------- */
.sk-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  flex-shrink: 0;
}

.sk-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 6px;
}

.sk-line {
  height: 12px;
}

/* ---------- 分页 ---------- */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 600px) {
  .main {
    padding: 20px 16px 40px;
  }

  .search-hero {
    padding: 28px 22px;
    border-radius: 20px;
  }

  .search-hero h2 {
    font-size: 23px;
  }

  .search-bar {
    flex-direction: column;
  }

  .filter-item,
  .filter-item.wide {
    width: 100%;
  }

  .result-count {
    margin-left: 0;
  }
}
</style>
