<template>
  <header class="site-header ch-glass" :class="{ scrolled }">
    <div class="ch-container header-inner">
      <!-- 左侧：品牌 -->
      <div class="brand" @click="router.push('/')">
        <span class="logo">
          <svg viewBox="0 0 32 32" width="22" height="22" aria-hidden="true">
            <path
              d="M16 3.5 4.5 9v7.2c0 6.1 4.6 10.6 11.5 12.3 6.9-1.7 11.5-6.2 11.5-12.3V9L16 3.5Z"
              fill="rgba(255,255,255,.22)"
            />
            <path
              d="M16 8.6 9 12v4.3c0 3.6 2.8 6.3 7 7.4 4.2-1.1 7-3.8 7-7.4V12l-7-3.4Z"
              fill="#fff"
              fill-opacity=".95"
            />
            <path
              d="M12.4 15.9h7.2M12.4 18.6h4.6"
              stroke="#6d28d9"
              stroke-width="1.5"
              stroke-linecap="round"
            />
          </svg>
        </span>
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
        <router-link to="/courses" class="nav-link" :class="{ active: active === 'courses' }">
          课程库
        </router-link>
        <router-link to="/resources" class="nav-link" :class="{ active: active === 'resources' }">
          资料库
        </router-link>

        <!-- 已登录：显示用户菜单 -->
        <el-dropdown v-if="userStore.isLoggedIn" @command="handleCommand" trigger="click">
          <span class="user-chip">
            <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
            <span class="user-meta">
              <span class="user-name">{{ userStore.displayName }}</span>
              <span class="user-role">{{ roleText }}</span>
            </span>
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                <el-icon><User /></el-icon> {{ userStore.userInfo?.username }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 未登录：显示登录按钮 -->
        <el-button v-else type="primary" round @click="router.push('/login')">
          登录 / 注册
        </el-button>
      </nav>
    </div>
  </header>
</template>

<script setup>
/**
 * 全站公用顶部导航栏
 *
 * 抽成组件的原因：所有页面都用同一套导航，改一处即可全站生效。
 * 登录状态直接读 Pinia，任意页面登录/退出后这里会自动更新。
 *
 * 交互细节：
 *   · 采用毛玻璃（backdrop-filter）效果，滚动时内容从下方"透过去"
 *   · 页面下滚超过 8px 时加一层阴影，让导航与内容区分开
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

defineProps({
  /** 当前激活的导航项：home / courses / resources */
  active: { type: String, default: '' }
})

const router = useRouter()
const userStore = useUserStore()

/** 页面是否已向下滚动（用于给导航加阴影） */
const scrolled = ref(false)

function onScroll() {
  scrolled.value = window.scrollY > 8
}

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
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  // 刷新页面后 Pinia 是空的，但 Token 还在 —— 补一次用户信息
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchCurrentUser()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  transition: box-shadow 0.3s var(--ch-ease), background 0.3s var(--ch-ease);
}

.site-header.scrolled {
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.07);
  background: rgba(255, 255, 255, 0.86);
}

.header-inner {
  height: var(--ch-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* ---------- 品牌 ---------- */
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.logo {
  width: 42px;
  height: 42px;
  border-radius: 13px;
  background: var(--ch-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--ch-shadow-primary);
  transition: var(--ch-transition);
}

.brand:hover .logo {
  transform: rotate(-6deg) scale(1.05);
}

.brand-text h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.brand-text p {
  margin: 2px 0 0;
  font-size: 11.5px;
  color: var(--ch-text-secondary);
}

/* ---------- 导航 ---------- */
.nav {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-link {
  position: relative;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--ch-text-regular);
  transition: var(--ch-transition);
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 2px;
  height: 2px;
  border-radius: 2px;
  background: var(--ch-gradient);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s var(--ch-ease);
}

.nav-link:hover {
  color: var(--ch-primary);
  background: rgba(79, 70, 229, 0.07);
}

.nav-link.active {
  color: var(--ch-primary);
}

.nav-link.active::after {
  transform: scaleX(1);
}

/* ---------- 用户菜单 ---------- */
.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-left: 8px;
  padding: 5px 12px 5px 5px;
  border-radius: 999px;
  cursor: pointer;
  outline: none;
  border: 1px solid var(--ch-border-light);
  background: rgba(255, 255, 255, 0.6);
  transition: var(--ch-transition);
}

.user-chip:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: var(--ch-shadow-sm);
}

.user-avatar {
  background: var(--ch-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ch-text-primary);
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  font-size: 11px;
  color: var(--ch-text-secondary);
}

.arrow {
  color: var(--ch-text-secondary);
  font-size: 12px;
}

/* ---------- 小屏适配 ---------- */
@media (max-width: 720px) {
  .brand-text p {
    display: none;
  }

  .user-meta {
    display: none;
  }

  .nav {
    gap: 0;
  }

  .nav-link {
    padding: 8px 9px;
    font-size: 13px;
  }
}
</style>
