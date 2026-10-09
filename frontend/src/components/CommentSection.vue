<template>
  <section class="comment-section">
    <div class="section-head">
      <h3>
        <el-icon><ChatDotRound /></el-icon> 讨论区
        <span class="count">{{ total }}</span>
      </h3>
    </div>

    <!-- ========== 发表评论 ========== -->
    <div v-if="userStore.isLoggedIn" class="editor">
      <el-avatar :size="38" class="avatar">{{ myAvatar }}</el-avatar>
      <div class="editor-main">
        <el-input
          v-model="newContent"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          resize="none"
          placeholder="说说你的看法，或指出资料中的问题…"
        />
        <div class="editor-foot">
          <el-radio-group v-model="newType" size="small">
            <el-radio-button value="NORMAL">讨论</el-radio-button>
            <el-radio-button value="ERROR">纠错</el-radio-button>
            <el-radio-button value="SUPPLEMENT">补充</el-radio-button>
          </el-radio-group>
          <el-button
            type="primary"
            round
            :loading="posting"
            :disabled="!newContent.trim()"
            @click="submitComment"
          >
            发表评论
          </el-button>
        </div>
      </div>
    </div>

    <div v-else class="login-hint">
      <el-icon><InfoFilled /></el-icon>
      <span>登录后即可参与讨论</span>
      <el-button link type="primary" @click="goLogin">去登录</el-button>
    </div>

    <!-- ========== 评论列表 ========== -->
    <div v-loading="loading" class="list">
      <template v-if="list.length">
        <div v-for="c in list" :key="c.id" class="comment">
          <!-- 一级评论 -->
          <div class="c-main">
            <el-avatar :size="36" class="avatar">{{ initial(c.userName) }}</el-avatar>
            <div class="c-body">
              <div class="c-top">
                <span class="c-name">{{ c.userName || '匿名用户' }}</span>
                <span v-if="c.type === 'ERROR'" class="type-tag error">纠错</span>
                <span v-else-if="c.type === 'SUPPLEMENT'" class="type-tag supplement">补充</span>
                <span class="c-time">{{ c.createdAt }}</span>
              </div>
              <p class="c-content">{{ c.content }}</p>
              <div class="c-actions">
                <el-button link size="small" @click="toggleReply(c)">
                  <el-icon><ChatLineSquare /></el-icon> 回复{{ c.replyCount ? ` (${c.replyCount})` : '' }}
                </el-button>
                <el-button
                  v-if="userStore.isLoggedIn"
                  link
                  size="small"
                  @click="openReport(c)"
                >
                  <el-icon><Warning /></el-icon> 举报
                </el-button>
                <el-button
                  v-if="c.canDelete"
                  link
                  type="danger"
                  size="small"
                  @click="handleDelete(c)"
                >
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>

              <!-- 回复输入框 -->
              <div v-if="replyingId === c.id" class="reply-editor">
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="2"
                  maxlength="1000"
                  resize="none"
                  :placeholder="`回复 @${c.userName || '匿名用户'}`"
                />
                <div class="reply-foot">
                  <el-button size="small" round @click="toggleReply(c)">取消</el-button>
                  <el-button
                    size="small"
                    type="primary"
                    round
                    :loading="posting"
                    :disabled="!replyContent.trim()"
                    @click="submitReply(c)"
                  >
                    回复
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 二级回复 -->
          <div v-if="c.replies && c.replies.length" class="replies">
            <div v-for="r in c.replies" :key="r.id" class="reply">
              <el-avatar :size="28" class="avatar">{{ initial(r.userName) }}</el-avatar>
              <div class="c-body">
                <div class="c-top">
                  <span class="c-name">{{ r.userName || '匿名用户' }}</span>
                  <span class="c-time">{{ r.createdAt }}</span>
                </div>
                <p class="c-content">{{ r.content }}</p>
                <div class="c-actions">
                  <el-button
                    v-if="userStore.isLoggedIn"
                    link
                    size="small"
                    @click="openReport(r)"
                  >
                    <el-icon><Warning /></el-icon> 举报
                  </el-button>
                  <el-button
                    v-if="r.canDelete"
                    link
                    type="danger"
                    size="small"
                    @click="handleDelete(r)"
                  >
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty
        v-else-if="!loading"
        :image-size="80"
        description="还没有人讨论，来说第一句吧"
      />
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 举报对话框 -->
    <ReportDialog
      v-model="reportVisible"
      target-type="COMMENT"
      :target-id="reportTarget.id"
      :target-name="reportTarget.content"
      target-label="评论"
    />
  </section>
</template>

