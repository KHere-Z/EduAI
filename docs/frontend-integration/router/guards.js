/**
 * 路由守卫 — 认证 & 角色控制
 *
 * 在 router/index.js 中引入：
 *   import { setupAuthGuard } from './guards'
 *   setupAuthGuard(router)
 */

import { useAuthStore } from '@/store/auth'

const ROLE_ROUTES = {
  1: '/admin',    // 管理员
  3: '/teacher',  // 教师
  4: '/student'   // 学生
}

/** 白名单：无需登录即可访问 */
const WHITE_LIST = ['/login', '/register', '/home', '/404']

export function setupAuthGuard(router) {
  router.beforeEach(async (to, from, next) => {
    const auth = useAuthStore()

    // 白名单 → 放行
    if (WHITE_LIST.some(path => to.path.startsWith(path))) {
      return next()
    }

    // 未登录 → 跳转登录页
    if (!auth.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // 已登录但 user 信息丢失 → 恢复会话
    if (!auth.user) {
      await auth.restoreSession()
    }

    // 角色路由保护：教师不能访问 /admin，管理员不能访问 /student 等
    const roleType = auth.roleType
    const targetPrefix = ROLE_ROUTES[roleType]
    if (targetPrefix && !to.path.startsWith(targetPrefix) && !to.path.startsWith('/home')) {
      // 跨角色访问 → 跳回自己的 Dashboard
      return next(targetPrefix + '/dashboard')
    }

    next()
  })
}