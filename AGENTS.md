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
| ④ | 数据库设计与建立 | 🟢 认证3表✅ · 英语14表✅ · 学生管理4表✅（v2）+ 题库2表✅（knowledge_points / question_bank） |
| ⑤ | 后端接口开发 | 🟢 登录✅ · 学生CRUD✅ · 管理员10接口✅ · 题库+知识点API✅(14端点) · 前后端联调完成✅ |
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
│   │   └── 07-database-questionbank.md  题库&知识点DB v2(2表)
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
> 2026-07-04: 题库DB v2 — knowledge_points 表 + question_bank 增加原图/配图/老师解析/画图字段
> 2026-07-04: 老师端数学学科管理页 — 知识点CRUD(分页+年级筛选) + 题库管理(15题分页+7维筛选) + Canvas画图配图
> 2026-07-04: 老师端上传题目 — 新题(全校共享)/错题(选学生) + AI分析自动保存
> 2026-07-04: 知识点动态刷新 — 初三→初一~初三全阶段，高三→高一~高三，六年级→上下学期
> 2026-07-04: 题库DB更新 — ER图+teacher_analysis_image字段+配图流程
> 2026-07-04: 学生端题库挑战页 — 错题库/待做题/全屏自习/知识点侧边栏/掌握度标注
> 2026-07-02: 修复 POST 500 — Entity 改为双向映射(@ManyToOne)，解决 Hibernate INSERT 缺 FK 问题
> 2026-07-03: 学生端完成 — enrollments/schedule/reschedule/checkin/streak + 老师端调课审批 9接口
> 2026-07-03: AdminStudentDTO 新增 username/password — 管理员创建学生时可同步创建登录账号
> 2026-07-03: Student 实体新增 userId 字段 — students↔users 关联（学生端登录入口）
> 2026-07-03: 管理员老师CRUD完成
> 2026-07-03: 题库DB v2 — knowledge_points表 + question_bank增加原图/配图/AI识别字段
> 2026-07-03: 老师端数学学科管理页 — 知识点CRUD + 题库管理 + 配图审核
> 2026-07-04: 老师端上传题目 — 新题(全校共享)/错题(选学生) + AI分析自动保存
> 2026-07-04: 知识点动态刷新 — 初三→初一~初三全阶段，高三→高一~高三，六年级→上下学期
> 2026-07-04: 题库DB更新 — ER图+teacher_analysis_image字段+配图流程
> 2026-07-04: ✅ 题库+知识点后端完成 — 2实体 + 13API端点(知识点5 + 老师题库5 + 学生题库2 + 知识点多学期1) + 19文件 + 6条测试数据
> 2026-07-04: ✅ 前后端联调 — 知识点CRUD/题库列表/上传/详情对接完成 + 学生端待做题/错题库正常返回
> 2026-07-04: 🔧 联调修复 — 学生grade自动过滤(前缀LIKE匹配"初一"→"初一·上学期") + 知识点学生角色开放 + 题目返回知识名称(knowledgePointNames) + enrollment新增grade字段
> 2026-07-04: ✅ 老师端学生列表API — GET /api/v1/teacher/math-students(按老师+学科筛选) + student_enrollment.subject中文匹配修复
> 2026-07-04: 🐛 排查多角色登录冲突 — 根因是前端localStorage token共享, 后端Sa-Token is-concurrent:true配置正确
> 2026-07-04: 📝 AGENTS.md更新 — 第十一章题库&知识点API对接指南完整版(请求/响应示例/字段说明/检查清单)
> 2026-07-03: 学生端完整实现 — 首页+课表+打卡+调课+学科中心7功能
> 2026-07-03: 调课申请全流程 — 学生提交→老师批准/待议→自动改排课
> 2026-07-03: 学生账号创建 — 管理员新增学生时同步创建登录账号(username+password, roleType=4)
> 2026-07-03: AGENTS.md 第十一章 — 管理员 API 完整对接指南（10个接口的请求/响应/错误码/检查清单）
> 2026-07-02: 学生管理前端交互完善 — 排课按钮/课时联动/课程完成/清空排课归零
> 2026-07-02: 修复 GlobalExceptionHandler — 改用 ResponseEntity 确保 401/403 正确返回到前端
> 2026-07-02: 修复请求编码 — 加 server.servlet.encoding.force=true，强制 UTF-8 解码（前端 GBK→UTF-8）
> 2026-07-02: **v2 重构** — students 精简为全局档案 + 新增 teacher_student 关系表 + enrollment FK 改为指向 teacher_student
> 2026-07-02: v2 修复: POST 不再重复创建 Student（按姓名+学校去重），同一学生可被多个老师添加不冲突
> 2026-07-02: ✅ v2 数据库迁移完成 — 4表(students/teacher_student/enrollment/session) + 测试数据已就绪

