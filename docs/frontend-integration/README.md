# 前端-后端联调指南

## 一、后端已就绪

启动后端（IntelliJ IDEA 运行 `EduAIApplication`），以下接口可用：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 登录 |
| POST | `/api/v1/auth/register` | 注册 |
| GET | `/api/v1/auth/me` | 获取当前用户 |
| POST | `/api/v1/auth/logout` | 退出登录 |

API 文档：启动后端后访问 `http://localhost:8080/doc.html`

---

## 二、前端对接步骤

### 1. 复制文件到前端项目

```
web/src/
├── api/
│   ├── request.js    ← 复制 frontend-integration/api/request.js
│   └── auth.js       ← 复制 frontend-integration/api/auth.js
├── store/
│   └── auth.js       ← 用 frontend-integration/store/auth.js 替换
└── router/
    └── guards.js     ← 复制 frontend-integration/router/guards.js
```

### 2. Vite 代理配置

在 `vite.config.js` 中添加：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 3. 登录页改造

找到 `src/views/login/index.vue`：

- **删除** `demoLogin()` 函数
- **加入** 真实登录逻辑（参考 `views/login-integration.js`）

核心代码：
```js
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()

async function handleLogin() {
  const user = await auth.login(loginForm.username, loginForm.password)
  // user.subjects = ["math", "physics"] ← 后端返回，替代前端硬编码
  router.push(getDashboardPath(user.roleType))
}
```

### 4. 路由守卫

在 `router/index.js` 中加入：
```js
import { setupAuthGuard } from './guards'
setupAuthGuard(router)
```

### 5. 侧边栏改造

原来 `demoLogin()` 里硬编码 `subjectMap`。现在从 Pinia 读取：

```js
// 旧代码（删除）
// const subjectMap = { coach: ['math','physics'], ... }
// subjects = subjectMap[username]

// 新代码
import { useAuthStore } from '@/store/auth'
const auth = useAuthStore()
const subjects = auth.subjects  // ← 后端登录时返回
const isEnglishTeacher = auth.isEnglishTeacher
```

---

## 三、测试账号

| 用户名 | 密码 | 角色 | 任教学科 |
|--------|------|------|----------|
| admin | admin123 | 管理员 | — |
| coach | coach123 | 教师 | 数学、物理 |
| english | english123 | 教师 | 英语 |
| math | math123 | 教师 | 数学 |
| multi | multi123 | 教师 | 数学、物理、化学 |

---

## 四、curl 测试（后端 API 验证）

```bash
# 1. 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coach","password":"coach123"}'

# 响应示例:
# {
#   "code": 200,
#   "message": "success",
#   "data": {
#     "user": {
#       "id": 2,
#       "username": "coach",
#       "realName": "李老师",
#       "roleType": 3,
#       "status": 1,
#       "subjects": ["math", "physics"],
#       "orgId": 1,
#       "orgName": "第一实验中学",
#       "title": "高级教师"
#     },
#     "token": "eyJh..."
#   }
# }

# 2. 获取当前用户（替换 YOUR_TOKEN）
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 退出
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 五、数据流

```
登录页 → POST /api/v1/auth/login { username, password }
       ↓
后端验证 BCrypt 密码 → Sa-Token 签发 token
       ↓                        ↓
  查询 teachers 表        StpUtil.login(id)
  subject_ids → subjects[]
       ↓
返回 { user: {..., subjects: ["math","physics"] }, token }
       ↓
前端 Pinia store:
  token → localStorage
  user  → localStorage + 响应式
       ↓
侧边栏: auth.subjects → 过滤学科菜单
路由守卫: auth.roleType → 角色权限拦截
```