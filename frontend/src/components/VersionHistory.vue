<template>
  <section class="version-section">
    <div class="section-head">
      <h3>
        <el-icon><Share /></el-icon> 版本历史
        <span class="count">{{ versions.length }}</span>
      </h3>
      <el-button
        v-if="canEdit"
        type="primary"
        plain
        round
        size="small"
        :icon="UploadFilled"
        @click="dialogVisible = true"
      >
        上传新版本
      </el-button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">
      <div v-for="n in 2" :key="n" class="ch-skeleton sk-item"></div>
    </div>

    <!-- 版本时间线 -->
    <el-timeline v-else-if="versions.length" class="timeline">
      <el-timeline-item
        v-for="v in versions"
        :key="v.id"
        :timestamp="v.createdAt"
        placement="top"
        :type="v.isCurrent ? 'primary' : ''"
        :hollow="!v.isCurrent"
      >
        <div class="v-card" :class="{ current: v.isCurrent }">
          <div class="v-top">
            <span class="v-no">v{{ v.versionNo }}</span>
            <span v-if="v.isCurrent" class="v-badge current-badge">
              <el-icon><CircleCheckFilled /></el-icon> 当前版本
            </span>
            <span v-if="v.isRollback" class="v-badge rollback-badge">
              <el-icon><RefreshLeft /></el-icon> 回滚生成
            </span>
            <span v-if="v.branch && v.branch !== 'main'" class="v-badge">
              {{ v.branch }}
            </span>

            <span class="v-actions">
              <el-button
                link
                type="primary"
                size="small"
                :icon="Download"
                @click="downloadVersion(v)"
              >
                下载
              </el-button>
              <el-button
                v-if="canEdit && !v.isCurrent"
                link
                type="warning"
                size="small"
                :icon="RefreshLeft"
                @click="handleRollback(v)"
              >
                回滚到此版本
              </el-button>
            </span>
          </div>

          <p class="v-log">{{ v.changeLog || '（无变更说明）' }}</p>

          <p class="v-meta">
            <span v-if="v.editorName"><el-icon><User /></el-icon>{{ v.editorName }}</span>
            <span v-if="v.fileName"><el-icon><Document /></el-icon>{{ v.fileName }}</span>
            <span v-if="v.readableSize"><el-icon><Coin /></el-icon>{{ v.readableSize }}</span>
          </p>
        </div>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-else description="暂无版本记录" :image-size="70" />

    <!-- 上传新版本对话框 -->
    <VersionDialog
      v-model="dialogVisible"
      :resource-id="resourceId"
      :current-version-no="maxVersionNo"
      @success="onVersionAdded"
    />
  </section>
</template>

<script setup>
/**
 * 版本树 / 版本历史
 *
 * 后端的设计是「永不修改历史」：
 *   每次更新内容 = 新增一条版本记录，并把资料的 current_version_id 指针移过去。
 *   回滚 = 以旧版本的文件为内容，**再新增一条版本**，并标记 isRollback + 回滚来源。
 *
 * 所以这里展示的是一条只增不改的时间线，用户可以准确地看到
 * "谁、什么时候、因为什么、改了什么"。
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Share, UploadFilled, Download, RefreshLeft,
  CircleCheckFilled, User, Document, Coin
} from '@element-plus/icons-vue'
import { getVersions, rollbackVersion, downloadVersionFile } from '@/api/resource'
import { useUserStore } from '@/store/user'
import VersionDialog from './VersionDialog.vue'

const props = defineProps({
  resourceId: { type: [Number, String], required: true },
  /** 是否有权限改（上传新版本 / 回滚） */
  canEdit: { type: Boolean, default: false }
})
const emit = defineEmits(['changed'])

const userStore = useUserStore()

const loading = ref(false)
const versions = ref([])
const dialogVisible = ref(false)

/** 当前最新版本号（用于提示"将生成 v(n+1)"） */
const maxVersionNo = computed(() => {
  if (!versions.value.length) return 1
  return Math.max(...versions.value.map((v) => v.versionNo || 1))
})

async function fetchVersions() {
  loading.value = true
  try {
    versions.value = await getVersions(props.resourceId) || []
  } catch (e) {
    versions.value = []
  } finally {
    loading.value = false
  }
}

/** 下载某个历史版本的文件 */
async function downloadVersion(v) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再下载')
    return
  }
  if (!v.fileId) {
    ElMessage.warning('该版本没有关联文件')
    return
  }
  try {
    const blob = await downloadVersionFile(v.fileId)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = v.fileName || `v${v.versionNo}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
  } catch (e) {
    ElMessage.error('下载失败：' + (e.message || '请重试'))
  }
}

/** 回滚 */
async function handleRollback(v) {
  try {
    await ElMessageBox.confirm(
      `确定回滚到 v${v.versionNo} 吗？\n` +
        '回滚不会删除任何历史版本，而是以该版本的内容生成一个新版本。',
      '回滚确认',
      { type: 'warning', confirmButtonText: '确定回滚', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await rollbackVersion(props.resourceId, v.id)
    ElMessage.success('已回滚（已生成新版本）')
    fetchVersions()
    emit('changed')
  } catch (e) {
    // 拦截器已提示
  }
}

function onVersionAdded() {
  fetchVersions()
  emit('changed')
}

// 资料 id 变化时（例如从一条资料跳到另一条）重新加载
watch(() => props.resourceId, fetchVersions)

defineExpose({ fetchVersions })

onMounted(fetchVersions)
</script>

<style scoped>
.version-section {
  margin-top: 6px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.section-head h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}

.section-head h3 .el-icon {
  color: var(--ch-primary);
}

.section-head .count {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.1);
}

/* ---------- 时间线 ---------- */
.timeline {
  padding-left: 4px;
}

.timeline :deep(.el-timeline-item__timestamp) {
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.v-card {
  padding: 14px 18px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
  transition: var(--ch-transition);
}

.v-card:hover {
  border-color: rgba(99, 102, 241, 0.3);
  box-shadow: var(--ch-shadow-sm);
}

.v-card.current {
  background: rgba(79, 70, 229, 0.05);
  border-color: rgba(79, 70, 229, 0.28);
}

.v-top {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.v-no {
  font-size: 15px;
  font-weight: 800;
  color: var(--ch-primary);
  letter-spacing: -0.01em;
}

.v-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--ch-text-secondary);
  background: rgba(100, 116, 139, 0.12);
}

.current-badge {
  color: #047857;
  background: rgba(16, 185, 129, 0.14);
}

.rollback-badge {
  color: #b45309;
  background: rgba(245, 158, 11, 0.16);
}

.v-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.v-log {
  margin: 0 0 8px;
  font-size: 13.5px;
  color: var(--ch-text-regular);
  line-height: 1.7;
}

.v-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.v-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

/* ---------- 骨架 ---------- */
.loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-item {
  height: 74px;
  border-radius: var(--ch-radius);
}
</style>
