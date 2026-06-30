/**
 * 认证状态管理 — Pinia Store
 *
 * 替代原有的 demoLogin() 模拟登录
 *
 * 用法：
 *   import { useAuthStore } from '@/store/auth'
 *   const auth = useAuthStore()
 *   await auth.login('coach', 'coach123')
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginAPI, getMeAPI, logoutAPI } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {

  // ==================== 状态 ====================
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // ==================== 计算属性 ====================
  const isLoggedIn = computed(() => !!token.value)
  const roleType = computed(() => user.value?.roleType)
  const isAdmin = computed(() => user.value?.roleType === 1)
  const isTeacher = computed(() => user.value?.roleType === 3)
  const isStudent = computed(() => user.value?.roleType === 4)
  /** 教师任教学科，如 ["math", "physics"] */
  const subjects = computed(() => user.value?.subjects || [])
  /** 该教师是否教英语 */
  const isEnglishTeacher = computed(() => subjects.value.includes('english'))

  // ==================== 方法 ====================

  /** 真实登录 */
  async function login(username, password) {
    const res = await loginAPI(username, password)
    // res.data = { user: {...}, token: "..." }
    const { user: userData, token: tokenStr } = res.data

    token.value = tokenStr
    user.value = userData

    // 持久化
    localStorage.setItem('token', tokenStr)
    localStorage.setItem('user', JSON.stringify(userData))

    return userData
  }

  /** 用已有 token 恢复会话（刷新页面时调用） */
  async function restoreSession() {
    if (!token.value) return null
    try {
      const res = await getMeAPI()
      user.value = res.data
      localStorage.setItem('user', JSON.stringify(res.data))
      return res.data
    } catch {
      // token 无效
      clearAuth()
      return null
    }
  }

  /** 退出登录 */
  async function logout() {
    try {
      await logoutAPI()
    } catch {
      // 即使后端调用失败也清除本地状态
    }
    clearAuth()
  }

  /** 清除本地认证状态 */
  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return {
    // 状态
    token, user,
    // 计算属性
    isLoggedIn, roleType, isAdmin, isTeacher, isStudent, subjects, isEnglishTeacher,
    // 方法
    login, restoreSession, logout, clearAuth
  }
})