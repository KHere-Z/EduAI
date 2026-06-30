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

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, permissions, isLoggedIn, role, isTeacher, isStudent, isAdmin, login, logout, fetchUserInfo }
})
