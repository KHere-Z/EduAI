/**
 * 登录页对接指南
 *
 * 找到现有的 src/views/login/index.vue，将 demoLogin() 替换为真实登录：
 *
 * 1. 在 <script setup> 顶部加入：
 *    import { useAuthStore } from '@/store/auth'
 *    import { useRouter, useRoute } from 'vue-router'
 *
 * 2. 移除 demoLogin() 函数
 *
 * 3. 替换为真实登录方法：
 *
 * const auth = useAuthStore()
 * const router = useRouter()
 * const route = useRoute()
 *
 * const loading = ref(false)
 *
 * async function handleLogin() {
 *   if (!loginForm.value.username || !loginForm.value.password) {
 *     ElMessage.warning('请输入用户名和密码')
 *     return
 *   }
 *   loading.value = true
 *   try {
 *     const user = await auth.login(loginForm.value.username, loginForm.value.password)
 *     ElMessage.success(`欢迎回来，${user.realName || user.username}`)
 *
 *     // 按角色跳转
 *     const redirect = route.query.redirect || getDashboardPath(user.roleType)
 *     router.push(redirect)
 *   } catch (err) {
 *     // 错误已在 request 拦截器中处理（ElMessage.error）
 *     // 如需额外处理，例如清空密码框：
 *     // loginForm.value.password = ''
 *   } finally {
 *     loading.value = false
 *   }
 * }
 *
 * // 角色 → 首页
 * function getDashboardPath(roleType) {
 *   switch (roleType) {
 *     case 1: return '/admin/dashboard'
 *     case 3: return '/teacher/dashboard'
 *     case 4: return '/student/dashboard'
 *     default: return '/home'
 *   }
 * }
 *
 * 4. 模板中登录按钮绑定 @click="handleLogin" 并加 :loading="loading"
 */