---

## 十A、管理员端

| 页面 | 路由 | 说明 |
|------|------|------|
| 管理概览 | `/admin/dashboard` | 4统计卡片 + 快捷入口 |
| 学生管理 | `/admin/students` | 全局学生CRUD · 多选任课老师 |
| 老师管理 | `/admin/teachers` | 老师卡片 + 详情弹窗 |
| 排课查看 | `/admin/schedules` | 选老师→日历→详情 |
| 系统设置 | `/admin/settings` | AI模型选择 + 配置 |

### 学生↔老师 多对多关系

- 前端: 任课老师列显示多个标签 · 编辑时 `teacherIds[]` 多选
- 后端: POST/PUT 遍历 teacherIds，每个创建一条 teacher_student 记录
- 更新: 先删旧的 teacher_student WHERE student_id=?，再插入新的

```json
// POST/PUT /api/v1/admin/students
{ "name":"张三",
  "username":"zhangsan", "password":"123456", ..., "teacherIds": [6, 2],
  "username": "zhangsan", "password": "123456" }
// → teacher_student(teacher_id=6,student_id=1) + (teacher_id=2,student_id=1)
```

### 管理员后端 API 状态

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/v1/admin/students` | 全局学生列表(不过滤teacher_id) | ✅ |
| POST | `/api/v1/admin/students` | 新增学生(含teacherIds) | ✅ |
| PUT | `/api/v1/admin/students/{id}` | 更新学生 | ✅ |
| DELETE | `/api/v1/admin/students/{id}` | 删除学生 | ✅ |
| GET | `/api/v1/admin/teachers` | 老师列表(含学生数/课时) | ✅ |
| POST | `/api/v1/admin/teachers` | 新增老师 | ✅ |
| PUT | `/api/v1/admin/teachers/{userId}` | 更新老师 | ✅ |
n> **orgName 处理**: 前端传机构名称文本, 后端 `organization` 表按 `name` 查找, 不存在则 INSERT 新机构, 然后将 `org_id` 写入 `teachers` 表。
| DELETE | `/api/v1/admin/teachers/{userId}` | 删除老师 | ✅ |
| GET | `/api/v1/admin/teachers/{id}` | 老师详情+学生列表 | ✅ |
| GET | `/api/v1/admin/schedules` | 全部排课(?teacherId=&year=&month=) | ✅ |
| GET | `/api/v1/admin/stats` | 概览统计 | ✅ |
| GET | `/api/v1/admin/settings` | 获取系统配置 | ✅ |
| PUT | `/api/v1/admin/settings` | 更新系统配置 | ✅ |

> 全部需要登录（Sa-Token），**仅 roleType=1（管理员）可访问**，非管理员返回 403


## 十一、题库 & 知识点 API 对接指南 ← 前端同事看这里

> **后端状态**: ✅ 已全部实现 · 数据库表: `knowledge_points` + `question_bank`  
> **测试数据**: 13个知识点 + 4道新题 + 2道错题（已就绪）  
> **认证**: 老师端需 roleType=3，学生端需 roleType=4

### 11.1 知识点 API（全老师共享，增删改全平台生效）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/knowledge-points?subject=math&gradeLevel=初三·上学期&page=1&pageSize=15` | 老师端：列表(分页15，按年级筛选) |
| GET | `/api/v1/knowledge-points?subject=math&grades=初一·上学期,初一·下学期,...&page=1&pageSize=50` | 学生端：多学期批量查询 |
| POST | `/api/v1/knowledge-points` | 新增知识点 |
| PUT | `/api/v1/knowledge-points/{id}` | 修改知识点 |
| DELETE | `/api/v1/knowledge-points/{id}` | 删除知识点 |

