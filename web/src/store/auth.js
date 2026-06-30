import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginAPI, getUserInfo } from '@/api/common/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const permissions = ref([])

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => user.value?.roleType)
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
    // 模拟登录 — 匹配数据库实际测试账号
    const fakeUsers = {
      'coach':   { name: '李老师', subjects: ['math','physics'] },
      'english': { name: '王老师', subjects: ['english'] },
      'math':    { name: '张老师', subjects: ['math'] },
      'multi':   { name: '陈老师', subjects: ['math','physics','chemistry'] },
      'admin':   { name: '系统管理员', subjects: ['math','physics','chemistry','biology','chinese','english','history','politics','geography'] }
    }
    const info = fakeUsers[username] || { name: username, subjects: ['math','physics'] }
    const subjects = info.subjects
    const fakeUser = { id: roleType * 100, username, realName: info.name, roleType, subjects }
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
