<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">CH</div>
        <h2>CourseHub</h2>
        <p>课程资料协作平台</p>
      </div>

      <!-- 登录 / 注册 切换 -->
      <el-tabs v-model="mode" class="mode-tabs" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <!-- ==================== 登录表单 ==================== -->
      <el-form
        v-if="mode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入学号或用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>

        <p class="hint">
          测试账号：<strong>admin / admin123</strong>（管理员）
        </p>
      </el-form>

      <!-- ==================== 注册表单 ==================== -->
      <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="建议使用学号，如 2025210001"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="至少 6 位"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="registerForm.nickname"
            placeholder="别人看到的名字"
            clearable
          />
        </el-form-item>

        <el-form-item label="学院" prop="collegeId">
          <el-select
            v-model="registerForm.collegeId"
            placeholder="请选择所属学院"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="c in colleges"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注 册
        </el-button>
      </el-form>

      <el-button link type="primary" class="back-link" @click="goHome">
        ← 返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 登录 / 注册页
 *
 * 登录成功后：
 *   1. Token 存进 localStorage（由 store 完成）
 *   2. 用户信息存进 Pinia（全局共享）
 *   3. 跳转到来源页（没来源就跳首页）
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getColleges } from '@/api/college'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 当前模式：login / register */
const mode = ref('login')
const loading = ref(false)
const colleges = ref([])

// ==================== 登录 ====================
const loginFormRef = ref(null)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度为 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为 6-50 个字符', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    return // 校验未通过，Element Plus 已提示
  }
  loading.value = true
  try {
    await userStore.login({ ...loginForm })
    ElMessage.success('登录成功')
    // 有 redirect 参数就回来源页，否则回首页
    const redirect = route.query.redirect
    router.push(redirect ? String(redirect) : '/')
  } catch (e) {
    // 错误提示由 request.js 的响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

// ==================== 注册 ====================
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  collegeId: null
})
const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 50, message: '账号长度为 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为 6-50 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 50, message: '昵称最长 50 个字符', trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!registerFormRef.value) return
  try {
    await registerFormRef.value.validate()
  } catch (e) {
    return
  }
  loading.value = true
  try {
    await userStore.register({ ...registerForm })
    ElMessage.success('注册成功，请登录')
    // 切回登录页并带上刚注册的账号，省得用户再输一遍
    mode.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}

onMounted(async () => {
  // 加载学院下拉数据（注册用），失败也不阻塞页面
  try {
    colleges.value = await getColleges()
  } catch (e) {
    colleges.value = []
  }
})
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
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 32px 32px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.brand {
  text-align: center;
  margin-bottom: 18px;
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

.mode-tabs {
  margin-bottom: 8px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.hint {
  margin: 16px 0 8px;
  font-size: 12px;
  color: var(--ch-text-secondary);
  text-align: center;
  line-height: 1.7;
}

.hint strong {
  color: var(--ch-primary);
}

.back-link {
  display: block;
  margin: 8px auto 0;
}
</style>
