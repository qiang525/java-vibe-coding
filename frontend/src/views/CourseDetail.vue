<template>
  <div class="course-detail-page">
    <AppHeader active="courses" />

    <main class="ch-container main" v-loading="loading">
      <template v-if="course">
        <!-- 课程头部 -->
        <section class="course-head ch-card">
          <div class="ch-left">
            <div class="cover">{{ (course.name || '课').charAt(0) }}</div>
            <div class="info">
              <h1>{{ course.name }}</h1>
              <p class="meta">
                <span v-if="course.collegeName">{{ course.collegeName }}</span>
                <span v-if="course.teacher">· {{ course.teacher }}</span>
                <span v-if="course.term">· {{ course.term }}</span>
                <span v-if="course.credit">· {{ course.credit }} 学分</span>
              </p>
              <p class="stats">
                成员 {{ course.memberCount || 0 }} · 资料 {{ course.resourceCount || 0 }}
              </p>
            </div>
          </div>

          <div class="ch-right">
            <el-button
              v-if="!joined"
              type="primary"
              :loading="joining"
              @click="handleJoin"
            >
              加入课程
            </el-button>
            <el-tag v-else type="success" size="large" effect="plain">
              已加入该课程
            </el-tag>
          </div>
        </section>

        <!-- 课程简介 -->
        <section v-if="course.description" class="desc-card ch-card">
          <h3>课程简介</h3>
          <p>{{ course.description }}</p>
        </section>

        <!-- 资料区 -->
        <section class="resource-section">
          <div class="section-head">
            <h3>课程资料（{{ total }}）</h3>
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
              <el-button type="primary" :icon="UploadFilled" @click="handleUpload">
                上传资料
              </el-button>
            </div>
          </div>

          <div v-loading="resLoading" class="resource-list">
            <template v-if="resources.length">
              <div
                v-for="r in resources"
                :key="r.id"
                class="res-item ch-card"
                @click="$router.push(`/resources/${r.id}`)"
              >
                <div class="res-body">
                  <h4 class="ch-ellipsis">{{ r.title }}</h4>
                  <p class="res-meta">
                    <el-tag size="small" effect="plain">{{ typeLabel(r.type) }}</el-tag>
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
import { Search, UploadFilled, ArrowRight } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
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
  TEXTBOOK: '教材', LECTURE: '讲义', EXAM: '真题',
  NOTES: '笔记', ASSIGNMENT: '作业', VIDEO: '视频', OTHER: '其他'
}
function typeLabel(t) { return TYPE_MAP[t] || '其他' }

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
  padding: 24px 20px 48px;
}

/* ---------- 课程头 ---------- */
.course-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 26px;
  margin-bottom: 16px;
}

.ch-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.cover {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 26px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info h1 {
  margin: 0 0 6px;
  font-size: 21px;
  font-weight: 600;
}

.meta,
.stats {
  margin: 0;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.stats {
  margin-top: 4px;
}

/* ---------- 简介 ---------- */
.desc-card {
  padding: 18px 22px;
  margin-bottom: 16px;
}

.desc-card h3 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}

.desc-card p {
  margin: 0;
  font-size: 14px;
  color: var(--ch-text-regular);
  line-height: 1.8;
}

/* ---------- 资料区 ---------- */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}

.section-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.res-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.res-item:hover {
  transform: translateX(3px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.07);
}

.res-body h4 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
}

.res-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.arrow {
  color: var(--ch-text-placeholder);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}
</style>
