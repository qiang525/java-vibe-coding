<template>
  <el-dialog
    v-model="visible"
    title="上传资料"
    width="640px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <!-- 步骤指示 -->
    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step title="选择文件" />
      <el-step title="上传文件" />
      <el-step title="填写信息" />
    </el-steps>

    <!-- ============ 第一步：选文件 ============ -->
    <div v-if="step === 0" class="step-body">
      <div
        class="drop-zone"
        :class="{ dragging }"
        @click="pickFile"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <span class="dz-glow"></span>
        <div class="dz-icon">
          <el-icon :size="30"><UploadFilled /></el-icon>
        </div>
        <p class="drop-main">点击选择文件，或把文件拖到这里</p>
        <p class="drop-tip">
          支持 PDF / Word / PPT / Excel / 图片 / 压缩包<br />
          单个文件最大 500MB，大文件会自动分片上传
        </p>
      </div>
      <input
        ref="fileInputRef"
        type="file"
        style="display: none"
        @change="onFileChange"
      />
    </div>

    <!-- ============ 第二步：上传中 ============ -->
    <div v-else-if="step === 1" class="step-body step-uploading">
      <div class="up-card">
        <div class="up-icon">
          <el-icon :size="22"><Document /></el-icon>
        </div>
        <div class="up-info">
          <p class="file-name ch-ellipsis">{{ file?.name }}</p>
          <p class="file-size">{{ formatSize(file?.size) }} · {{ statusText }}</p>
        </div>
        <span class="up-percent">{{ percent }}%</span>
      </div>

      <el-progress
        :percentage="percent"
        :status="percent === 100 ? 'success' : undefined"
        :stroke-width="12"
        :show-text="false"
        striped
        :striped-flow="percent < 100"
        class="progress"
      />

      <p class="up-tip">
        <el-icon><InfoFilled /></el-icon>
        上传过程中请勿关闭本窗口；中途失败可重新上传，已传的分片不会重传。
      </p>
    </div>

    <!-- ============ 第三步：填资料信息 ============ -->
    <div v-else class="step-body">
      <div class="ok-banner">
        <el-icon :size="20"><CircleCheckFilled /></el-icon>
        <div>
          <strong>文件已上传完成</strong>
          <p>填写资料信息后即可发布</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="84px"
        class="info-form"
      >
        <el-form-item label="资料标题" prop="title">
          <el-input v-model="form.title" placeholder="如：第二章 线性表 讲义" />
        </el-form-item>

        <el-form-item label="资料类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="t in typeOptions"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="可见范围" prop="visibility">
          <el-radio-group v-model="form.visibility" class="vis-group">
            <el-radio
              v-for="v in visibilityOptions"
              :key="v.value"
              :value="v.value"
              border
            >
              {{ v.label }}
            </el-radio>
          </el-radio-group>
          <p class="radio-tip">{{ currentVisibilityTip }}</p>
        </el-form-item>

        <el-form-item label="资料描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="简单说明这份资料的内容（选填）"
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div class="footer">
        <el-button round @click="visible = false">
          {{ step === 2 ? '取消' : '关闭' }}
        </el-button>
        <el-button
          v-if="step === 2"
          type="primary"
          round
          :loading="publishing"
          @click="submit"
        >
          发布资料
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 上传资料对话框
 *
 * 三步流程：
 *   ① 选择文件 → ② 上传（分片 + 秒传 + 断点续传）→ ③ 填写信息并发布
 *
 * 上传完拿到 fileId，再调发布接口把文件"挂"到课程下 —— 这就是后端的
 * "上传与发布两步走"设计在前端的体现。
 */
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UploadFilled, Document, InfoFilled, CircleCheckFilled
} from '@element-plus/icons-vue'
import { chunkUpload, formatSize } from '@/utils/upload'
import { publishResource } from '@/api/resource'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  courseId: { type: [Number, String], default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

/** 当前步骤：0 选文件 / 1 上传中 / 2 填信息 */
const step = ref(0)
const fileInputRef = ref(null)
const dragging = ref(false)
const file = ref(null)
const percent = ref(0)
const statusText = ref('')
const fileId = ref(null)
const publishing = ref(false)

const typeOptions = [
  { value: 'LECTURE', label: '讲义' },
  { value: 'TEXTBOOK', label: '教材' },
  { value: 'EXAM', label: '真题' },
  { value: 'NOTES', label: '笔记' },
  { value: 'ASSIGNMENT', label: '作业' },
  { value: 'VIDEO', label: '视频' },
  { value: 'OTHER', label: '其他' }
]

const visibilityOptions = [
  { value: 'PUBLIC', label: '全平台公开' },
  { value: 'COURSE_ONLY', label: '仅本课程' },
  { value: 'PRIVATE', label: '仅自己可见' }
]

const formRef = ref(null)
const form = reactive({
  title: '',
  type: 'LECTURE',
  visibility: 'PUBLIC',
  description: ''
})

const rules = {
  title: [
    { required: true, message: '请输入资料标题', trigger: 'blur' },
    { max: 200, message: '标题最长 200 个字符', trigger: 'blur' }
  ]
}

const currentVisibilityTip = computed(() => {
  const map = {
    PUBLIC: '所有访问者都能看到这份资料',
    COURSE_ONLY: '只有本课程成员和课程负责人能看到',
    PRIVATE: '只有你自己能看到，适合存个人笔记'
  }
  return map[form.visibility] || ''
})

// ==================== 第一步：选文件 ====================
function pickFile() {
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const f = e.target.files?.[0]
  if (f) {
    startUpload(f)
  }
  e.target.value = '' // 允许重复选择同一个文件
}

function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) {
    startUpload(f)
  }
}