**GET 列表响应**（`data` 内）：
```json
{
  "list": [{
    "id": 1, "subject": "math", "name": "有理数运算",
    "gradeLevel": "初一·上学期", "parentId": null, "sortOrder": 1,
    "createdAt": "2026-07-04T17:30:00", "updatedAt": "2026-07-04T17:30:00"
  }],
  "total": 13, "page": 1, "pageSize": 15
}
```

**POST/PUT 请求体**：
```json
{ "name": "勾股定理", "subject": "math", "gradeLevel": "初二·下学期", "parentId": null, "sortOrder": 1 }
```
| 字段 | 必填 | 说明 |
|------|------|------|
| name | ✅ | 知识点名称（同科目下唯一） |
| subject | ✅ | 学科（math/chinese/english/...） |
| gradeLevel | 否 | 年级·学期（如"初三·上学期"） |
| parentId | 否 | 父知识点ID（树形结构） |
| sortOrder | 否 | 排序，默认0 |

### 11.2 题库 API（老师端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/teacher/questions?subject=math&kpId=&type=&studentId=&gradeLevel=&date=&page=1&pageSize=15` | 题库列表(7维筛选+分页) |
| GET | `/api/v1/teacher/questions/{id}` | 题目详情(含原图/配图/AI解析/老师解析) |
| PUT | `/api/v1/teacher/questions/{id}` | 更新题目(只传需改字段) |
| POST | `/api/v1/teacher/questions/upload` | 老师上传新题(全校共享) |
| DELETE | `/api/v1/teacher/questions/{id}` | 删除题目 |

**GET 列表查询参数**（`subject` 必填，其余可选）：

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| subject | string | 学科（必填） | `math` |
| kpId | long | 知识点ID（LIKE匹配knowledge_point_ids） | `3` |
| type | string | 题型 | `WRONG` / `NEW` |
| studentId | long | 学生ID | `9` |
| gradeLevel | string | 年级·学期 | `初三·上学期` |
| date | string | 上传日期 | `2026-07-04` |
| page | int | 页码，默认1 | `1` |
| pageSize | int | 每页条数，默认15 | `15` |

**GET 列表响应**（`data` 内）：
```json
{
  "list": [{
    "id": 1, "subject": "math", "type": "NEW", "source": "TEACHER",
    "studentId": null, "studentName": null,
    "teacherId": 10, "teacherName": "zsy",
    "title": "计算：(-3)² + |-5| - 2×(-4)",
    "knowledgePointIds": "1", "answer": "20",
    "difficulty": "EASY", "mastery": "UNMASTERED",
    "gradeLevel": "初一·上学期",
    "diagramStatus": "NONE",
    "originalImageUrl": null, "diagramImageUrl": null,
    "aiExtractedText": null, "analysis": null, "solution": null, "similarJson": null,
    "teacherAnalysis": null, "teacherAnalysisImage": null,
    "errorType": null,
    "createdAt": "2026-07-04T17:30:00", "updatedAt": "2026-07-04T17:30:00"
  }],
  "total": 4, "page": 1, "pageSize": 15
}
```

**GET 详情响应**：同列表单条格式，含全部字段。

