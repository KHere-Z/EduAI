import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginAPI, getUserInfo } from '@/api/common/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const permissions = ref([])

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.role_type)
  const isTeacher = computed(() => role.value === 3)
  const isStudent = computed(() => role.value === 4)
  const isAdmin = computed(() => role.value === 1)

  async function login(username, password) {
    const res = await loginAPI(username, password)
    token.value = res.token
    user.value = res.user
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify(res.user))
    return res
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    user.value = res
    localStorage.setItem('user', JSON.stringify(res))
  }

  // 模拟登录（后端未启动时使用）
  function demoLogin(username, roleType, roleName) {
    const fakeToken = 'demo-token-' + Date.now()
    // 每个老师有自己教的学科列表，按用户名分配不同组合方便测试
    const subjectMap = {
      'math': ['math'],
      'chinese': ['chinese'],
      'physics': ['physics'],
      'multi': ['math', 'physics', 'chemistry'],
      'all': ['math', 'physics', 'chemistry', 'biology', 'chinese', 'english', 'history', 'politics', 'geography']
    }
    const subjects = subjectMap[username] || ['math', 'physics']
    const fakeUser = { id: roleType * 100, username, real_name: username, role_type: roleType, role_name: roleName, org_id: 1, status: 1, subjects }
    token.value = fakeToken
    user.value = fakeUser
    localStorage.setItem('token', fakeToken)
    localStorage.setItem('user', JSON.stringify(fakeUser))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, permissions, isLoggedIn, role, isTeacher, isStudent, isAdmin, login, demoLogin, logout, fetchUserInfo }
})
