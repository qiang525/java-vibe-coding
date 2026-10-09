<template>
  <div class="course-detail-page">
    <AppHeader active="courses" />

    <main class="ch-container main">
      <!-- 骨架屏 -->
      <template v-if="loading && !course">
        <div class="ch-card">
          <div class="ch-skeleton sk-line" style="width: 40%; height: 24px"></div>
          <div class="ch-skeleton sk-line" style="width: 65%; margin-top: 16px"></div>
        </div>
      </template>

      <template v-else-if="course">
        <!-- 面包屑 -->
        <el-breadcrumb separator="/" class="crumb">
          <el-breadcrumb-item :to="{ path: '/courses' }">课程库</el-breadcrumb-item>
          <el-breadcrumb-item>{{ course.name }}</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 课程头部（渐变横幅） -->
        <section class="course-head">
          <span class="blob blob-1"></span>
          <span class="blob blob-2"></span>

          <div class="ch-inner">
            <div class="cover">{{ (course.name || '课').charAt(0) }}</div>

            <div class="info">
              <h1>{{ course.name }}</h1>
              <p class="meta">
                <span v-if="course.collegeName" class="meta-chip">{{ course.collegeName }}</span>
                <span v-if="course.teacher">{{ course.teacher }}</span>
                <span v-if="course.term">{{ course.term }}</span>
                <span v-if="course.credit">{{ course.credit }} 学分</span>
              </p>
              <div class="stat-pills">
                <span class="pill"><el-icon><User /></el-icon>{{ course.memberCount || 0 }} 名成员</span>
                <span class="pill"><el-icon><Files /></el-icon>{{ course.resourceCount || 0 }} 份资料</span>
              </div>
            </div>

            <div class="head-action">
              <el-button
                v-if="!joined"
                type="primary"
                round
                size="large"
                :loading="joining"
                @click="handleJoin"
              >
                加入课程
              </el-button>
              <span v-else class="joined-tag">
                <el-icon><CircleCheckFilled /></el-icon> 已加入该课程
              </span>
            </div>
          </div>
        </section>

        <!-- 课程简介 -->
        <section v-if="course.description" class="desc-card ch-card ch-fade-up">
          <h3><el-icon><Document /></el-icon> 课程简介</h3>
          <p>{{ course.description }}</p>
        </section>

        <!-- 资料区 -->
        <section class="resource-section">
          <div class="section-head">
            <h3>课程资料 <span class="count">{{ total }}</span></h3>
            <div class="head-actions">
              <el-input
                v-model="keyword"
                placeholder="在本课程内搜索"
                clearable
                style="width: 220px"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" :icon="UploadFilled" round @click="handleUpload">
                上传资料
              </el-button>
            </div>
          </div>

          <div class="resource-list">
            <!-- 骨架屏 -->
            <template v-if="resLoading && !resources.length">
              <div v-for="n in 3" :key="'sk' + n" class="res-item ch-card">
                <div class="sk-body">
                  <div class="ch-skeleton sk-line" style="width: 45%"></div>
                  <div class="ch-skeleton sk-line" style="width: 30%"></div>
                </div>
              </div>
            </template>

            <template v-else-if="resources.length">
              <div
                v-for="(r, i) in resources"
                :key="r.id"
                class="res-item ch-card ch-card--interactive ch-fade-up"
                :class="`ch-delay-${(i % 4) + 1}`"
                @click="$router.push(`/resources/${r.id}`)"
              >
                <div class="res-icon" :class="'type-' + (r.type || 'OTHER')">
                  {{ typeShort(r.type) }}
                </div>
                <div class="res-body">
                  <h4 class="ch-ellipsis">{{ r.title }}</h4>
                  <p class="res-meta">
                    <span class="type-chip">{{ typeLabel(r.type) }}</span>
                    <span v-if="r.uploaderName">{{ r.uploaderName }}</span>
                    <span v-if="r.readableSize">{{ r.readableSize }}</span>
                    <span>下载 {{ r.downloadCount || 0 }}</span>
                  </p>
                </div>
                <el-icon class="arrow"><ArrowRight /></el-icon>
              </div>
            </template>

            <el-empty
              v-else-if="!resLoading"
              description="本课程还没有资料"
            >
              <el-button type="primary" @click="handleUpload">上传第一份资料</el-button>
            </el-empty>
          </div>

          <div v-if="total > 0" class="pager">
            <el-pagination
              background
              layout="prev, pager, next, total"
              :total="total"
              :current-page="page"
              :page-size="pageSize"
              @current-change="handlePageChange"
            />
          </div>
        </section>
      </template>

      <el-empty v-else-if="!loading" description="课程不存在">
        <el-button type="primary" @click="$router.push('/courses')">返回课程库</el-button>
      </el-empty>
    </main>

    <AppFooter />

    <!-- 上传对话框 -->
    <UploadDialog
      v-model="uploadVisible"
      :course-id="courseId"
      @success="onUploadSuccess"
    />
  </div>
</template>

