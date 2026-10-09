<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">CH</div>
        <h2>CourseHub</h2>
        <p>课程资料协作平台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入学号或用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          @click="handleSubmit"
        >
          登 录
        </el-button>
      </el-form>

      <p class="hint">
        登录功能将在 <strong>M1 阶段</strong> 接入，当前为页面框架预览。
      </p>

      <el-button link type="primary" @click="goHome">← 返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录页（框架版）
 *
 * 说明：本阶段只完成页面结构与表单校验，
 * 实际的登录接口调用会在 M1（用户与课程）阶段接入。
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度为 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为 6-50 个字符', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    // TODO(M1): 调用 POST /api/auth/login
    ElMessage.info('登录接口将在 M1 阶段接入')
  } catch (e) {
    // 校验失败，Element Plus 会自动提示
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f5f7fa 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.brand .logo {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  font-size: 19px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.brand p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--ch-text-secondary);
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.hint {
  margin: 18px 0 12px;
  font-size: 12px;
  color: var(--ch-text-secondary);
  text-align: center;
  line-height: 1.7;
}
</style>
