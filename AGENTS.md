# EduAI · 安文AI教育 — 项目上下文



> 前后端分离，此文件为共享上下文。  
> 前端 Claude（VS Code）→ `web/`  
> 后端 Claude（IDEA）→ `D:\soft\Icode\EduAI-server`  
> **上下文变更后同步到两边**

---

## 一、项目定义

**安文AI教育** — 全学科 AI 智能学习平台。9 学科，3 角色（老师/学生/管理员）。

| 语 | 数 | 英 | 物 | 化 | 生 | 史 | 政 | 地 |
|----|----|----|----|----|----|----|----|----|
| 📝 | 📐 | 📖 | ⚛️ | 🧪 | 🧬 | 📜 | ⚖️ | 🌍 |

- **前端**：Vue 3 + Element Plus + Pinia + Vite · `http://localhost:5173`
- **后端**：Spring Boot 3.4.5 + JDK 21 + Maven 16模块 · `http://localhost:8080`
- **仓库**：`https://github.com/KHere-Z/EduAI` · 分支 `dev/20260630`
- **开发服务器**：当前运行在 `localhost:5173`（`npm run dev`）

## 二、进度

| # | 步骤 | 状态 |
|---|------|------|
| ① | 需求文档 & 技术栈 | ✅ `docs/spec/01-requirements.md` |
| ② | 项目架构设计 | ✅ `docs/spec/02-architecture.md` |
| ③ | 页面设计 & 前端编写 | ✅ `docs/spec/03-api-spec.md` |
| ④ | 数据库设计与建立 | ✅ MySQL 8.0 运行中 · users/teachers/organization 3表+测试数据 |
| ⑤ | 后端接口开发 | 🟡 认证登录完成（Sa-Token + BCrypt），前端联调代码就绪 |
| ⑥ | 配置 AI 服务（DeepSeek） | ⏳ |
| ⑦ | 部署上线 | ⏳ |

## 三、项目结构

```
EduAI/                              ← Git 仓库
├── web/                            ← 前端（VS Code 独立项目）
│   ├── src/
│   │   ├── views/
│   │   │   ├── teacher/            ← 老师端
│   │   │   │   ├── dashboard/             工作台
│   │   │   │   ├── english/               英语专属（课堂/词汇/AI阅读/AI口语/语法）
│   │   │   │   └── subject/               学科中心（9学科×9功能，动态路由）
│   │   │   ├── student/            ← 学生端
│   │   │   │   ├── dashboard/             学习中心
│   │   │   │   ├── wrongbook/             错题本
│   │   │   │   ├── analysis/              AI学情分析
│   │   │   │   ├── scores/               我的成绩
│   │   │   │   ├── practice/             智能练习
│   │   │   │   └── subject/              学科学习
│   │   │   ├── admin/              ← 管理员端
│   │   │   │   ├── dashboard/             管理概览
│   │   │   │   ├── teachers/             老师管理
│   │   │   │   ├── students/             学生管理
│   │   │   │   ├── subjects/             学科管理
│   │   │   │   └── resources/            词库/语法库/题库
│   │   │   ├── login/              登录页（三角色切换）
│   │   │   ├── register/           注册页
│   │   │   └── home/               安文AI教育品牌首页
│   │   ├── layouts/
│   │   │   ├── TeacherLayout.vue        老师侧边栏
│   │   │   ├── StudentLayout.vue        学生侧边栏
│   │   │   ├── AdminLayout.vue          管理侧边栏
│   │   │   └── DefaultLayout.vue
│   │   ├── router/index.js              路由（三角色）
│   │   ├── store/auth.js                Pinia认证
│   │   ├── api/                          Axios封装
│   │   └── assets/styles/global.css     设计系统（安文AI品牌色）
│   ├── tests/
│   │   └── test_frontend.py             Playwright自动化测试
│   └── vite.config.js
│
├── server/                          ← 后端 Maven 多模块（当前项目）
│   ├── pom.xml                      Spring Boot 3.4.5 父POM
│   ├── eduai-common/                公共模块（Result、BusinessException）
│   ├── eduai-security/              认证鉴权（Sa-Token + JPA）
│   ├── eduai-system/                启动模块（唯一可执行模块）
│   ├── eduai-ai/                    DeepSeek AI 客户端 [待开发]
│   ├── eduai-file/                  文件处理 / OCR [待开发]
│   ├── eduai-statistics/            成绩统计 [待开发]
│   ├── eduai-subject-common/        9学科共享代码 [待开发]
│   └── eduai-subject-{学科}/        9学科模块 [待开发]
│
├── docs/
│   ├── spec/                        需求 & 设计文档
│   │   ├── 01-requirements.md
│   │   ├── 02-architecture.md
│   │   ├── 03-api-spec.md
│   │   ├── 04-database-auth.md
│   │   └── 04-database-english.md
│   ├── sql/
│   │   └── init-database.sql        数据库初始化脚本
│   └── frontend-integration/        前端对接代码（复制到 web/ 项目）
│       ├── api/request.js
│       ├── api/auth.js
│       ├── store/auth.js
│       ├── router/guards.js
│       └── README.md
│
└── AGENTS.md                        ← 本文件
```

