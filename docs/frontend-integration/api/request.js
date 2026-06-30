/**
 * Axios 封装 — 统一拦截器
 *
 * 用法：
 *   import request from '@/api/request'
 *   const res = await request.post('/auth/login', { username, password })
 *
 * 后端响应格式：{ code: 200, message: "success", data: {...} }
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'

// 后端地址（开发环境 Vite 代理到 localhost:8080）
const BASE_URL = '/api/v1'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// ---- 请求拦截器：自动带 Token ----
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---- 响应拦截器：统一错误处理 ----
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端返回非 200 的业务错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res  // { code: 200, message: "success", data: ... }
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        // Token 过期 / 未登录 → 回登录页
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('权限不足')
      } else {
        ElMessage.error(error.response.data?.message || '服务器异常')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request