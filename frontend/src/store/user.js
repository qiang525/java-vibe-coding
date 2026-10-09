/**
 * 用户状态管理（Pinia）
 *
 * ============================================================
 * 为什么需要状态管理？
 * ------------------------------------------------------------
 * 登录状态需要被很多页面用到：导航栏要显示用户名、课程页要判断
 * 有没有权限创建课程、退出时要清空。如果每个页面各自用 localStorage
 * 读一遍，就会出现"这个页面已退出、那个页面还显示着用户名"的不一致。
 *
 * Pinia 提供了一份「全局共享、响应式」的状态：一处修改，所有页面同步更新。
 * ============================================================
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { getToken, setToken, removeToken } from '@/api/request'

export const useUserStore = defineStore('user', () => {
  /** 当前用户信息，未登录时为 null */
  const userInfo = ref(null)

  /** 是否已登录（看本地有没有 Token） */
  const isLoggedIn = computed(() => !!getToken())

  /** 是否管理员 */
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  /** 昵称，没登录时兜底显示"未登录" */
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || ''
  )

  /**
   * 登录：调用接口 → 保存 Token → 保存用户信息
   */
  async function login(form) {
    const data = await authApi.login(form)
    setToken(data.token)
    userInfo.value = data.user
    return data
  }

  /**
   * 注册
   */
  async function register(form) {
    return authApi.register(form)
  }

  /**
   * 拉取当前用户信息（刷新页面后恢复登录态用）
   */
  async function fetchCurrentUser() {
    if (!getToken()) {
      return null
    }
    try {
      userInfo.value = await authApi.getCurrentUser()
      return userInfo.value
    } catch (e) {
      // Token 失效，就当未登录处理（拦截器已经会跳登录页）
      userInfo.value = null
      return null
    }
  }

  /**
   * 退出登录：通知后端 + 清除本地 Token 与用户信息
   */
  function logout() {
    // 后端接口失败也不影响本地退出，所以 catch 掉不阻塞
    authApi.logout().catch(() => {})
    removeToken()
    userInfo.value = null
  }

  return {
    userInfo,
    isLoggedIn,
    isAdmin,
    displayName,
    login,
    register,
    fetchCurrentUser,
    logout
  }
})