> **当前工作目录**: `D:\soft\Icode\EduAI-server`（后端）  
> **前端目录**: VS Code 打开 `EduAI/web/`

## 四、三角色

| 角色 | role_type | 路由 | 测试账号 |
|------|-----------|------|----------|
| 管理员 | 1 | `/admin/dashboard` | admin / admin123 |
| 老师 | 3 | `/teacher/dashboard` | coach(=数学+物理) / english(=英语) / math(=单科) / multi(=三科) |
| 学生 | 4 | `/student/dashboard` | 待前端实现注册流程 |

**老师端**拥有最完整的页面 —— 英语专属功能 + 9学科通用功能。  
**学生端**侧重学习工具 —— 错题本 / AI分析 / 成绩 / 练习。  
**管理员端**管后台 —— 老师/学生/学科/题库 CRUD。

## 五、每个学科 9 个通用功能

```
路由: /teacher/subject/{subject}/{feature}

wrong-questions   → 错题整理（表格+筛选+分页）
wrong-analysis    → AI错题分析（输入→根因→举一反三）
knowledge-points  → 知识点树（Tree+详情面板）
exam-points       → 考点整理（考频/分值/年份）
solution-models   → 解题模型（卡片+弹窗模板）
question-bank     → 题库（多条件筛选）
ai-feedback       → AI课堂反馈（4指标+诊断报告）
ai-analysis       → AI综合分析（雷达图+趋势+冲刺建议）
score-statistics  → 成绩统计（4核心指标+分布+明细）
```

## 六、老师端动态学科

每个老师有 `user.subjects` 数组（如 `['math','physics']`），侧边栏和 Dashboard 根据实际任教学科动态渲染：

- **单学科老师**（如 `math`）：学科中心只显示 1 个入口
- **多学科老师**（如 `['math','physics','chemistry']`）：显示多个
- **英语老师**：英语子菜单出现在侧边栏 + 快捷操作出现英语入口
- **非英语老师**：英语菜单全部隐藏

模拟登录时按用户名分配：`coach`→数学+物理，`math`→纯数学，`multi`→三科，`all`→全9科
后端实现时从 Teacher 表读取 subject_ids，AuthService 登录后注入 subjects 字段

## 七、英语学科（参照 airunword.com · 已移除蓝思值）

```
/teacher/classroom        课堂管理
/teacher/vocab-test       词汇测试
/teacher/word-memorize    单词记忆
/teacher/ai-reading       AI阅读理解
/teacher/ai-dialogue      AI情境口语
/teacher/grammar          语法体系
/teacher/sentence-practice 造句练习
/teacher/feedback         学习反馈
```

## 八、前后端约定

| 约定 | 值 |
|------|-----|
| 前端地址 | `localhost:5173`（Vite dev） |
| 后端地址 | `localhost:8080`（Spring Boot） |
| API 前缀 | `/api/v1/` |
| 认证 Header | `Authorization: Bearer <token>` |
| 响应格式 | `{ code, message, data }` |
| 分页 | `?page=1&pageSize=20` → `{ list, total, page, pageSize }` |

## 九、测试

| 工具 | 说明 |
|------|------|
| Playwright 1.61 + Chromium | 浏览器自动化 |
| webapp-testing 技能 | Claude Code 自动侦察+生成测试 |
| Playwright MCP | Claude Code 原生调用浏览器 |
| `web/tests/test_frontend.py` | 32页面全量测试脚本 |

运行：`PYTHONIOENCODING=utf-8 python web/tests/test_frontend.py`

## 十、关键架构决策

1. **动态学科路由** `/teacher/subject/:subject/*` 一个路由渲染 9 学科，避免 81 个页面
2. **spring-boot-maven-plugin 只在 eduai-system**，其他模块不打 fat jar
3. **前后端物理分离**：VS Code 只有 `web/`，IDEA 只有 `server/`

---

> 📝 **更新日志**  
> 2026-06-30: ①②步完成  
> 2026-06-30: ③前端完成 — 设计系统+登录页+Dashboard+9学科动态路由+9功能页+接口文档  
> 2026-06-30: 三角色重构（coach→teacher, platform→admin, 去掉agency, 新增student）  
> 2026-06-30: 用户认证DB设计 — users/teachers/organization 3表 + 登录API设计
> 2026-06-30: ④英语DB设计(14表) + 8个英语老师端页面完整实现
> 2026-06-30: 老师端按任教学科动态菜单 — user.subjects驱动侧边栏+英语条件显示
> 2026-06-30: Playwright + webapp-testing 测试体系安装，32页自动化测试通过
> 2026-06-30: MySQL 配置完成 — 3表(users/teachers/organization) + 5个测试账号
> 2026-06-30: 前后端登录联调 — 后端 Sa-Token+BCrypt 完成，前端对接代码就绪 docs/frontend-integration/
> 2026-06-30: 项目结构优化 — 清理16模块空占位目录，补全 .gitkeep，统一目录规范
