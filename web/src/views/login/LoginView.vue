<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">安文</div>
        <h2>AI 智能教育平台</h2>
        <p>覆盖 9 大学科 · AI 驱动的个性化学习<br/>错题分析 · 举一反三 · 智能反馈</p>
        <div class="feature-list">
          <div class="feature-item" v-for="f in features" :key="f">
            <span class="feature-dot" />
            {{ f }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-panel">
      <div class="login-card">
        <h3>欢迎回来</h3>
        <p class="login-desc">登录您的账号继续使用</p>

        <div class="role-tabs">
          <button
            v-for="r in roles"
            :key="r.value"
            :class="['role-btn', { active: loginType === r.value }]"
            @click="loginType = r.value"
          >{{ r.label }}</button>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              placeholder="密码"
              type="password"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            native-type="submit"
            class="login-btn"
          >登 录</el-button>
        </el-form>

        <p class="register-link">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </div>
      <p class="copyright">© 2026 安文AI教育</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const loginType = ref('teacher')
const loading = ref(false)
const formRef = ref(null)

const features = ['9 大学科全覆盖', 'AI 错题分析与举一反三', '智能课堂反馈', '学情雷达图 & 趋势分析']

const roles = [
  { label: '老师', value: 'teacher' },
  { label: '学生', value: 'student' },
  { label: '管理', value: 'admin' }
]

const form = reactive({ username: 'coach', password: 'coach123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  // 模拟登录 — 后端未启动时走 demo 模式
  const roleMap = { teacher: 3, student: 4, admin: 1 }
  const roleType = roleMap[loginType.value] || 3
  authStore.demoLogin(form.username, roleType, loginType.value)
  const roleRoutes = { teacher: '/teacher/dashboard', student: '/student/dashboard', admin: '/admin/dashboard' }
  setTimeout(() => {
    router.push(roleRoutes[loginType.value] || '/')
    loading.value = false
  }, 400)
}
</script>

<style scoped>
.login-page { display: flex; min-height: 100vh; }

/* 左侧品牌 */
.brand-panel {
  flex: 1; background: linear-gradient(160deg, #1E1B4B 0%, #312E81 30%, #4338CA 70%, #6366F1 100%);
  display: flex; align-items: center; justify-content: center;
  padding: 60px; position: relative; overflow: hidden;
}
.brand-panel::before {
  content: ''; position: absolute;
  top: -50%; right: -30%; width: 600px; height: 600px;
  background: radial-gradient(circle, rgba(129,140,248,.15), transparent 70%);
  border-radius: 50%;
}
.brand-content { position: relative; z-index: 1; max-width: 420px; }
.brand-logo {
  font-size: 56px; font-weight: 800; color: #fff;
  letter-spacing: 12px; margin-bottom: 12px;
}
.brand-content h2 { color: #E0E7FF; font-size: 22px; font-weight: 500; margin-bottom: 12px; }
.brand-content > p { color: #A5B4FC; font-size: 14px; line-height: 1.8; margin-bottom: 32px; }
.feature-list { display: flex; flex-direction: column; gap: 12px; }
.feature-item { color: #C7D2FE; font-size: 14px; display: flex; align-items: center; gap: 10px; }
.feature-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #818CF8; box-shadow: 0 0 8px rgba(129,140,248,.5);
}

/* 右侧登录 */
.login-panel {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 60px;
  background: var(--color-bg);
}
.login-card { width: 100%; max-width: 400px; }
.login-card h3 { font-size: 28px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
.login-desc { color: var(--text-muted); font-size: 14px; margin-bottom: 28px; }

.role-tabs { display: flex; gap: 8px; margin-bottom: 28px; background: var(--color-bg-alt); padding: 4px; border-radius: var(--radius-sm); }
.role-btn {
  flex: 1; padding: 8px 16px; border: none; border-radius: var(--radius-sm);
  background: transparent; color: var(--text-secondary); font-size: 14px;
  cursor: pointer; transition: all var(--transition); font-family: inherit;
}
.role-btn.active { background: #fff; color: var(--color-primary); font-weight: 600; box-shadow: var(--shadow-sm); }
.role-btn:hover:not(.active) { color: var(--text-primary); }

.login-btn { width: 100%; height: 46px; font-size: 16px; letter-spacing: 4px; }
.register-link { text-align: center; margin-top: 20px; color: var(--text-muted); font-size: 14px; }
.copyright { color: var(--text-muted); font-size: 12px; margin-top: 40px; }
</style>
