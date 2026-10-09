<template>
  <div class="detail-page">
    <AppHeader active="resources" />

    <main class="ch-container main">
      <!-- 骨架屏 -->
      <template v-if="loading && !detail">
        <div class="ch-card">
          <div class="ch-skeleton sk-line" style="width: 50%; height: 26px"></div>
          <div class="ch-skeleton sk-line" style="width: 70%; margin-top: 20px"></div>
          <div class="ch-skeleton sk-line" style="width: 90%; margin-top: 14px"></div>
        </div>
      </template>

      <template v-else-if="detail">
        <!-- 面包屑 -->
        <el-breadcrumb separator="/" class="crumb">
          <el-breadcrumb-item :to="{ path: '/courses' }">课程库</el-breadcrumb-item>
          <el-breadcrumb-item :to="`/courses/${detail.courseId}`">
            {{ detail.courseName || '课程详情' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>资料详情</el-breadcrumb-item>
        </el-breadcrumb>

        <!-- ==================== 资料主体 ==================== -->
        <div class="detail-card ch-card ch-fade-up">
          <!-- 标题区 -->
          <div class="head">
            <div class="head-icon" :class="'type-' + (detail.type || 'OTHER')">
              {{ typeShort(detail.type) }}
            </div>
            <div class="head-main">
              <h1>{{ detail.title }}</h1>
              <div class="tags">
                <span class="type-chip">{{ typeLabel(detail.type) }}</span>
                <span class="vis-chip" :class="'vis-' + (detail.visibility || 'PUBLIC')">
                  <el-icon><component :is="visibilityIcon(detail.visibility)" /></el-icon>
                  {{ visibilityLabel(detail.visibility) }}
                </span>
                <span v-if="detail.status === 'PUBLISHED'" class="ok-chip">
                  <el-icon><CircleCheckFilled /></el-icon> 已发布
                </span>
              </div>
            </div>
          </div>

          <!-- 数据条 -->
          <div class="stat-strip">
            <span class="s-item">
              <b>{{ detail.downloadCount || 0 }}</b> 下载
            </span>
            <span class="s-item">
              <b>{{ detail.viewCount || 0 }}</b> 浏览
            </span>
            <span class="s-item">
              <b>{{ detail.favoriteCount || 0 }}</b> 收藏
            </span>
            <span class="s-item">
              <b>{{ detail.commentCount || 0 }}</b> 评论
            </span>
            <span class="s-item">
              <b>{{ detail.versionCount || 1 }}</b> 版本
            </span>
            <span v-if="detail.score != null" class="s-item score">
              <el-icon><Star /></el-icon> 可信度 <b>{{ Number(detail.score).toFixed(1) }}</b>
            </span>
          </div>

          <!-- 元信息栅格 -->
          <div class="meta-grid">
            <div v-for="m in metaItems" :key="m.label" class="meta-item">
              <span class="meta-label">
                <el-icon><component :is="m.icon" /></el-icon>{{ m.label }}
              </span>
              <span class="meta-value">{{ m.value }}</span>
            </div>
          </div>

          <!-- 操作区 -->
          <div class="actions">
            <el-button
              type="primary"
              size="large"
              round
              :icon="Download"
              :loading="downloading"
              :disabled="!detail.fileId"
              @click="handleDownload"
            >
              下载文件
            </el-button>

            <!-- 收藏（点击切换） -->
            <el-button
              size="large"
              round
              :type="collected ? 'warning' : 'default'"
              :plain="collected"
              :loading="collecting"
              @click="toggleCollect"
            >
              <el-icon v-if="collected"><StarFilled /></el-icon>
              <el-icon v-else><Star /></el-icon>
              {{ collected ? '已收藏' : '收藏' }}
            </el-button>

            <el-button
              v-if="detail.canEdit"
              size="large"
              round
              :icon="Edit"
              @click="openEdit"
            >
              编辑信息
            </el-button>

            <el-button
              v-if="detail.canEdit"
              size="large"
              type="danger"
              plain
              round
              :icon="Delete"
              @click="handleDelete"
            >
              删除资料
            </el-button>

            <!-- 举报 -->
            <el-button
              size="large"
              round
              :icon="Warning"
              @click="openReport"
            >
              举报
            </el-button>

            <span v-if="!userStore.isLoggedIn" class="login-tip">
              <el-icon><InfoFilled /></el-icon> 登录后可下载与收藏
            </span>
          </div>

          <!-- 描述 -->
          <div class="section">
            <h3><el-icon><Document /></el-icon> 资料描述</h3>
            <p class="desc">{{ detail.description || '暂无描述' }}</p>
          </div>
        </div>

        <!-- ==================== 版本历史 / 讨论区 ==================== -->
        <div class="tabs-card ch-card ch-fade-up ch-delay-2">
          <el-tabs v-model="activeTab">
            <el-tab-pane name="versions">
              <template #label>
                <span class="tab-label">
                  <el-icon><Share /></el-icon> 版本历史
                  <span class="tab-count">{{ detail.versionCount || versionCount }}</span>
                </span>
              </template>
              <VersionHistory
                ref="versionRef"
                :resource-id="route.params.id"
                :can-edit="!!detail.canEdit"
                @changed="onVersionChanged"
              />
            </el-tab-pane>

            <el-tab-pane name="comments">
              <template #label>
                <span class="tab-label">
                  <el-icon><ChatDotRound /></el-icon> 讨论区
                  <span class="tab-count">{{ detail.commentCount || 0 }}</span>
                </span>
              </template>
              <CommentSection
                ref="commentRef"
                :resource-id="route.params.id"
                @changed="onCommentChanged"
              />
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>

      <el-empty
        v-else-if="!loading"
        description="资料不存在或你没有权限查看"
      >
        <el-button type="primary" @click="$router.push('/resources')">
          返回资料库
        </el-button>
      </el-empty>
    </main>

    <AppFooter />

    <!-- 举报对话框 -->
    <ReportDialog
      v-model="reportVisible"
      target-type="RESOURCE"
      :target-id="route.params.id"
      :target-name="detail?.title"
      target-label="资料"
    />

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑资料信息" width="540px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.type" style="width: 100%">
            <el-option label="讲义" value="LECTURE" />
            <el-option label="教材" value="TEXTBOOK" />
            <el-option label="真题" value="EXAM" />
            <el-option label="笔记" value="NOTES" />
            <el-option label="作业" value="ASSIGNMENT" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="editForm.visibility" style="width: 100%">
            <el-option label="全平台公开" value="PUBLIC" />
            <el-option label="仅本课程" value="COURSE_ONLY" />
            <el-option label="仅自己可见" value="PRIVATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 资料详情页
 *
 * 这一页是"资料协作"的**汇聚点**，把 M2 + M3 的能力都用上：
 *   · 下载文件（Blob 方式，才能带上 Token）
 *   · 收藏 / 取消收藏（M3）
 *   · 举报资料（M3）
 *   · 版本历史：看历史、下载历史版本、回滚、上传新版本（M3 版本树）
 *   · 讨论区：发评论、回复、删除、举报评论（M3 评论）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download, Edit, Delete, Document, InfoFilled, CircleCheckFilled,
  Calendar, User, Folder, View, Coin, Star, StarFilled, Warning,
  Share, ChatDotRound
} from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import VersionHistory from '@/components/VersionHistory.vue'
import CommentSection from '@/components/CommentSection.vue'
import ReportDialog from '@/components/ReportDialog.vue'
import {
  getResourceDetail,
  updateResource,
  deleteResource
} from '@/api/resource'
import { isCollected, collectResource, uncollectResource } from '@/api/collection'
import request from '@/api/request'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const downloading = ref(false)
const saving = ref(false)
const detail = ref(null)

const activeTab = ref('versions')
const versionRef = ref(null)
const commentRef = ref(null)

// 收藏状态
const collected = ref(false)
const collecting = ref(false)

// 举报
const reportVisible = ref(false)

const versionCount = ref(0)

const editVisible = ref(false)
const editForm = reactive({
  title: '',
  type: 'LECTURE',
  visibility: 'PUBLIC',
  description: ''
})

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

function visibilityLabel(v) {
  return { PUBLIC: '全平台公开', COURSE_ONLY: '仅本课程', PRIVATE: '仅自己可见' }[v] || v
}
function visibilityIcon(v) {
  return { PUBLIC: 'View', COURSE_ONLY: 'UserFilled', PRIVATE: 'Lock' }[v] || 'View'
}

/** 元信息栅格数据 */
const metaItems = computed(() => {
  const d = detail.value || {}
  return [
    { label: '所属课程', value: d.courseName || '-', icon: Folder },
    { label: '上传者', value: d.uploaderName || '-', icon: User },
    { label: '上传时间', value: d.createdAt || '-', icon: Calendar },
    { label: '文件名', value: d.fileName || '-', icon: Document },
    { label: '文件大小', value: d.readableSize || '-', icon: Coin },
    { label: '最近更新', value: d.updatedAt || d.createdAt || '-', icon: Calendar }
  ]
})

// ==================== 加载 ====================
async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getResourceDetail(route.params.id)
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function fetchCollected() {
  if (!userStore.isLoggedIn) {
    collected.value = false
    return
  }
  try {
    collected.value = await isCollected(route.params.id)
  } catch (e) {
    collected.value = false
  }
}

// ==================== 下载 ====================
async function handleDownload() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再下载')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  downloading.value = true
  try {
    // 用 blob 方式拿到文件流（这样才能带上 Authorization 头）
    const blob = await request.get(`/api/resources/${route.params.id}/download`, {
      responseType: 'blob',
      timeout: 300000
    })
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = detail.value.fileName || 'download'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
    // 刷新一下，让下载次数 +1 显示出来
    fetchDetail()
  } catch (e) {
    ElMessage.error('下载失败：' + (e.message || '请重试'))
  } finally {
    downloading.value = false
  }
}

