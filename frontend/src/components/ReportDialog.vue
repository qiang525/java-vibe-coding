<template>
  <el-dialog
    v-model="visible"
    title="举报"
    width="560px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <!-- 举报对象提示 -->
    <div class="target-box">
      <span class="target-icon"><el-icon><Warning /></el-icon></span>
      <div>
        <p class="target-type">举报{{ targetLabel }}</p>
        <p class="target-name ch-ellipsis">{{ targetName }}</p>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="report-form"
    >
      <el-form-item label="举报原因" prop="reason">
        <el-radio-group v-model="form.reason" class="reason-group">
          <el-radio
            v-for="r in reasons"
            :key="r.value"
            :value="r.value"
            border
          >
            {{ r.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="补充说明" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="简单说明问题所在，方便管理员核实（选填）"
        />
      </el-form-item>
    </el-form>

    <div class="tip">
      <el-icon><InfoFilled /></el-icon>
      同一对象在管理员处理前只能举报一次，请如实填写。
    </div>

    <template #footer>
      <div class="footer">
        <el-button round @click="visible = false">取消</el-button>
        <el-button type="primary" round :loading="submitting" @click="submit">
          提交举报
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 举报对话框（可复用）
 *
 * 因为举报是「多态」的（可以举报资料、评论、用户），
 * 所以这里通过 props 传入 targetType / targetId，
 * 任何地方想加举报入口，挂上这个组件即可。
 */
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, InfoFilled } from '@element-plus/icons-vue'
import { submitReport, REPORT_REASONS } from '@/api/report'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 举报对象类型：RESOURCE / COMMENT / USER */
  targetType: { type: String, default: 'RESOURCE' },
  /** 举报对象 ID */
  targetId: { type: [Number, String], default: null },
  /** 展示用的对象名字（资料标题 / 评论摘要等） */
  targetName: { type: String, default: '' },
  /** 对象类型的中文名，不传则按 targetType 推导 */
  targetLabel: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const reasons = REPORT_REASONS

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({
  reason: 'COPYRIGHT',
  description: ''
})

const rules = {
  reason: [{ required: true, message: '请选择举报原因', trigger: 'change' }]
}

async function submit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  if (!props.targetId) {
    ElMessage.error('缺少举报对象')
    return
  }

  submitting.value = true
  try {
    await submitReport({
      targetType: props.targetType,
      targetId: Number(props.targetId),
      reason: form.reason,
      description: form.description
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    emit('success')
    visible.value = false
  } catch (e) {
    // 错误提示由 request.js 拦截器统一处理（含 40902 重复举报）
  } finally {
    submitting.value = false
  }
}

function reset() {
  form.reason = 'COPYRIGHT'
  form.description = ''
  submitting.value = false
}
</script>

<style scoped>
.target-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--ch-radius);
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.25);
  margin-bottom: 20px;
}

.target-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  box-shadow: 0 8px 18px rgba(245, 158, 11, 0.3);
}

.target-type {
  margin: 0 0 3px;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.target-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  max-width: 400px;
}

.report-form {
  margin-top: 4px;
}

.reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reason-group :deep(.el-radio.is-bordered) {
  margin-right: 0;
  border-radius: 10px;
}

.tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
}

.footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
