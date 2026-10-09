<template>
  <div class="resource-page">
    <AppHeader active="resources" />

    <main class="ch-container main">
      <!-- ========== 搜索区 ========== -->
      <section class="search-hero ch-card">
        <h2>资料库</h2>
        <p class="sub">搜索课程资料、讲义、真题与笔记</p>

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
          <el-button type="primary" size="large" @click="handleSearch">
            搜索
          </el-button>
        </div>

        <!-- 搜索热词 -->
        <div v-if="hotKeywords.length" class="hot-words">
          <span class="hot-label">热门搜索：</span>
          <el-tag
            v-for="h in hotKeywords"
            :key="h.keyword"
            class="hot-tag"
            type="info"
            effect="plain"
            @click="searchByKeyword(h.keyword)"
          >
            {{ h.keyword }}
          </el-tag>
        </div>
      </section>

      <!-- ========== 筛选栏 ========== -->
      <div class="filter-bar">
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
      <div v-loading="loading" class="resource-list">
        <template v-if="list.length">
          <div
            v-for="r in list"
            :key="r.id"
            class="resource-card ch-card"
            @click="goDetail(r.id)"
          >
            <div class="rc-icon" :class="'type-' + (r.type || 'OTHER')">
              {{ typeShort(r.type) }}
            </div>

            <div class="rc-body">
              <h3 class="ch-ellipsis">{{ r.title }}</h3>

              <p class="rc-meta">
                <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
                <span class="meta-item">{{ r.courseName || '未分类课程' }}</span>
                <span class="meta-item" v-if="r.uploaderName">
                  上传者：{{ r.uploaderName }}
                </span>
                <el-tag
                  v-if="r.visibility && r.visibility !== 'PUBLIC'"
                  size="small"
                  :type="r.visibility === 'PRIVATE' ? 'danger' : 'warning'"
                  effect="plain"
                >
                  {{ r.visibility === 'PRIVATE' ? '仅自己' : '仅本课程' }}
                </el-tag>
              </p>

              <p class="rc-desc">{{ r.description || '暂无描述' }}</p>

              <div class="rc-foot">
                <span class="stat"><el-icon><Document /></el-icon>{{ r.fileName || '无文件' }}</span>
                <span class="stat" v-if="r.readableSize">{{ r.readableSize }}</span>
                <span class="stat"><el-icon><View /></el-icon>{{ r.viewCount || 0 }}</span>
                <span class="stat"><el-icon><Download /></el-icon>{{ r.downloadCount || 0 }}</span>
                <span class="stat score" v-if="r.relevance !== null && r.relevance !== undefined">
                  相关度 {{ (r.relevance || 0).toFixed(3) }}
                </span>
              </div>
            </div>
          </div>
        </template>

        <el-empty
          v-else-if="!loading"
          :description="query.keyword ? `没有找到与「${query.keyword}」相关的资料` : '暂无资料'"
        />
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
import { Search, Document, View, Download } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
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
  padding: 26px 20px 48px;
}

/* ---------- 搜索区 ---------- */
.search-hero {
  margin-bottom: 18px;
}

.search-hero h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
}

.search-hero .sub {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.search-bar {
  display: flex;
  gap: 10px;
  max-width: 680px;
}

.hot-words {
  margin-top: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-label {
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.hot-tag {
  cursor: pointer;
}

.hot-tag:hover {
  color: var(--ch-primary);
  border-color: var(--ch-primary);
}

/* ---------- 筛选栏 ---------- */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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

/* ---------- 列表 ---------- */
.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.resource-card {
  display: flex;
  gap: 16px;
  padding: 18px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.resource-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.08);
}

.rc-icon {
  width: 50px;
  height: 50px;
  flex-shrink: 0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #7aa9ff, #4a7dff);
}

.type-EXAM { background: linear-gradient(135deg, #ff9a6b, #f56c6c); }
.type-NOTES { background: linear-gradient(135deg, #7ad0a3, #3aa76d); }
.type-TEXTBOOK { background: linear-gradient(135deg, #b39ddb, #7e57c2); }
.type-VIDEO { background: linear-gradient(135deg, #ffb74d, #f57c00); }

.rc-body {
  flex: 1;
  min-width: 0;
}

.rc-body h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.rc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.rc-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--ch-text-regular);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rc-foot {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat.score {
  color: var(--ch-primary);
}

/* ---------- 分页 ---------- */
.pager {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}
</style>