// ==================== 收藏 ====================
async function toggleCollect() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  collecting.value = true
  try {
    if (collected.value) {
      await uncollectResource(route.params.id)
      collected.value = false
      ElMessage.success('已取消收藏')
    } else {
      await collectResource(route.params.id)
      collected.value = true
      ElMessage.success('已加入我的收藏')
    }
    fetchDetail() // 刷新收藏数
  } catch (e) {
    // 拦截器已提示（含 40905 重复收藏）
  } finally {
    collecting.value = false
  }
}

// ==================== 举报 ====================
function openReport() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再举报')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  reportVisible.value = true
}

// ==================== 版本 / 评论变化回调 ====================
function onVersionChanged() {
  fetchDetail()
}

function onCommentChanged() {
  fetchDetail()
}

// ==================== 编辑 ====================
function openEdit() {
  Object.assign(editForm, {
    title: detail.value.title,
    type: detail.value.type,
    visibility: detail.value.visibility,
    description: detail.value.description
  })
  editVisible.value = true
}

async function submitEdit() {
  saving.value = true
  try {
    // 注意：PUT 是全量更新，courseId / fileId 这些必填字段也要带上
    await updateResource(route.params.id, {
      courseId: detail.value.courseId,
      fileId: detail.value.fileId,
      title: editForm.title,
      type: editForm.type,
      visibility: editForm.visibility,
      description: editForm.description
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    fetchDetail()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

// ==================== 删除 ====================
async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${detail.value.title}》吗？删除后无法恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return // 用户取消
  }
  try {
    await deleteResource(route.params.id)
    ElMessage.success('已删除')
    router.push('/resources')
  } catch (e) {
    // 拦截器已提示
  }
}

onMounted(() => {
  fetchDetail()
  fetchCollected()
})
</script>

<style scoped>
.main {
  padding: 24px 24px 48px;
}

.crumb {
  margin-bottom: 16px;
}

.detail-card {
  padding: 30px 32px;
}

/* ---------- 标题区 ---------- */
.head {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--ch-border-light);
  margin-bottom: 20px;
}

.head-icon {
  width: 62px;
  height: 62px;
  flex-shrink: 0;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.3);
}

