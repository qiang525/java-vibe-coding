<template>
  <div class="report-page">
    <AppHeader active="reports" />

    <main class="ch-container main">
      <!-- 非管理员：无权限提示 -->
      <div v-if="!userStore.isAdmin" class="no-perm ch-card ch-fade-up">
        <div class="np-icon"><el-icon :size="30"><Lock /></el-icon></div>
        <h2>需要管理员权限</h2>
        <p>举报处理页面仅对管理员开放。当前账号没有该权限。</p>
        <el-button type="primary" round @click="$router.push('/')">返回首页</el-button>
      </div>

      <template v-else>
        <div class="page-head ch-fade-up">
          <div class="ch-page-head">
            <h2>举报处理</h2>
            <p>核实用户提交的举报，并给出处理结论</p>
          </div>
          <div class="stat-mini">
            <span class="mini-item warn">
              <b>{{ pendingCount }}</b> 待处理
            </span>
          </div>
        </div>

        <!-- 状态筛选 -->
        <div class="filter-bar ch-card ch-fade-up ch-delay-1">
          <el-radio-group v-model="status" @change="handleFilterChange">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待处理</el-radio-button>
            <el-radio-button value="HANDLED">已处理</el-radio-button>
            <el-radio-button value="REJECTED">已驳回</el-radio-button>
          </el-radio-group>

          <span v-if="!loading" class="result-count">共 {{ total }} 条</span>
        </div>

        <!-- 举报列表 -->
        <div class="report-list">
          <template v-if="loading && !list.length">
            <div v-for="n in 3" :key="'sk' + n" class="rp-card ch-card">
              <div class="ch-skeleton sk-line" style="width: 40%"></div>
              <div class="ch-skeleton sk-line" style="width: 80%"></div>
            </div>
          </template>

          <template v-else-if="list.length">
            <div
              v-for="(r, i) in list"
              :key="r.id"
              class="rp-card ch-card ch-fade-up"
              :class="`ch-delay-${(i % 3) + 1}`"
            >
              <div class="rp-top">
                <span class="rp-target-chip" :class="'tt-' + r.targetType">
                  {{ REPORT_TARGET_LABELS[r.targetType] || r.targetType }}
                </span>
                <h3 class="ch-ellipsis">{{ r.targetTitle || '（对象不存在或已删除）' }}</h3>
                <el-tag
                  :type="REPORT_STATUS_TYPES[r.status] || 'info'"
                  size="small"
                  effect="light"
                  class="rp-status"
                >
                  {{ REPORT_STATUS_LABELS[r.status] || r.status }}
                </el-tag>
              </div>

              <div class="rp-body">
                <p class="rp-row">
                  <span class="rp-label">举报原因</span>
                  <span class="reason-chip">
                    {{ REPORT_REASON_LABELS[r.reason] || r.reason }}
                  </span>
                </p>
                <p class="rp-row" v-if="r.description">
                  <span class="rp-label">补充说明</span>
                  <span class="rp-text">{{ r.description }}</span>
                </p>
                <p class="rp-row">
                  <span class="rp-label">举报人</span>
                  <span class="rp-text">{{ r.reporterName || '未知' }}</span>
                  <span class="rp-time">{{ r.createdAt }}</span>
                </p>

                <!-- 处理结果 -->
                <div v-if="r.status !== 'PENDING'" class="handled-box">
                  <p class="rp-row">
                    <span class="rp-label">处理人</span>
                    <span class="rp-text">{{ r.handlerName || '管理员' }}</span>
                    <span class="rp-time">{{ r.handledAt }}</span>
                  </p>
                  <p class="rp-row" v-if="r.handleNote">
                    <span class="rp-label">处理说明</span>
                    <span class="rp-text">{{ r.handleNote }}</span>
                  </p>
                </div>
              </div>

              <div class="rp-foot">
                <el-button
                  v-if="r.targetType === 'RESOURCE' && r.targetId"
                  link
                  type="primary"
                  size="small"
                  @click="$router.push(`/resources/${r.targetId}`)"
                >
                  查看被举报资料
                </el-button>
                <el-button
                  v-if="r.status === 'PENDING'"
                  type="primary"
                  size="small"
                  round
                  @click="openHandle(r)"
                >
                  处理该举报
                </el-button>
              </div>
            </div>
          </template>

          <el-empty
            v-else-if="!loading"
            :image-size="80"
            :description="status ? '该状态下暂无举报' : '暂无举报记录'"
          />
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
      </template>
    </main>

    <AppFooter />

    <!-- 处理对话框 -->
    <el-dialog v-model="handleVisible" title="处理举报" width="540px">
      <div class="handle-target">
        <p class="ht-label">被举报对象</p>
        <p class="ht-name">{{ current?.targetTitle || '-' }}</p>
        <p class="ht-reason">
          原因：{{ REPORT_REASON_LABELS[current?.reason] || current?.reason }}
        </p>
      </div>

      <el-form :model="handleForm" label-position="top">
        <el-form-item label="处理结论" required>
          <el-radio-group v-model="handleForm.status" class="handle-group">
            <el-radio value="HANDLED" border>属实，已处理</el-radio>
            <el-radio value="REJECTED" border>不属实，驳回</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.handleNote"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="记录处理依据，便于后续追溯（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button round @click="handleVisible = false">取消</el-button>
        <el-button type="primary" round :loading="submitting" @click="submitHandle">
          确认处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 举报处理（管理员）
 *
 * 后端两个接口都带 @RequireRole("ADMIN")，非管理员调会被拦（40302）。
 * 页面上也做一次判断，直接给出友好提示，避免无意义的请求。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import {
  getReports, handleReport,
  REPORT_TARGET_LABELS, REPORT_REASON_LABELS,
  REPORT_STATUS_LABELS, REPORT_STATUS_TYPES
} from '@/api/report'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const status = ref('')

