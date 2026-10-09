<template>
  <el-dialog
    v-model="visible"
    title="上传新版本"
    width="600px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div class="cur-version">
      <el-icon><Share /></el-icon>
      当前版本为 <b>v{{ currentVersionNo || 1 }}</b>，上传后会生成
      <b>v{{ (currentVersionNo || 1) + 1 }}</b>，历史版本会完整保留。
    </div>

    <!-- 选文件 -->
    <div
      v-if="!file"
      class="drop-zone"
      :class="{ dragging }"
      @click="pickFile"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <div class="dz-icon">
        <el-icon :size="26"><UploadFilled /></el-icon>
      </div>
      <p class="drop-main">点击选择新版本文件，或拖到这里</p>
      <p class="drop-tip">支持 PDF / Word / PPT / Excel / 图片 / 压缩包，单个最大 500MB</p>
    </div>

    <!-- 已选文件 -->
    <div v-else class="picked">
      <div class="picked-card">
        <span class="picked-icon"><el-icon><Document /></el-icon></span>
        <div class="picked-info">
          <p class="picked-name ch-ellipsis">{{ file.name }}</p>
          <p class="picked-size">{{ formatSize(file.size) }}</p>
        </div>
        <el-button
          v-if="!uploading"
          link
          type="danger"
          @click="file = null"
        >
          重选
        </el-button>
      </div>

      <!-- 上传进度 -->
      <div v-if="uploading" class="progress-wrap">
        <el-progress
          :percentage="percent"
          :stroke-width="12"
          :show-text="false"
          striped
          :striped-flow="percent < 100"
        />
        <p class="progress-text">{{ percent }}% · {{ statusText }}</p>
      </div>

      <!-- 变更说明 -->
      <el-form v-else :model="form" label-position="top" class="log-form">
        <el-form-item label="变更说明">
          <el-input
            v-model="form.changeLog"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="如：修正了第三章的公式错误（选填）"
          />
        </el-form-item>
      </el-form>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      @change="onFileChange"
    />

    <template #footer>
      <div class="footer">
        <el-button round :disabled="uploading" @click="visible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          round
          :disabled="!file"
          :loading="uploading"
          @click="submit"
        >
          上传并生成新版本
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 新增版本对话框
 *
 * 和「上传资料」的区别：
 *   · 上传资料 = 文件 + 发布信息 → 产生一条新的资料记录
 *   · 新增版本 = 文件 → 挂到已有资料的版本树上（不产生新资料）
 *
 * 上传同样走分片接口（秒传 / 断点续传 / 合并哈希校验都是后端已有能力），
 * 拿到 fileId 后调 addVersion 生成版本。
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Share } from '@element-plus/icons-vue'
import { chunkUpload, formatSize } from '@/utils/upload'
import { addVersion } from '@/api/resource'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  resourceId: { type: [Number, String], default: null },
  /** 当前版本号，用于提示"将生成 v(n+1)" */
  currentVersionNo: { type: [Number, String], default: 1 }
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const fileInputRef = ref(null)
const dragging = ref(false)
const file = ref(null)
const uploading = ref(false)
const percent = ref(0)
const statusText = ref('')
const form = reactive({ changeLog: '' })

function pickFile() {
  fileInputRef.value?.click()
}

function onFileChange(e) {
  const f = e.target.files?.[0]
  if (f) file.value = f
  e.target.value = ''
}

function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) file.value = f
}

async function submit() {
  if (!file.value || !props.resourceId) return

  uploading.value = true
  percent.value = 0
  statusText.value = '准备中…'

  try {
    const fileId = await chunkUpload(file.value, {
      onProgress: (p, text) => {
        percent.value = p
        if (text) statusText.value = text
      }
    })

    await addVersion(props.resourceId, {
      fileId,
      changeLog: form.changeLog || '更新内容'
    })

    ElMessage.success('新版本已生成')
    emit('success')
    visible.value = false
  } catch (e) {
    ElMessage.warning('上传失败，请重试')
    uploading.value = false
  }
}

function reset() {
  file.value = null
  uploading.value = false
  percent.value = 0
  statusText.value = ''
  dragging.value = false
  form.changeLog = ''
}
</script>

<style scoped>
.cur-version {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 12px 16px;
  border-radius: var(--ch-radius);
  font-size: 13px;
  color: var(--ch-primary-dark);
  background: rgba(79, 70, 229, 0.07);
  border: 1px solid rgba(79, 70, 229, 0.18);
  margin-bottom: 18px;
}

/* ---------- 拖拽区 ---------- */
.drop-zone {
  border: 2px dashed var(--ch-border);
  border-radius: var(--ch-radius-lg);
  padding: 38px 22px;
  text-align: center;
  cursor: pointer;
  background: var(--ch-bg-muted);
  transition: var(--ch-transition);
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.05);
  transform: translateY(-2px);
}

.dz-icon {
  width: 58px;
  height: 58px;
  margin: 0 auto 14px;
  border-radius: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--ch-gradient);
  box-shadow: var(--ch-shadow-primary);
}

.drop-main {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}

.drop-tip {
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

/* ---------- 已选文件 ---------- */
.picked-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px 16px;
  border-radius: var(--ch-radius);
  background: var(--ch-bg-muted);
  border: 1px solid var(--ch-border-light);
  margin-bottom: 18px;
}

.picked-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: var(--ch-gradient);
  box-shadow: var(--ch-shadow-primary);
}

.picked-info {
  flex: 1;
  min-width: 0;
}

.picked-name {
  margin: 0 0 3px;
  font-size: 14px;
  font-weight: 600;
}

.picked-size {
  margin: 0;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.progress-wrap {
  padding: 4px 2px 8px;
}

.progress-text {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--ch-primary);
}

.log-form {
  margin-top: 2px;
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