.type-EXAM { background: linear-gradient(135deg, #f97316, #ef4444); }
.type-NOTES { background: linear-gradient(135deg, #10b981, #059669); }
.type-TEXTBOOK { background: linear-gradient(135deg, #8b5cf6, #6d28d9); }
.type-VIDEO { background: linear-gradient(135deg, #f59e0b, #f97316); }
.type-ASSIGNMENT { background: linear-gradient(135deg, #06b6d4, #0ea5e9); }

.head-main {
  flex: 1;
  min-width: 0;
}

.head-main h1 {
  margin: 4px 0 12px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.type-chip,
.vis-chip,
.ok-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 500;
}

.type-chip {
  color: var(--ch-primary-dark);
  background: rgba(79, 70, 229, 0.1);
}

.vis-PUBLIC { color: #047857; background: rgba(16, 185, 129, 0.12); }
.vis-COURSE_ONLY { color: #b45309; background: rgba(245, 158, 11, 0.14); }
.vis-PRIVATE { color: #b91c1c; background: rgba(239, 68, 68, 0.12); }

.ok-chip { color: #047857; background: rgba(16, 185, 129, 0.12); }

/* ---------- 数据条 ---------- */
.stat-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.s-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 15px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
}

.s-item b {
  font-size: 15px;
  color: var(--ch-text-primary);
  font-weight: 700;
}

.s-item.score {
  color: #b45309;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.25);
}

.s-item.score b {
  color: #b45309;
}

/* ---------- 元信息栅格 ---------- */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
  transition: var(--ch-transition);
}

.meta-item:hover {
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(79, 70, 229, 0.04);
}

.meta-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.meta-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--ch-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 操作区 ---------- */
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 20px 0;
  border-top: 1px solid var(--ch-border-light);
  border-bottom: 1px solid var(--ch-border-light);
  margin-bottom: 24px;
}

.login-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ch-text-secondary);
  margin-left: 4px;
}

/* ---------- 描述 ---------- */
.section h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.section h3 .el-icon {
  color: var(--ch-primary);
}

.desc {
  margin: 0;
  font-size: 14px;
  color: var(--ch-text-regular);
  line-height: 1.95;
  white-space: pre-wrap;
}

/* ---------- Tabs 卡片 ---------- */
.tabs-card {
  margin-top: 18px;
  padding: 8px 28px 26px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14.5px;
  font-weight: 600;
}

.tab-count {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.1);
}

.tabs-card :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

/* ---------- 骨架屏 ---------- */
.sk-line {
  height: 14px;
}

@media (max-width: 800px) {
  .main {
    padding: 18px 16px 40px;
  }

  .detail-card {
    padding: 22px 20px;
  }

  .tabs-card {
    padding: 6px 18px 20px;
  }

  .meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .head-main h1 {
    font-size: 19px;
  }
}

@media (max-width: 480px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .head-icon {
    width: 52px;
    height: 52px;
    font-size: 22px;
  }
}
</style>