// ==================== 第二步：上传 ====================
async function startUpload(f) {
  file.value = f
  step.value = 1
  percent.value = 0
  statusText.value = '准备中…'

  try {
    const id = await chunkUpload(f, {
      onProgress: (p, text) => {
        percent.value = p
        if (text) statusText.value = text
      },
      onStage: (stage) => {
        if (stage === 'done') statusText.value = '上传完成'
      }
    })
    fileId.value = id

    // 自动用文件名（去掉扩展名）填标题，省得用户再敲一遍
    form.title = f.name.replace(/\.[^.]+$/, '')
    step.value = 2
  } catch (e) {
    // 错误提示已由 request.js 的拦截器统一给出
    statusText.value = '上传失败：' + (e.message || '请重试')
    step.value = 0
    ElMessage.warning('上传失败，请重试')
  }
}

// ==================== 第三步：发布 ====================
async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (!props.courseId) {
    ElMessage.error('缺少课程信息，无法发布')
    return
  }

  publishing.value = true
  try {
    await publishResource({
      courseId: props.courseId,
      fileId: fileId.value,
      title: form.title,
      type: form.type,
      visibility: form.visibility,
      description: form.description
    })
    ElMessage.success('资料发布成功')
    emit('success')
    visible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    publishing.value = false
  }
}

// ==================== 重置 ====================
function reset() {
  step.value = 0
  file.value = null
  percent.value = 0
  statusText.value = ''
  fileId.value = null
  publishing.value = false
  dragging.value = false
  form.title = ''
  form.type = 'LECTURE'
  form.visibility = 'PUBLIC'
  form.description = ''
}

watch(visible, (v) => {
  if (!v) {
    // 关闭时重置（延迟一点，避免关闭动画中内容跳变）
    setTimeout(reset, 300)
  }
})
</script>

<style scoped>
.steps {
  margin-bottom: 26px;
}

.step-body {
  min-height: 240px;
}

/* =============================================================================
   第一步：拖拽区
   ============================================================================= */
.drop-zone {
  position: relative;
  overflow: hidden;
  border: 2px dashed var(--ch-border);
  border-radius: var(--ch-radius-lg);
  padding: 46px 24px;
  text-align: center;
  cursor: pointer;
  transition: var(--ch-transition);
  background: var(--ch-bg-muted);
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.05);
  transform: translateY(-2px);
}

.dz-glow {
  position: absolute;
  width: 220px;
  height: 220px;
  top: -120px;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 50%;
  background: rgba(139, 92, 246, 0.28);
  filter: blur(60px);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.drop-zone:hover .dz-glow,
.drop-zone.dragging .dz-glow {
  opacity: 1;
}

.dz-icon {
  position: relative;
  width: 68px;
  height: 68px;
  margin: 0 auto 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--ch-gradient);
  box-shadow: var(--ch-shadow-primary);
  transition: var(--ch-transition);
}

.drop-zone:hover .dz-icon,
.drop-zone.dragging .dz-icon {
  transform: scale(1.08) translateY(-2px);
}

.drop-main {
  position: relative;
  margin: 0 0 10px;
  font-size: 15.5px;
  font-weight: 600;
  color: var(--ch-text-primary);
}

.drop-tip {
  position: relative;
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
  line-height: 1.9;
}

/* =============================================================================
   第二步：上传中
   ============================================================================= */
.step-uploading {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
  padding-top: 10px;
}

.up-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
}

.up-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--ch-gradient);
  box-shadow: var(--ch-shadow-primary);
}

.up-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  margin: 0 0 4px;
  font-size: 14.5px;
  font-weight: 600;
}

.file-size {
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.up-percent {
  font-size: 20px;
  font-weight: 800;
  color: var(--ch-primary);
  letter-spacing: -0.02em;
}

.progress {
  margin: 0 2px;
}

.up-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0;
  font-size: 12.5px;
  line-height: 1.8;
  color: var(--ch-text-secondary);
}

/* =============================================================================
   第三步：填信息
   ============================================================================= */
.ok-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: var(--ch-radius);
  margin-bottom: 22px;
  color: #047857;
  background: rgba(16, 185, 129, 0.09);
  border: 1px solid rgba(16, 185, 129, 0.25);
}

.ok-banner strong {
  font-size: 14.5px;
}

.ok-banner p {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.info-form {
  margin-top: 4px;
}

.vis-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.vis-group :deep(.el-radio.is-bordered) {
  border-radius: 10px;
  margin-right: 0;
}

.radio-tip {
  margin: 8px 0 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
  line-height: 1.6;
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 600px) {
  .vis-group {
    flex-direction: column;
  }
}
</style>