/** 待处理数量（用于页头提示，单独查一次 PENDING） */
const pendingCount = ref(0)

const handleVisible = ref(false)
const current = ref(null)
const handleForm = reactive({ status: 'HANDLED', handleNote: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getReports({
      status: status.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchPendingCount() {
  try {
    const data = await getReports({ status: 'PENDING', page: 1, pageSize: 1 })
    pendingCount.value = data.total || 0
  } catch (e) {
    pendingCount.value = 0
  }
}

function handleFilterChange() {
  page.value = 1
  fetchList()
}

function handlePageChange(p) {
  page.value = p
  fetchList()
}

function openHandle(r) {
  current.value = r
  handleForm.status = 'HANDLED'
  handleForm.handleNote = ''
  handleVisible.value = true
}

async function submitHandle() {
  if (!current.value) return
  submitting.value = true
  try {
    await handleReport(current.value.id, {
      status: handleForm.status,
      handleNote: handleForm.handleNote
    })
    ElMessage.success('处理完成')
    handleVisible.value = false
    fetchList()
    fetchPendingCount()
  } catch (e) {
    // 拦截器已提示（含 40904 重复处理）
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (userStore.isAdmin) {
    fetchList()
    fetchPendingCount()
  }
})
</script>

<style scoped>
.main {
  padding: 30px 24px 48px;
}

/* ---------- 无权限 ---------- */
.no-perm {
  text-align: center;
  padding: 56px 24px;
}

.np-icon {
  width: 66px;
  height: 66px;
  margin: 0 auto 18px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #94a3b8, #64748b);
}

.no-perm h2 {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
}

.no-perm p {
  margin: 0 0 22px;
  font-size: 13.5px;
  color: var(--ch-text-secondary);
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-mini {
  display: flex;
  gap: 12px;
}

.mini-item {
  padding: 10px 18px;
  border-radius: var(--ch-radius);
  font-size: 13px;
  color: var(--ch-text-secondary);
  background: #fff;
  border: 1px solid var(--ch-border-light);
  box-shadow: var(--ch-shadow-sm);
}

.mini-item.warn {
  color: #b45309;
  background: rgba(245, 158, 11, 0.08);
  border-color: rgba(245, 158, 11, 0.25);
}

.mini-item b {
  font-size: 18px;
  margin-right: 4px;
}

/* ---------- 筛选 ---------- */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 13px;
  color: var(--ch-text-secondary);
}

/* ---------- 列表 ---------- */
.report-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
}

.rp-card {
  padding: 20px 22px;
}

.rp-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.rp-top h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  max-width: 480px;
}

.rp-status {
  margin-left: auto;
}

.rp-target-chip {
  padding: 3px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  flex-shrink: 0;
}

.tt-RESOURCE { background: linear-gradient(135deg, #4f46e5, #7c3aed); }
.tt-COMMENT { background: linear-gradient(135deg, #06b6d4, #0ea5e9); }
.tt-USER { background: linear-gradient(135deg, #f59e0b, #f97316); }

.rp-body {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 14px 16px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
  margin-bottom: 14px;
}

.rp-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 13px;
  flex-wrap: wrap;
}

.rp-label {
  flex-shrink: 0;
  padding: 2px 9px;
  border-radius: 6px;
  font-size: 11.5px;
  color: var(--ch-text-secondary);
  background: rgba(100, 116, 139, 0.1);
}

.rp-text {
  color: var(--ch-text-regular);
  word-break: break-word;
}

.rp-time {
  font-size: 12px;
  color: var(--ch-text-placeholder);
}

.reason-chip {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.1);
}

.handled-box {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed var(--ch-border);
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.rp-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.sk-line {
  height: 14px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* ---------- 处理对话框 ---------- */
.handle-target {
  padding: 14px 16px;
  border-radius: var(--ch-radius);
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.25);
  margin-bottom: 20px;
}

.ht-label {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.ht-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
}

.ht-reason {
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.handle-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.handle-group :deep(.el-radio.is-bordered) {
  margin-right: 0;
  border-radius: 10px;
}

@media (max-width: 700px) {
  .main {
    padding: 20px 16px 40px;
  }

  .rp-status {
    margin-left: 0;
  }

  .rp-top h3 {
    max-width: 100%;
  }
}
</style>
