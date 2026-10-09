<template>
  <header class="site-header">
    <div class="ch-container header-inner">
      <!-- 左侧：品牌 -->
      <div class="brand" @click="router.push('/')">
        <div class="logo">CH</div>
        <div class="brand-text">
          <h1>CourseHub</h1>
          <p>让每一份资料都有出处</p>
        </div>
      </div>

      <!-- 右侧：导航 + 登录状态 -->
      <nav class="nav">
        <router-link to="/" class="nav-link" :class="{ active: active === 'home' }">
          首页
        </router-link>
        <router-link
          to="/courses"
          class="nav-link"
          :class="{ active: active === 'courses' }"
        >
          课程
        </router-link>
        <router-link
          to="/resources"
          class="nav-link"
          :class="{ active: active === 'resources' }"
        >
          资料库
        </router-link>

        <!-- 已登录：显示用户菜单 -->
        <el-dropdown v-if="userStore.isLoggedIn" @command="handleCommand">
          <span class="user-chip">
            <el-avatar :size="28" class="user-avatar">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ userStore.displayName }}</span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>角色：{{ roleText }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 未登录：显示登录按钮 -->
        <el-button v-else type="primary" @click="router.push('/login')">
          登录
        </el-button>
      </nav>
    </div>
  </header>
</template>

<script setup>
/**
 * 全站公用顶部导航栏
 *
 * 抽成组件的原因：首页和课程页都要用同一套导航，
 * 如果每个页面各写一份，改个链接就要改好几处，容易不一致。
 *
 * 登录状态直接读 Pinia，任意页面登录/退出后，这里会自动更新。
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

defineProps({
  /** 当前激活的导航项：home / courses */
  active: { type: String, default: '' }
})

const router = useRouter()
const userStore = useUserStore()

/** 头像里显示的第一个字母 */
const avatarText = computed(() => {
  const name = userStore.displayName
  return name ? name.charAt(0).toUpperCase() : 'U'
})

const roleText = computed(() => {
  const map = {
    ADMIN: '管理员',
    TEACHER: '教师',
    ASSISTANT: '助教',
    STUDENT: '学生'
  }
  return map[userStore.userInfo?.role] || '用户'
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}

onMounted(() => {
  // 刷新页面后 Pinia 是空的，但 Token 还在 —— 补一次用户信息
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchCurrentUser()
  }
})
</script>

<style scoped>
.site-header {
  background: #fff;
  border-bottom: 1px solid var(--ch-border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  height: var(--ch-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
}

.brand-text h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.2;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--ch-text-secondary);
}

.nav {
  display: flex;
  align-items: center;
  gap: 22px;
}

.nav-link {
  color: var(--ch-text-regular);
  font-size: 14px;
}

.nav-link.active,
.nav-link:hover {
  color: var(--ch-primary);
}

.nav-disabled {
  color: var(--ch-text-placeholder);
  cursor: not-allowed;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: var(--ch-primary);
  color: #fff;
  font-size: 13px;
}

.user-name {
  font-size: 14px;
  color: var(--ch-text-primary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  color: var(--ch-text-secondary);
  font-size: 12px;
}
</style>
