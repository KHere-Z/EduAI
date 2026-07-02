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
- **前端仓库**：`https://github.com/KHere-Z/EduAIFront`
- **后端仓库**：`https://github.com/KHere-Z/EduAI`
- **开发服务器**：当前运行在 `localhost:5173`（`npm run dev`）

## 二、进度

| # | 步骤 | 状态 |
|---|------|------|
| ① | 需求文档 & 技术栈 | ✅ `docs/spec/01-requirements.md` |
| ② | 项目架构设计 | ✅ `docs/spec/02-architecture.md` |
| ③ | 页面设计 & 前端编写 | ✅ `docs/spec/03-api-spec.md` |
| ④ | 数据库设计与建立 | 🟢 认证3表✅ · 英语14表✅ · 学生管理4表✅（v2: students/teacher_student/enrollment/session） |
| ⑤ | 后端接口开发 | 🟢 登录API✅ · 学生CRUD 全7接口✅（v2: 通过 teacher_student 关系表） |
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
│   │   └── 05-database-student.md     学生管理DB(3表+API)
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

英语已整合到学科中心，与其他学科并列。点击英语进入英语学科首页。

### 英语功能页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 英语首页 | `/teacher/english/home` | Dashboard + 8功能入口 + 单词进度概览 |
| 课堂管理 | `/teacher/classroom` | 创建班级/排课/学生记录 |
| 词汇测试 | `/teacher/vocab-test` | 选词库→组卷→历史 |
| 单词记忆 | `/teacher/word-memorize` | 学词→复习→艾宾浩斯 |
| AI 阅读理解 | `/teacher/ai-reading` | 文章→答题→AI批改 |
| AI 情境口语 | `/teacher/ai-dialogue` | 场景→对话→AI评测 |
| 语法体系 | `/teacher/grammar` | 分类·手风琴·公式 |
| 造句练习 | `/teacher/sentence-practice` | 选语法→造句→批改 |
| 学习反馈 | `/teacher/feedback` | 强弱项·AI建议·周报 |
| 单词进度 | `/teacher/english/word-progress` | **NEW** 学员单词掌握率/打卡/明细 |

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
> 2026-07-02: 英语整合入学���中心 + 英语首页(EnglishHome) + 学员单词学习进度(WordProgress)
> 2026-07-02: 学生信息管理DB设计(students/enrollment/session 3表) + 完整API接口文档
> 2026-07-02: 学生管理页全功能 — 批量排课/课程完成标记/调课/学生选择器/真实数据对接
> 2026-06-30: MySQL 配置完成 — 3表(users/teachers/organization) + 5个测试账号
> 2026-06-30: 前后端登录联调 — 后端 Sa-Token 完成（明文密码比对），前端对接代码就绪 docs/frontend-integration/
> 2026-06-30: 项目结构优化 — 清理16模块空占位目录，补全 .gitkeep，统一目录规范
> 2026-07-02: 学生管理后端完成 — students/enrollment/session 3表 + 7个API端点(含日历)
> 2026-07-02: 修复 POST 500 — Entity 改为双向映射(@ManyToOne)，解决 Hibernate INSERT 缺 FK 问题
> 2026-07-02: 学生管理前端交互完善 — 排课按钮/课时联动/课程完成/清空排课归零
> 2026-07-02: 修复 GlobalExceptionHandler — 改用 ResponseEntity 确保 401/403 正确返回到前端
> 2026-07-02: 修复请求编码 — 加 server.servlet.encoding.force=true，强制 UTF-8 解码（前端 GBK→UTF-8）
> 2026-07-02: **v2 重构** — students 精简为全局档案 + 新增 teacher_student 关系表 + enrollment FK 改为指向 teacher_student
> 2026-07-02: v2 修复: POST 不再重复创建 Student（按姓名+学校去重），同一学生可被多个老师添加不冲突
> 2026-07-02: ✅ v2 数据库迁移完成 — 4表(students/teacher_student/enrollment/session) + 测试数据已就绪

---

## 十一、学生管理 API（前端对接）
n### 前端交互逻辑

- **排课按钮**: 下拉选择已有学生 → 选科目 → 日历点日期 → 填时间段 → 确认
- **课时联动**: 排课保存后设置初始课时 · 点✅完成自动-1 · 清空排课归零
- **清空排课**: PUT enrollments:[] + hoursLeft:0，保留 teacher_student 关系
- **课程完成**: 点击✅ → UI变蓝 · 当日全部完成→日历提前变绿
- **调课**: 📝按钮 → 弹窗选新日期+时间段
- **删除**: 不再支持DELETE，仅清空排课（保留学生关系）

