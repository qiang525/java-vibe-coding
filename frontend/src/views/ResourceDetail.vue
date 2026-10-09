<template>
  <div class="detail-page">
    <AppHeader active="resources" />

    <main class="ch-container main" v-loading="loading">
      <template v-if="detail">
        <!-- 面包屑 -->
        <el-breadcrumb separator="/" class="crumb">
          <el-breadcrumb-item :to="{ path: '/courses' }">课程</el-breadcrumb-item>
          <el-breadcrumb-item :to="`/courses/${detail.courseId}`">
            {{ detail.courseName || '课程详情' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>资料详情</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="detail-card ch-card">
          <!-- 标题区 -->
          <div class="head">
            <h1>{{ detail.title }}</h1>
            <div class="tags">
              <el-tag size="small">{{ typeLabel(detail.type) }}</el-tag>
              <el-tag
                size="small"
                :type="visibilityType(detail.visibility)"
                effect="plain"
              >
                {{ visibilityLabel(detail.visibility) }}
              </el-tag>
              <el-tag size="small" type="success" effect="plain" v-if="detail.status === 'PUBLISHED'">
                已发布
              </el-tag>
            </div>
          </div>

          <!-- 元信息 -->
          <el-descriptions :column="3" border class="meta">
            <el-descriptions-item label="所属课程">
              {{ detail.courseName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="上传者">
              {{ detail.uploaderName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="上传时间">
              {{ detail.createdAt || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="文件名">
              {{ detail.fileName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="文件大小">
              {{ detail.readableSize || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="浏览次数">
              {{ detail.viewCount || 0 }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 操作区 -->
          <div class="actions">
            <el-button
              type="primary"
              size="large"
              :icon="Download"
              :loading="downloading"
              :disabled="!detail.fileId"
              @click="handleDownload"
            >
              下载文件
            </el-button>

            <el-button
              v-if="detail.canEdit"
              size="large"
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
              :icon="Delete"
              @click="handleDelete"
            >
              删除资料
            </el-button>

            <span v-if="!userStore.isLoggedIn" class="login-tip">
              登录后可下载文件
            </span>
          </div>

          <!-- 描述 -->
          <div class="section">
            <h3>资料描述</h3>
            <p class="desc">{{ detail.description || '暂无描述' }}</p>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="资料不存在或你没有权限查看">
        <el-button type="primary" @click="$router.push('/resources')">
          返回资料库
        </el-button>
      </el-empty>
    </main>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" title="编辑资料信息" width="520px">
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
 * 关键点：**下载必须用 axios 拿到 Blob 再触发下载**，不能直接用
 * `<a href="/api/resources/1/download">` —— 因为浏览器直接打开链接带不上
 * Token，后端会返回 401。
 */
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Edit, Delete } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import {
  getResourceDetail,
  updateResource,
  deleteResource
} from '@/api/resource'
import request from '@/api/request'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const downloading = ref(false)
const saving = ref(false)
const detail = ref(null)

const editVisible = ref(false)
const editForm = reactive({
  title: '',
  type: 'LECTURE',
  visibility: 'PUBLIC',
  description: ''
})

const TYPE_MAP = {
  TEXTBOOK: '教材', LECTURE: '讲义', EXAM: '真题',
  NOTES: '笔记', ASSIGNMENT: '作业', VIDEO: '视频', OTHER: '其他'
}
function typeLabel(t) { return TYPE_MAP[t] || '其他' }

function visibilityLabel(v) {
  return { PUBLIC: '全平台公开', COURSE_ONLY: '仅本课程', PRIVATE: '仅自己可见' }[v] || v
}
function visibilityType(v) {
  return { PUBLIC: 'success', COURSE_ONLY: 'warning', PRIVATE: 'danger' }[v] || 'info'
}

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

onMounted(fetchDetail)
</script>

<style scoped>
.main {
  padding: 24px 20px 48px;
}

.crumb {
  margin-bottom: 14px;
}

.detail-card {
  padding: 26px;
}

.head h1 {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 600;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
}

.meta {
  margin-bottom: 20px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid var(--ch-border-light);
  border-bottom: 1px solid var(--ch-border-light);
  margin-bottom: 20px;
}

.login-tip {
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.section h3 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}

.desc {
  margin: 0;
  font-size: 14px;
  color: var(--ch-text-regular);
  line-height: 1.9;
  white-space: pre-wrap;
}
</style>