**POST upload 请求体**：
```json
{
  "subject": "math",
  "title": "题目文字内容",
  "answer": "正确答案",
  "knowledgePointIds": "3,7",
  "difficulty": "MEDIUM",
  "gradeLevel": "初三·上学期",
  "originalImageUrl": "/uploads/xxx.png",
  "diagramImageUrl": "/uploads/diagram_xxx.png",
  "diagramStatus": "AUTO",
  "aiExtractedText": "AI识别原始文字",
  "teacherAnalysis": "老师手写解析",
  "teacherAnalysisImage": "/uploads/analysis_xxx.png"
}
```
| 字段 | 必填 | 说明 |
|------|------|------|
| subject | ✅ | 学科 |
| title | ✅ | 题目文字 |
| answer | 否 | 正确答案 |
| knowledgePointIds | 否 | 关联知识点ID，逗号分隔（如"3,7,12"） |
| difficulty | 否 | EASY / MEDIUM / HARD，默认MEDIUM |
| gradeLevel | 否 | 年级·学期 |
| originalImageUrl | 否 | 原拍照图片URL |
| diagramImageUrl | 否 | 配图URL |
| diagramStatus | 否 | NONE / AUTO / MANUAL |
| teacherAnalysis | 否 | 老师文字解析 |
| teacherAnalysisImage | 否 | 老师解析配图URL |

**PUT 请求体**：与 POST 类似，但所有字段选填（只传需更新的字段）。额外支持：`mastery`、`analysis`、`solution`、`errorType`。

### 11.3 题库 API（学生端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/student/questions/wrong?subject=math&page=1&pageSize=15` | 我的错题库 |
| GET | `/api/v1/student/questions/new?subject=math&gradeLevel=初一·上学期&page=1&pageSize=15` | 待做题（全校新题） |

**响应格式**：与老师端题库列表一致（`QuestionPageVO`）。

### 11.4 前端对接检查清单

1. ✅ **知识点列表** → 老师端调 `GET /api/v1/knowledge-points?subject=math&gradeLevel=初三·上学期`，分页15
2. ✅ **知识点CRUD** → POST/PUT/DELETE，同科目下 name 唯一
3. ✅ **学生端知识点** → `GET /api/v1/knowledge-points?subject=math&grades=初一·上学期,初一·下学期`（逗号分隔多学期）
4. ✅ **题库列表** → `GET /api/v1/teacher/questions?subject=math` + 可选筛选参数，默认分页15
5. ✅ **题目详情** → `GET /api/v1/teacher/questions/{id}`，含 teacherName + 全部解析字段
6. ✅ **上传新题** → `POST /api/v1/teacher/questions/upload`，type自动设为NEW，source=TEACHER
7. ✅ **更新题目** → `PUT /api/v1/teacher/questions/{id}`，只传需改字段（如 knowledgePointIds / teacherAnalysis / diagramImageUrl）
8. ✅ **学生错题库** → `GET /api/v1/student/questions/wrong?subject=math`，自动取当前学生的错题(type=WRONG)
9. ✅ **学生待做题** → `GET /api/v1/student/questions/new?subject=math&gradeLevel=`，全校老师上传的NEW题

### 11.5 错误处理

```json
{ "code": 400, "message": "该学科下已存在同名知识点", "data": null }
{ "code": 401, "message": "请先登录", "data": null }
{ "code": 403, "message": "仅教师可访问", "data": null }
{ "code": 404, "message": "题目不存在", "data": null }
{ "code": 404, "message": "学生档案不存在，请联系管理员", "data": null }
```

## 十B、学生端