> **后端状态**: ✅ 已实现 (v2) · 数据库已迁移 · **前端页面**: `/teacher/students`（就绪）  
> **数据库 v2**: students(全局档案) / teacher_student(关系+课时) / student_enrollment / student_session  
> **测试**: allsub(6/赵老师) 登录 → GET/POST/DELETE 全部可用，已预置张顺仪(英语15次+数学15次)  

### v2 行为变化（前端需知）

| 行为 | v1（旧） | v2（新） |
|------|----------|----------|
| 添加学生 | 每次 POST 都创建新 Student 行 | 按姓名+学校查找已有 Student，不存在才创建；然后创建 teacher_student 关系 |
| 重复添加 | 同一老师可重复添加同一学生（产生多条 students 记录） | 同一老师对同一学生只能添加一次，重复添加返回 **400 "该学生已在您的列表中"** |
| 多老师共享 | 不同老师各自存一份 Student | 不同老师共享同一个 Student 档案，各自维护独立的课时和科目 |
| 响应中的 id | student.id | **teacher_student.id**（关系表主键） |
| 课时 | students.hours_left（学生维度） | teacher_student.hours_left（每个老师-学生组合独立） |

### 前端适配检查清单

> **好消息：API 请求/响应格式完全不变，大概率不需要改代码。**  
> 建议逐项验证：

1. **添加学生** — 正常流程不变，新行为：同一老师重复添加同一姓名+学校的学生时返回 400，前端应展示错误提示
2. **学生列表** — 响应中 `id` 现在是 teacher_student 的 ID，如果前端用这个 id 做 key 或传给 DELETE/PATCH，继续正常工作
3. **课时加减** — `PATCH /{id}/hours` 操作的 id 是 teacher_student.id，行为不变
4. **删除学生** — `DELETE /{id}` 删除的是 teacher_student 关系（不影响 Student 档案本身和其他老师的该学生记录）
5. **学生选择器** — 如果要搜索"系统中所有学生"（跨老师），需新增接口（目前 GET 只返回当前老师的学生）。如需此功能请联系后端加接口
6. **日历** — 不变，仍按 teacherId + 月份查询

### API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/students` | 分页列表（keyword/grade/subject 筛选） |
| POST | `/api/v1/students` | 新增学生（含科目+排课嵌套） |
| GET | `/api/v1/students/{id}` | 学生详情 |
| PUT | `/api/v1/students/{id}` | 更新学生（先删后插 enrollments） |
| DELETE | `/api/v1/students/{id}` | 删除学生（级联删除科目和排课） |
| PATCH | `/api/v1/students/{id}/hours` | 课时加减 `{ "delta": -1 }` |
| GET | `/api/v1/students/calendar` | 日历排课 `?year=2026&month=7&teacherId=` |

> 全部需要登录（Sa-Token），学生按老师隔离

### 请求/响应格式

**POST / PUT 请求体**（`application/json`）：
```json
{
  "name": "张三",
  "gender": "男",
  "contact": "13800001111",
  "hoursLeft": 15,
  "grade": "初一",
  "school": "第一实验中学",
  "regDate": "2026-03-15",
  "enrollments": [
    {
      "subject": "英语",
      "sessions": [
        { "classDate": "2026-07-03", "startTime": "14", "endTime": "16" },
        { "classDate": "2026-07-05", "startTime": "14", "endTime": "16" }
      ]
    }
  ]
}
```

**GET 列表响应**（`data` 内）：
```json
{
  "list": [{ "id": 1, "name": "张三", ..., "enrollments": [...] }],
  "total": 28, "page": 1, "pageSize": 20
}
```

**PATCH 课时**：`{ "delta": -1 }`（上课-1，充值+N）

**GET 日历响应**：
```json
{
  "dates": {
    "2026-07-03": [
      { "studentId": 1, "studentName": "张三", "subject": "英语", "startTime": "14", "endTime": "16" }
    ]
  }
}
```

### 年级可选值

`一年级` `二年级` `三年级` `四年级` `五年级` `六年级` `初一` `初二` `初三` `高一` `高二` `高三`

### 测试数据

以 **allsub**（id=6，密码 allsub123）登录后调用 `GET /api/v1/students` 即可看到 1 条示例数据：
- 张顺仪，男，初一，夏港中学，剩余 15 课时，报了英语(14次排课) + 数学(1次排课)

以 **coach**（id=2，密码 coach123）登录看到空列表（张顺仪属于 allsub），需自己 POST 添加。
