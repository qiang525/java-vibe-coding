<template>
  <el-dialog
    v-model="visible"
    title="上传资料"
    width="620px"
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
        <el-icon class="drop-icon"><UploadFilled /></el-icon>
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
    <div v-else-if="step === 1" class="step-body">
      <div class="uploading">
        <p class="file-name">
          <el-icon><Document /></el-icon>
          {{ file?.name }}
          <span class="file-size">{{ formatSize(file?.size) }}</span>
        </p>

        <el-progress
          :percentage="percent"
          :status="percent === 100 ? 'success' : undefined"
          :stroke-width="14"
          striped
          :striped-flow="percent < 100"
        />

        <p class="status-text">{{ statusText }}</p>
        <p class="drop-tip">
          上传过程中请勿关闭本窗口；中途失败也可以重新上传，已传的分片不会重传
        </p>
      </div>
    </div>

    <!-- ============ 第三步：填资料信息 ============ -->
    <div v-else class="step-body">
      <el-alert
        type="success"
        :closable="false"
        class="ok-alert"
        show-icon
      >
        文件已上传完成，请填写资料信息后发布
      </el-alert>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
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
          <el-radio-group v-model="form.visibility">
            <el-radio
              v-for="v in visibilityOptions"
              :key="v.value"
              :value="v.value"
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
        <el-button @click="visible = false">
          {{ step === 2 ? '取消' : '关闭' }}
        </el-button>
        <el-button
          v-if="step === 2"
          type="primary"
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
import { UploadFilled, Document } from '@element-plus/icons-vue'
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
  margin-bottom: 22px;
}

.step-body {
  min-height: 220px;
}

/* ---------- 拖拽区 ---------- */
.drop-zone {
  border: 2px dashed var(--ch-border);
  border-radius: var(--ch-radius-lg);
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafbfc;
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: var(--ch-primary);
  background: #f0f7ff;
}

.drop-icon {
  font-size: 46px;
  color: var(--ch-primary);
}

.drop-main {
  margin: 12px 0 8px;
  font-size: 15px;
  color: var(--ch-text-primary);
}

.drop-tip {
  margin: 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
  line-height: 1.8;
}

/* ---------- 上传中 ---------- */
.uploading {
  padding: 10px 4px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 14px;
  color: var(--ch-text-primary);
}

.file-size {
  color: var(--ch-text-secondary);
  font-size: 12px;
}

.status-text {
  margin: 12px 0 6px;
  font-size: 13px;
  color: var(--ch-primary);
}

/* ---------- 填信息 ---------- */
.ok-alert {
  margin-bottom: 18px;
}

.info-form {
  margin-top: 4px;
}

.radio-tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
  line-height: 1.6;
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