| 页面 | 路由 | 说明 |
|------|------|------|
| 学习中心 | `/student/dashboard` | 学科卡片+今日课表+打卡日历 |
| 课程表 | `/student/schedule` | 日历高亮+查看详情+申请调课 |
| 学科中心 | `/student/subject/:subject` | 7功能入口 |
| AI错题分析 | `/student/subject/math/wrong-analysis` | 拍照上传→AI解析 |
| AI试卷分析 | `/student/subject/math/exam-analysis` | 上传试卷→扣分分析 |
| 题库 | `/student/subject/math/question-bank` | 错题库+计时自习 |
| AI动图 | `/student/subject/math/ai-animation` | 动点题生成动画 |
| AI聊天 | `/student/subject/math/ai-chat` | 智能问答 |
| 知识点 | `/student/subject/math/knowledge-points` | 本学���体系 |
| 作业 | `/student/subject/math/homework` | 老师布置+打勾 |

### 11.1 认证要求

所有接口需要在 Header 中携带 token：

```
Authorization: Bearer <token>
```

先用 `admin / admin123` 调用 `POST /api/v1/auth/login` 获取 token。

### 11.2 学生管理

#### GET `/api/v1/admin/students` — 全局学生列表

**查询参数**：`?page=1&pageSize=20&keyword=张&grade=初一`

**响应**（`data` 内）：
```json
{
  "list": [{
    "id": 1,
    "name": "张三",
    "gender": "男",
    "contact": "13800001111",
    "grade": "初一",
    "school": "第一实验中学",
    "createdAt": "2026-07-02T10:00:00",
    "updatedAt": "2026-07-02T10:00:00",
    "teacherRelations": [
      { "teacherId": 6, "teacherName": "赵老师", "subjects": ["英语","数学"], "hoursLeft": 15 },
      { "teacherId": 2, "teacherName": "李老师", "subjects": ["数学"], "hoursLeft": 0 }
    ]
  }],
  "total": 28, "page": 1, "pageSize": 20
}
```

> **id** = `students.id`（全局学生ID，不是 teacher_student.id）

#### POST `/api/v1/admin/students` — 新增学生

