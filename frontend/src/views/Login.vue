<template>
  <div class="login-page">
    <!-- ============================================================
         左侧品牌介绍面板（小屏隐藏）
         ============================================================ -->
    <aside class="brand-panel">
      <span class="blob blob-a"></span>
      <span class="blob blob-b"></span>
      <span class="grid-overlay"></span>

      <div class="bp-inner">
        <div class="bp-brand" @click="goHome">
          <span class="bp-logo">CH</span>
          <div>
            <h2>CourseHub</h2>
            <p>课程资料协作平台</p>
          </div>
        </div>

        <div class="bp-copy">
          <h1>让每一份资料<br />都有出处</h1>
          <p class="bp-sub">
            按学院、学期、课程、章节四级组织资料，用可信度评分与版本树，
            让课程资料真正沉淀下来。
          </p>

          <ul class="bp-list">
            <li v-for="f in panelFeatures" :key="f.text">
              <span class="bp-ic"><el-icon><component :is="f.icon" /></el-icon></span>
              {{ f.text }}
            </li>
          </ul>
        </div>

        <p class="bp-foot">南京信息工程大学 · 25 级课程实践项目</p>
      </div>
    </aside>

    <!-- ============================================================
         右侧表单区
         ============================================================ -->
    <main class="form-panel">
      <div class="form-card">
        <div class="fc-head">
          <h2>{{ mode === 'login' ? '欢迎回来 👋' : '创建账号' }}</h2>
          <p>{{ mode === 'login' ? '登录后即可加入课程、上传与下载资料' : '填写基本信息，开始使用 CourseHub' }}</p>
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

          <div class="demo-hint">
            <el-icon><InfoFilled /></el-icon>
            测试账号：<strong>admin</strong> / <strong>admin123</strong>
            <el-button link type="primary" size="small" @click="fillDemo">一键填入</el-button>
          </div>
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
              :prefix-icon="Postcard"
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

        <el-button link class="back-link" @click="goHome">
          <el-icon><Back /></el-icon> 返回首页
        </el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
/**
 * 登录 / 注册页
 *
 * 采用「左右分屏」布局：
 *   · 左侧 品牌介绍面板 —— 用渐变与光晕建立第一印象
 *   · 右侧 表单卡片    —— 专注完成登录/注册动作
 * 窄屏（<900px）自动隐藏左侧面板，只保留表单。
 *
 * 登录成功后：
 *   1. Token 存进 localStorage（由 store 完成）
 *   2. 用户信息存进 Pinia（全局共享）
 *   3. 跳转到来源页（没来源就跳首页）
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, Postcard, Back, InfoFilled,
  Search, Share, UploadFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getColleges } from '@/api/college'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 当前模式：login / register */
const mode = ref('login')
const loading = ref(false)
const colleges = ref([])

const panelFeatures = [
  { icon: Search, text: '中文全文检索，秒级定位资料' },
  { icon: UploadFilled, text: '大文件分片上传，支持断点续传' },
  { icon: Share, text: '版本树留痕，多人协作不覆盖' }
]

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

function fillDemo() {
  loginForm.username = 'admin'
  loginForm.password = 'admin123'
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
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: var(--ch-bg-page);
}

/* =============================================================================
   左侧品牌面板
   ============================================================================= */
.brand-panel {
  position: relative;
  overflow: hidden;
  background: var(--ch-gradient-hero);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 56px;
}

.blob-a {
  width: 360px;
  height: 360px;
  top: -100px;
  right: -80px;
  background: rgba(168, 85, 247, 0.55);
}

.blob-b {
  width: 300px;
  height: 300px;
  bottom: -120px;
  left: -70px;
  background: rgba(56, 189, 248, 0.45);
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
  animation: ch-float 10s ease-in-out infinite;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at 25% 25%, #000 0%, transparent 70%);
  -webkit-mask-image: radial-gradient(circle at 25% 25%, #000 0%, transparent 70%);
  pointer-events: none;
}

.bp-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.bp-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.bp-logo {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
}

.bp-brand h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.bp-brand p {
  margin: 2px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

.bp-copy h1 {
  margin: 0 0 18px;
  font-size: 40px;
  line-height: 1.25;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
}

.bp-sub {
  margin: 0 0 28px;
  font-size: 14.5px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.72);
  max-width: 420px;
}

.bp-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bp-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.bp-ic {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #c4b5fd;
  flex-shrink: 0;
}

.bp-foot {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* =============================================================================
   右侧表单
   ============================================================================= */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.form-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 22px;
  padding: 36px 34px 26px;
  box-shadow: var(--ch-shadow-lg);
  border: 1px solid var(--ch-border-light);
  animation: ch-fade-up 0.55s var(--ch-ease) both;
}

.fc-head h2 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.fc-head p {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--ch-text-secondary);
  line-height: 1.7;
}

.mode-tabs {
  margin-bottom: 6px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 16px;
  padding: 10px 14px;
  border-radius: 11px;
  font-size: 12.5px;
  color: var(--ch-text-secondary);
  background: var(--ch-bg-muted);
  border: 1px dashed var(--ch-border);
}

.demo-hint strong {
  color: var(--ch-primary);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  margin: 14px auto 0;
  color: var(--ch-text-secondary);
}

.back-link:hover {
  color: var(--ch-primary);
}

/* =============================================================================
   响应式
   ============================================================================= */
@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-card {
    padding: 30px 24px 22px;
  }
}
</style>
