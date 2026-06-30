/**
 * 认证 API
 */
import request from './request'

/** 登录 */
export function loginAPI(username, password) {
  return request.post('/auth/login', { username, password })
}

/** 注册 */
export function registerAPI(data) {
  return request.post('/auth/register', data)
}

/** 获取当前用户信息 */
export function getMeAPI() {
  return request.get('/auth/me')
}

/** 退出登录 */
export function logoutAPI() {
  return request.post('/auth/logout')
}