**请求体**：
```json
{
  "name": "张三",
  "gender": "男",
  "contact": "13800001111",
  "grade": "初一",
  "school": "第一实验中学",
  "teacherIds": [6, 2],
  "username": "zhangsan", "password": "123456"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| name | ✅ | 姓名 |
| gender | 否 | 男/女 |
| contact | 否 | 联系方式 |
| grade | 否 | 一年级~高三 |
| school | 否 | 所在学校 |
| teacherIds | 否 | 任课老师ID列表（`users.id`），每个创建一个 teacher_student 记录 |
|| username | ✅(新增) | 登录账号，创建 users 记录(roleType=4) |
|| password | ✅(新增) | 登录密码 |

**行为**：
- 按姓名+学校查重，同名同校返回已有 Student 不重复创建
- 遍历 teacherIds，跳过已存在的 teacher_student 关系（不报错）
- 返回完整的 `AdminStudentVO`（含 `teacherRelations`）

**响应**：同 GET 列表中的单条格式（`data` 直接是对象，无分页包裹）

**错误**：
| 场景 | code | message |
|------|------|---------|
| 姓名未填 | 400 | "姓名不能为空" |
| 非管理员 | 403 | "仅管理员可访问" |

#### PUT `/api/v1/admin/students/{id}` — 更新学生

**请求体**：与 POST 相同格式

**行为**：
- 更新 Student 基本信息
- **teacherIds 重建逻辑**：先删该学生的全部旧 teacher_student 记录，再按新 `teacherIds` 逐条插入
- 如果 `teacherIds` 不传（null），则保留原有老师关系不变
- 如果传 `"teacherIds": []`（空数组），清空所有老师关系

#### DELETE `/api/v1/admin/students/{id}` — 删除学生

**行为**：级联删除全部 teacher_student → enrollment → session，最后删除 Student 档案

### 11.3 老师管理

#### GET `/api/v1/admin/teachers` — 老师列表
POST   | `/api/v1/admin/teachers` | 新增老师 | 🔴P0 |
PUT    | `/api/v1/admin/teachers/{userId}` | 更新老师 | 🔴P0 |
n> **orgName 处理**: 前端传机构名称文本, 后端 `organization` 表按 `name` 查找, 不存在则 INSERT 新机构, 然后将 `org_id` 写入 `teachers` 表。
DELETE | `/api/v1/admin/teachers/{userId}` | 删除老师 | 🔴P0 |

**查询参数**：`?page=1&pageSize=20&keyword=李`

**响应**（`data` 内）：
```json
{
  "list": [{
    "id": 1,
    "userId": 2,
    "username": "coach",
    "realName": "李老师",
    "phone": null,
    "email": null,
    "subjects": ["math", "physics"],
    "title": "高级教师",
    "orgName": "第一实验中学",
    "avatar": null,
    "status": 1,
    "studentCount": 5,
    "totalHours": 120
  }],
  "total": 5, "page": 1, "pageSize": 20
}
```

| 字段 | 说明 |
|------|------|
| id | teachers 表主键 |
| userId | users 表主键（传给 GET teachers/{id} 用这个） |
| subjects | 任教学科列表 |
| studentCount | 该老师下的学生数量 |
| totalHours | 该老师全部学生的剩余课时汇总 |

#### GET `/api/v1/admin/teachers/{id}` — 老师详情

> **注意**：路径中的 `{id}` 是 **`userId`**（`users.id`），不是 teachers.id

**响应**（`data` 内）：
```json
{
  "id": 1,
  "userId": 2,
  "username": "coach",
  "realName": "李老师",
  "phone": null,
  "email": null,
  "subjects": ["math", "physics"],
  "title": "高级教师",
  "orgName": "第一实验中学",
  "avatar": null,
  "bio": null,
  "status": 1,
  "studentCount": 5,
  "totalHours": 120,
  "students": [
    {
      "tsId": 10,
      "studentId": 1,
      "studentName": "张三",
      "gender": "男",
      "grade": "初一",
      "school": "第一实验中学",
      "hoursLeft": 15,
      "subjects": ["英语", "数学"]
    }
  ]
}
```

#### POST `/api/v1/admin/teachers` — 新增老师

**请求体**：
```json
{
  "realName": "张老师",
  "username": "zhang",
  "password": "123456",
  "title": "高级教师",
  "subjectIds": ["数学", "物理"],
  "orgId": 1,
  "phone": "138xxx",
  "email": "zhang@example.com"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| realName | ✅ | 真实姓名 |
| username | ✅(新增) | 用户名，唯一 |
| password | ✅(新增) | 密码 |
| title | 否 | 职称 |
| subjectIds | 否 | 任教学科数组 → 逗号分隔存入 teachers.subject_ids |
| orgId | 否 | 机构ID |
| phone | 否 | 手机号 |
| email | 否 | 邮箱 |

**响应**：同 GET 列表中的单条 `AdminTeacherVO` 格式

#### PUT `/api/v1/admin/teachers/{userId}` — 更新老师

**请求体**：与 POST 相同格式。`password` 留空/不传 = 不修改密码，`username` 不变 = 可省略

n> **orgName 处理**: 前端传机构名称文本, 后端 `organization` 表按 `name` 查找, 不存在则 INSERT 新机构, 然后将 `org_id` 写入 `teachers` 表。
#### DELETE `/api/v1/admin/teachers/{userId}` — 删除老师

**行为**：级联删除该老师的全部 teacher_student 关系 → Teacher 记录 → User 记录

### 11.4 排课查看

#### GET `/api/v1/admin/schedules` — 全部排课

**查询参数**：`?teacherId=6&year=2026&month=7&page=1&pageSize=50`

| 参数 | 说明 |
|------|------|
| teacherId | 可选，筛选指定老师的排课 |
| year / month | 可选，筛选指定月份的排课（不传则返回全部） |
| page / pageSize | 分页，默认 1/50 |

**响应**（`data` 内）：
```json
{
  "list": [
    {
      "sessionId": 1,
      "classDate": "2026-07-03",
      "startTime": "14",
      "endTime": "16",
      "subject": "英语",
      "teacherId": 6,
      "teacherName": "赵老师",
      "studentId": 1,
      "studentName": "张三"
    }
  ],
  "total": 10, "page": 1, "pageSize": 50
}
```

### 11.5 概览统计

#### GET `/api/v1/admin/stats` — Dashboard 数据

**响应**（`data` 内）：
```json
{
  "teacherCount": 5,
  "studentCount": 28,
  "totalHours": 520,
  "relationCount": 35,
  "sessionCount": 80,
  "enrollmentCount": 42
}
```

| 字段 | 说明 | 用于 |
|------|------|------|
| teacherCount | 教师总数 | 统计卡片1 |
| studentCount | 学生总数 | 统计卡片2 |
| totalHours | 全部剩余课时汇总 | 统计卡片3 |
| relationCount | 老师-学生关系总数 | 备用 |
| sessionCount | 排课记录总数 | 统计卡片4 |
| enrollmentCount | 报名科目总数 | 备用 |

### 11.6 系统设置

#### GET `/api/v1/admin/settings` — 获取配置

**响应**（`data` 内）：
```json
{
  "ai_model": "deepseek-chat",
  "ai_api_key": "sk-xxx",
  "ai_api_url": "https://api.deepseek.com/v1",
  "system_name": "安文AI教育",
  "max_concurrency": "10"
}
```

> 首次使用时返回 `{}`，需要先 PUT 写入配置

#### PUT `/api/v1/admin/settings` — 更新配置

**请求体**（只传需更新的字段，未传的字段保持不变）：
```json
{
  "aiModel": "deepseek-chat",
  "aiApiKey": "sk-xxx",
  "aiApiUrl": "https://api.deepseek.com/v1",
  "systemName": "安文AI教育",
  "maxConcurrency": "10"
}
```

**响应**：返回更新后的全部配置（同 GET）

### 11.7 前端对接检查清单

1. ✅ **登录** → `POST /api/v1/auth/login` with `admin/admin123`，拿到 token
2. ✅ **学生列表** → `GET /api/v1/admin/students`，展示表格 + `teacherRelations` 渲染为标签
3. ✅ **新增学生** → 表单含 `teacherIds` 多选下拉（调 GET teachers 拿列表）
4. ✅ **编辑学生** → 回填表单，teacherIds 重建逻辑由后端处理
5. ✅ **删除学生** → 弹窗确认后调 DELETE（注意：会级联删除该学生的所有排课数据）
6. ✅ **老师列表** → `GET /api/v1/admin/teachers`，卡片/表格展示
7. ✅ **老师详情** → `GET /api/v1/admin/teachers/{userId}`，弹窗展示学生列表
8. ✅ **排课查看** → 先选老师（调 GET teachers），再选月份，调 GET schedules
9. ✅ **管理概览** → `GET /api/v1/admin/stats`，4 个统计卡片
10. ✅ **系统设置** → `GET /api/v1/admin/settings` 回填 → PUT 保存

### 11.8 错误处理

统一响应格式，非 200 时 `message` 为可展示给用户的错误信息：

```json
{ "code": 400, "message": "该学生已存在（姓名+第一实验中学）", "data": null }
{ "code": 401, "message": "请先登录", "data": null }
{ "code": 403, "message": "仅管理员可访问", "data": null }
{ "code": 404, "message": "学生不存在", "data": null }
```


## 十二、学生管理 API（老师端·前端对接）
### 学生端后端 API 状态

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/v1/student/enrollments` | 学生报名科目列表(用于学科中心) | ✅ |
| GET | `/api/v1/student/schedule` | 学生课表(?year&month) | ✅ |
| POST | `/api/v1/student/reschedule` | 提交调课申请 | ✅ |
| GET | `/api/v1/teacher/reschedules` | 老师查看调课申请 | ✅ |
| PUT | `/api/v1/teacher/reschedules/{id}/approve` | 批准调课 | ✅ |
| PUT | `/api/v1/teacher/reschedules/{id}/defer` | 待议调课 | ✅ |
| DELETE | `/api/v1/teacher/reschedules/{id}` | 关闭申请 | ✅ |
| POST | `/api/v1/student/checkin` | 学习打卡 | ✅ |
| GET | `/api/v1/student/streak` | 打卡天数 | ✅ |
| POST | `/api/v1/student/subject/{subject}/wrong-question` | 上传错题图片 | ⏳ 待文件模块 |
| POST | `/api/v1/student/subject/{subject}/exam` | 上传试卷 | ⏳ 待文件模块 |

> 学生需已有 `students.user_id` 关联才能使用学生端 API。管理员创建学生时填 `username` 即自动绑定。

### 学生端 API 对接指南

#### 认证：学生用 `username/password` 登录 → 获取 token → 调以下接口

#### GET `/api/v1/student/enrollments`
```json
{
  "courses": [{
    "tsId": 1, "subject": "英语", "teacherName": "王老师",
    "teacherId": 3, "hoursLeft": 15,
    "upcomingSessions": [
      {"id": 1, "classDate": "2026-07-03", "startTime": "14", "endTime": "16"}
    ]
  }]
}
```

#### GET `/api/v1/student/schedule?year=2026&month=7`
```json
{
  "schedules": [
    {"sessionId": 1, "classDate": "2026-07-03", "startTime": "14", "endTime": "16",
      "subject": "英语", "teacherName": "王老师", "teacherId": 3}
  ]
}
```

#### POST `/api/v1/student/reschedule`
```json
// 请求
{ "sessionId": 1, "requestedDate": "2026-07-10", "requestedStart": "16", "requestedEnd": "18", "reason": "临时有事" }
// 响应: RescheduleVO（含 id/status= pending）
```

#### POST `/api/v1/student/checkin` — 打卡（无请求体，每日限一次）
#### GET `/api/v1/student/streak` → `{ "streak": 5, "totalDays": 12, "checkedInToday": true }`

#### 老师端调课：
- **GET** `/api/v1/teacher/reschedules` → 待审批列表
- **PUT** `/api/v1/teacher/reschedules/{id}/approve` → 批准(自动更新session日期)
- **PUT** `/api/v1/teacher/reschedules/{id}/defer` → 待议
- **DELETE** `/api/v1/teacher/reschedules/{id}` → 关闭

### 前端交互逻辑

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

## 十三、老师端数学学科管理 — 对接进度

### 已完成

| 功能 | 前端 | 后端 |
|------|------|------|
| 知识点 CRUD | ✅ API调用 | ✅ 4接口全通 |
| 知识点分页+年级筛选 | ✅ | ✅ |
| 题库列表(7维筛选) | ✅ API调用 | ✅ |
| 题目详情弹窗 | ✅ | ✅ GET /teacher/questions/{id} |
| 题目保存(知识点/解析/配图) | ✅ PUT调用 | ✅ |
| Canvas画图配图 | ✅ 本地Canvas | — |
| 年级→知识点联动 | ✅ disabled逻辑 | ✅ |
| 年级范围(三年级~高三) | ✅ 20学期 | — |

### 待完成

| 功能 | 状态 | 需后端 |
|------|------|--------|
| 上传题目 | 🟡 UI就绪·AI模拟 | POST /teacher/questions/upload 已实现,前端未调真实API |
| 学生下拉列表 | 🔴 硬编码3个 | 需 `/admin/teachers/{id}/math-students` 返回该老师报名数学的学生 |
| 题库学生筛选 | 🟡 下拉已有 | 同上,学生列表需从API获取 |
| 知识点名称显示(题库表) | ✅ 已用knowledgePointNames | — |

### 后端待实现（仅1个）

```
GET /api/v1/teacher/math-students
→ 返回当前老师名下报名了数学科目的学生列表
→ [{ id:9, name:"白克林", grade:"初三" }, ...]
```