<script setup>
/**
 * 评论区
 *
 * 两级结构：一级评论 + 它的回复。
 * 后端一次请求就把回复嵌在每条一级评论的 replies 里了，
 * 所以前端不需要为每条评论再单独请求（避免 N+1）。
 *
 * 权限：发表/回复/举报需要登录；删除按钮由后端算好的 canDelete 控制显示。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound, ChatLineSquare, Warning, Delete, InfoFilled
} from '@element-plus/icons-vue'
import { getComments, createComment, deleteComment } from '@/api/comment'
import { useUserStore } from '@/store/user'
import ReportDialog from './ReportDialog.vue'

const props = defineProps({
  resourceId: { type: [Number, String], required: true }
})
const emit = defineEmits(['changed'])

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const posting = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const newContent = ref('')
const newType = ref('NORMAL')

const replyingId = ref(null)
const replyContent = ref('')

const reportVisible = ref(false)
const reportTarget = ref({ id: null, content: '' })

const myAvatar = computed(() => initial(userStore.displayName))

function initial(name) {
  return name ? String(name).charAt(0).toUpperCase() : 'U'
}

// ==================== 加载 ====================
async function fetchComments() {
  loading.value = true
  try {
    const data = await getComments(props.resourceId, {
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

// ==================== 发表 ====================
async function submitComment() {
  const content = newContent.value.trim()
  if (!content) return

  posting.value = true
  try {
    await createComment(props.resourceId, { content, type: newType.value })
    ElMessage.success('评论成功')
    newContent.value = ''
    newType.value = 'NORMAL'
    page.value = 1
    fetchComments()
    emit('changed')
  } catch (e) {
    // 拦截器已提示
  } finally {
    posting.value = false
  }
}

// ==================== 回复 ====================
function toggleReply(c) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再回复')
    return
  }
  if (replyingId.value === c.id) {
    replyingId.value = null
    replyContent.value = ''
    return
  }
  replyingId.value = c.id
  replyContent.value = ''
}

async function submitReply(parent) {
  const content = replyContent.value.trim()
  if (!content) return

  posting.value = true
  try {
    await createComment(props.resourceId, { content, parentId: parent.id })
    ElMessage.success('回复成功')
    replyingId.value = null
    replyContent.value = ''
    fetchComments()
    emit('changed')
  } catch (e) {
    // 拦截器已提示
  } finally {
    posting.value = false
  }
}

// ==================== 删除 ====================
async function handleDelete(c) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteComment(c.id)
    ElMessage.success('已删除')
    fetchComments()
    emit('changed')
  } catch (e) {
    // 拦截器已提示
  }
}

// ==================== 举报 ====================
function openReport(c) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再举报')
    return
  }
  reportTarget.value = {
    id: c.id,
    content: (c.content || '').slice(0, 40)
  }
  reportVisible.value = true
}

// ==================== 其他 ====================
function handlePageChange(p) {
  page.value = p
  fetchComments()
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: `/resources/${props.resourceId}` } })
}

watch(() => props.resourceId, () => {
  page.value = 1
  fetchComments()
})

defineExpose({ fetchComments })

onMounted(fetchComments)
</script>

<style scoped>
.comment-section {
  margin-top: 6px;
}

.section-head {
  margin-bottom: 18px;
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

/* ---------- 发表区 ---------- */
.editor {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
  margin-bottom: 22px;
}

.editor-main {
  flex: 1;
  min-width: 0;
}

.editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.login-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  border-radius: var(--ch-radius);
  font-size: 13.5px;
  color: var(--ch-text-secondary);
  background: var(--ch-bg-muted);
  border: 1px dashed var(--ch-border);
  margin-bottom: 22px;
}

/* ---------- 列表 ---------- */
.list {
  min-height: 80px;
}

.comment {
  padding: 16px 0;
  border-bottom: 1px solid var(--ch-border-light);
}

.comment:first-child {
  padding-top: 0;
}

.c-main,
.reply {
  display: flex;
  gap: 12px;
}

.avatar {
  background: var(--ch-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.c-body {
  flex: 1;
  min-width: 0;
}

.c-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.c-name {
  font-size: 13.5px;
  font-weight: 600;
}

.c-time {
  font-size: 12px;
  color: var(--ch-text-placeholder);
}

.type-tag {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 500;
}

.type-tag.error {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.type-tag.supplement {
  color: #b45309;
  background: rgba(245, 158, 11, 0.14);
}

.c-content {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ch-text-regular);
  white-space: pre-wrap;
  word-break: break-word;
}

.c-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* ---------- 回复 ---------- */
.replies {
  margin: 12px 0 0 48px;
  padding: 12px 16px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply + .reply {
  padding-top: 12px;
  border-top: 1px dashed var(--ch-border);
}

.reply-editor {
  margin-top: 12px;
}

.reply-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

@media (max-width: 600px) {
  .replies {
    margin-left: 20px;
  }
}
</style>