<script setup>
/**
 * 课程详情页
 *
 * 做两件事：
 *   1. 展示课程信息，支持加入课程
 *   2. 列出该课程下的资料，并提供"上传资料"入口
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, UploadFilled, ArrowRight, User, Files, Document, CircleCheckFilled
} from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import UploadDialog from '@/components/UploadDialog.vue'
import { getCourseDetail, joinCourse } from '@/api/course'
import { getMyCourses } from '@/api/course'
import { getResources, searchResources } from '@/api/resource'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const courseId = route.params.id
const loading = ref(false)
const resLoading = ref(false)
const joining = ref(false)
const course = ref(null)
const joined = ref(false)

const resources = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const uploadVisible = ref(false)

const TYPE_MAP = {
  TEXTBOOK: { short: '教', label: '教材' },
  LECTURE: { short: '讲', label: '讲义' },
  EXAM: { short: '题', label: '真题' },
  NOTES: { short: '记', label: '笔记' },
  ASSIGNMENT: { short: '业', label: '作业' },
  VIDEO: { short: '视', label: '视频' },
  OTHER: { short: '他', label: '其他' }
}
function typeShort(t) { return (TYPE_MAP[t] || TYPE_MAP.OTHER).short }
function typeLabel(t) { return (TYPE_MAP[t] || TYPE_MAP.OTHER).label }

// ==================== 加载课程 ====================
async function fetchCourse() {
  loading.value = true
  try {
    course.value = await getCourseDetail(courseId)
  } catch (e) {
    course.value = null
  } finally {
    loading.value = false
  }
}

/** 判断当前用户是否已加入本课程 */
async function checkJoined() {
  if (!userStore.isLoggedIn) {
    joined.value = false
    return
  }
  try {
    const data = await getMyCourses({ page: 1, pageSize: 100 })
    const list = data.list || []
    joined.value = list.some((c) => String(c.id) === String(courseId))
  } catch (e) {
    joined.value = false
  }
}

async function handleJoin() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入课程')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  joining.value = true
  try {
    await joinCourse(courseId)
    ElMessage.success('已加入课程')
    joined.value = true
    fetchCourse()
  } catch (e) {
    // 拦截器已提示
  } finally {
    joining.value = false
  }
}

// ==================== 加载资料 ====================
async function fetchResources() {
  resLoading.value = true
  try {
    const params = {
      courseId: Number(courseId),
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    }
    const data = keyword.value
      ? await searchResources(params)
      : await getResources(params)
    resources.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    resources.value = []
    total.value = 0
  } finally {
    resLoading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchResources()
}

function handlePageChange(p) {
  page.value = p
  fetchResources()
}

// ==================== 上传 ====================
function handleUpload() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再上传资料')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  uploadVisible.value = true
}

function onUploadSuccess() {
  page.value = 1
  fetchResources()
  fetchCourse() // 刷新资料数
}

onMounted(() => {
  fetchCourse()
  fetchResources()
  checkJoined()
})
</script>

<style scoped>
.main {
  padding: 24px 24px 48px;
}

.crumb {
  margin-bottom: 16px;
}

/* =============================================================================
   课程头部（渐变横幅）
   ============================================================================= */
.course-head {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  background: var(--ch-gradient-hero);
  padding: 30px 34px;
  margin-bottom: 18px;
  box-shadow: 0 24px 56px rgba(49, 27, 146, 0.26);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(64px);
  pointer-events: none;
}

.blob-1 {
  width: 280px;
  height: 280px;
  top: -120px;
  right: -30px;
  background: rgba(168, 85, 247, 0.5);
  animation: ch-float 10s ease-in-out infinite;
}

.blob-2 {
  width: 220px;
  height: 220px;
  bottom: -130px;
  left: 12%;
  background: rgba(56, 189, 248, 0.4);
  animation: ch-float 12s ease-in-out infinite reverse;
}

.ch-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 22px;
}

.cover {
  width: 78px;
  height: 78px;
  flex-shrink: 0;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(8px);
}

.info {
  flex: 1;
  min-width: 0;
}

.info h1 {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
}

.meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 0 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.meta-chip {
  padding: 3px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #fff;
  font-weight: 500;
}

.stat-pills {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12.5px;
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.head-action {
  flex-shrink: 0;
}

.joined-tag {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 11px 20px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: rgba(16, 185, 129, 0.22);
  border: 1px solid rgba(52, 211, 153, 0.5);
}

/* =============================================================================
   简介
   ============================================================================= */
.desc-card {
  margin-bottom: 20px;
}

.desc-card h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.desc-card h3 .el-icon {
  color: var(--ch-primary);
}

.desc-card p {
  margin: 0;
  font-size: 14px;
  color: var(--ch-text-regular);
  line-height: 1.9;
}

/* =============================================================================
   资料区
   ============================================================================= */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.section-head .count {
  display: inline-block;
  margin-left: 4px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.1);
  vertical-align: middle;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.res-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
}

.res-icon {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
  transition: var(--ch-transition);
}

.res-item:hover .res-icon {
  transform: scale(1.06) rotate(-4deg);
}

.type-EXAM { background: linear-gradient(135deg, #f97316, #ef4444); }
.type-NOTES { background: linear-gradient(135deg, #10b981, #059669); }
.type-TEXTBOOK { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
.type-VIDEO { background: linear-gradient(135deg, #f59e0b, #f97316); }
.type-ASSIGNMENT { background: linear-gradient(135deg, #06b6d4, #0ea5e9); }

.res-body {
  flex: 1;
  min-width: 0;
}

.res-body h4 {
  margin: 0 0 8px;
  font-size: 15.5px;
  font-weight: 600;
}

.res-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
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

.arrow {
  color: var(--ch-text-placeholder);
  transition: var(--ch-transition);
}

.res-item:hover .arrow {
  color: var(--ch-primary);
  transform: translateX(3px);
}

/* ---------- 骨架屏 ---------- */
.sk-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-line {
  height: 12px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 700px) {
  .main {
    padding: 18px 16px 40px;
  }

  .course-head {
    padding: 24px 20px;
  }

  .ch-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .info h1 {
    font-size: 21px;
  }

  .head-actions {
    width: 100%;
  }

  .head-actions :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
