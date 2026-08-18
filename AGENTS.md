# EduAI · 智学AI教育 — 项目上下文

> 前后端物理分离，本文件为两端共享上下文。**本仓库是前端**，后端在独立仓库。
> 上下文变更后同步到两边。

| 端 | 位置 | 打开方式 |
|---|---|---|
| 前端（本仓库） | `web/` | VS Code 打开本目录 |
| 后端 | `D:\soft\Icode\EduAI-server` | IDEA 打开 |

## 一、项目与栈

安文AI教育（现名「智学AI教育」）— 全学科 AI 智能学习平台，9 学科 × 3 角色（老师/学生/管理员）。

| 端 | 技术栈 | 本地地址 |
|---|---|---|
| 前端 | Vue 3 + Element Plus + Pinia + Vite | http://localhost:5173 |
| 后端 | Spring Boot 3.4.5 + JDK 21 + Maven 多模块 + Sa-Token + JPA + Redis | http://localhost:8080 |
| 反代 | Nginx（SSE / OCR 代理） | http://localhost:8090 |

学科：语📝 数📐 英📖 物⚛️ 化🧪 生🧬 史📜 政⚖️ 地🌍

仓库：前端 `https://github.com/KHere-Z/EduAIFront` · 后端 `https://github.com/KHere-Z/EduAI`

## 二、目录

```
本仓库（前端）
├── web/                      Vue 3 前端
│   ├── src/
│   │   ├── views/            三角色页面（teacher / student / admin / login / register / home / profile / recharge）
│   │   ├── layouts/          TeacherLayout / StudentLayout / AdminLayout
│   │   ├── router/index.js   路由（三角色 + 动态学科）
│   │   ├── store/auth.js     Pinia 认证（角色 token 隔离）
│   │   ├── api/common/       Axios 封装（admin/auth/students/student/questions/knowledge/ai）
│   │   ├── utils/            markdown 渲染等
│   │   └── assets/styles/    设计系统
│   └── tests/test_frontend.py  Playwright 全量测试
├── docs/spec/                需求/架构/API/DB 设计（01~07）
├── nginx.conf                本地反代（5173 + 8080 + OCR 8765）
├── ocr_server.py             PaddleOCR 在线代理（FastAPI，:8765）
└── .mcp.json                 jCodemunch MCP 配置

后端仓库（另见 D:\soft\Icode\EduAI-server）
├── eduai-common       公共模块（Result / 异常 / 限流 / 缓存 / 异步）
├── eduai-security     认证鉴权（Sa-Token · 登录注册 · 积分/会员 · 关系/支付）
├── eduai-system       启动模块（学生/老师/题库/知识点/试卷/作业/反馈/出卷/资源）
└── eduai-ai           AI 客户端（DeepSeek + Doubao · SSE · 识图）
```

## 三、角色与认证

| 角色 | roleType | 首页 | 登录方式 |
|---|---|---|---|
| 管理员 | 1 | `/admin/dashboard` | 密码 `admin / admin123` |
| 老师 | 3 | `/teacher/dashboard` | 密码 `coach`(数学+物理) / `english`(英语) / `math`(单科) / `multi`(三科) |
| 学生 | 4 | `/student/dashboard` | 自助注册 / 短信验证码 / 微信 |

- **token 按角色隔离**：`localStorage.eduai_token_{roleType}`，多 tab 不同角色不串号。
- 登录方式见 `web/src/api/common/auth.js`：密码 / 短信（`send-sms` → `login-sms`）/ 微信（`wechat-login`、绑定/解绑手机）/ 注册。

## 四、前端路由

**老师端 `/teacher`**

| 路由 | 页面 |
|---|---|
| `dashboard` | 工作台 |
| `students` | 学生信息管理 |
| `subject/math/manage` | 数学学科管理（知识点/题库/试卷/资源） |
| `subject/math/kp-resources/:kpId` | 知识点资源管理 |
| `subject/math/exam-builder` | 出卷 |
| `subject/:subject` | 学科中心（9 学科，SubjectCenter 动态渲染） |
| `english/home` · `english/word-progress` | 英语中心 / 单词进度 |
| `classroom / vocab-test / word-memorize / ai-reading / ai-dialogue / grammar / sentence-practice / feedback` | 英语 8 功能 |
| `profile` · `profile/:uid` | 个人中心 / 用户主页 |
| `recharge` | 智学点充值 |

**学生端 `/student`**

| 路由 | 页面 |
|---|---|
| `dashboard` / `schedule` | 学习中心 / 课程表 |
| `subject/:subject` | 学科中心（SubjectHome） |
| `subject/:subject/{wrong-analysis, exam-analysis, question-bank, ai-animation, knowledge-points, homework, feedback, resources}` | 学科 8 功能 |
| `subject/:subject/knowledge-points/:kpId` | 知识点资源详情 |
| `wrong-book` / `ai-analysis` / `scores` / `practice` | 错题本 / AI 学情 / 成绩 / 智能练习 |
| `profile` · `profile/:uid` · `recharge` | 个人中心 / 用户主页 / 充值 |

**管理员端 `/admin`**

| 路由 | 页面 |
|---|---|
| `dashboard` / `students` / `teachers` / `schedules` / `settings` / `resources` | 概览 / 学生 / 老师 / 排课 / 系统设置（含 AI 模型管理）/ 学习资源上传 |

## 五、接口约定

- 前缀 `/api/v1/`，认证 Header `Authorization: Bearer <token>`
- 统一响应 `{ code, message, data }`；成功 `code=200, message="success"`（前端 request.js 按 `code !== 200` 判错，文档示例勿写 `0`/`"ok"`）
- 分页 `?page=1&pageSize=20` → `{ list, total, page, pageSize }`
- 文件上传 `multipart/form-data`；JSON 体经 `request.js` 强制 UTF-8（避免 Windows GBK 乱码）
- 前端 API / 上传资源 URL 统一走**相对路径**（`/api`、`/uploads`），由 Nginx 反代到后端；可用 `VITE_API_BASE` / `VITE_UPLOAD_BASE` 环境变量覆盖（封装见 `web/src/utils/url.js`）。**禁止在组件里硬编码 `localhost`**

## 六、核心 API（前端已封装于 `web/src/api/common/`）

| 域 | 端点 | 文件 |
|---|---|---|
| 认证 | `POST /auth/login` · `/login-sms` · `/wechat-login` · `/register` · `/send-sms` · `/bind-phone` · `/bind-wechat`；`GET /auth/me` | auth.js |
| 管理员 | `CRUD /admin/students` · `/admin/teachers` · `/admin/models`；`GET /admin/schedules` · `/admin/stats` · `/admin/settings` | admin.js |
| 学生管理 | `CRUD /students` · `PATCH /students/{id}/hours` · `GET /students/calendar`；`GET /student/enrollments` · `/schedule` · `/streak`；`POST /student/checkin` · `/reschedule` | students.js / student.js |
| 题库 | `CRUD /teacher/questions` · `/upload`；`GET /student/questions/wrong` · `/new` · `/similar` | questions.js |
| 知识点 | `CRUD /knowledge-points` · `/knowledge-points/{kpId}/resources` · `/resources/{id}/download` | knowledge.js |
| 学习资源 | `GET/POST/DELETE /resource/textbooks` · `GET/POST /resource/textbooks/{id}/chapters` · `GET/POST /resource/chapters/{id}/sections` · `GET /resource/resources` · `POST /resource/resources/upload` · `GET/DELETE /resource/resources/{id}`（目录删除 `/resource/chapters/{id}`、`/resource/sections/{id}`，均级联） | resources.js |
| 反馈 | `GET/POST/DELETE /teacher/feedbacks`；`GET /student/feedbacks` | admin.js |
| AI | `POST /ai/chat`（通用）· `/ai/wrong-analysis` · `/ai/exam-analysis` · `/ai/upload`（详见 §七） | ai.js |
| 支付/充值 | `POST /payment/create` · `/payment/status/{orderId}` · `/payment/mock-pay/{orderId}` | — |
| 积分/会员 | `GET /user/points` · `/user/points/history` · `/user/membership` | — |
| 收益/提现（隐藏保留） | `GET /teacher/revenue` · `POST /teacher/revenue/withdraw`；`GET /admin/revenue/withdraws` · `POST /admin/revenue/withdraws/{id}/review` | —（提现已隐藏，菜单入口注释；分成改为直接发智学点，见 §十一） |
| 师生关系 | `GET /relations` · `/relations/incoming` · `POST /relations/request` · `PUT /relations/{id}/approve` `/reject` | — |
| 用户主页 | `GET /users/{uid}` | — |
| 作业 | `CRUD /teacher/homework` · `GET /student/homework` · `POST /student/homework/{id}/submit` | — |
| 试卷 | `CRUD /student/exam-papers` · `GET/PUT/DELETE /teacher/exam-papers` | — |
| 出卷 | `POST /teacher/exam-builder/export-pdf` | — |

> 「文件 = —」为后端已实现、尚未封装进 `api/common/` 的端点，前端可直接 axios 调用。
> 学习资源模块后端已就绪：`resourceService.js`「API 优先 + localStorage 兜底」自动切后端；本模块所有 GET 返回扁平数组 `data: [...]`（暂不分页，无 `{list,total}`）。

## 七、AI 服务

- **双模型**：DeepSeek（通用/错题） + Doubao（试卷分析，支持识图），按模块路由。
- **模型集中管理**：`ai_models`（CRUD） + `ai_config`（`{feature}_model` 指向 `ai_models.value` → 取 apiUrl/apiKey）。
- **通用聊天**：`POST /ai/chat`（AIChat.vue 依赖，后端已实现）+ `/ai/chat/stream`（流式，后端已实现，待前端接线）。
- **流式输出**：SSE `/ai/wrong-analysis/stream` · `/ai/exam-analysis/stream`，前端经 Nginx 8090 反代（解决跨域）。
- 上传图片/PDF/Word 走 `/ai/upload`；OCR 走 `ocr_server.py`（PaddleOCR-VL，经 `/ocr` 代理）。

`ai.js` 已封装的端点（**前端预留，后续实现，勿删**）：

| 函数 | 端点 | 状态 |
|---|---|---|
| `sendChatMessage` | `POST /ai/chat` | 通用聊天，AIChat.vue 在用（后端已实现） |
| `analyzeWrongQuestion` | `POST /ai/wrong-analysis` | 错题分析，在用 |
| `analyzeExam` | `POST /ai/exam-analysis` | 试卷分析，在用 |
| `sendChatMessageStream` | `POST /ai/chat/stream` | 流式，后端已实现、待前端接线 |
| `getChatHistory` / `deleteChatHistory` | `GET/DELETE /ai/chat/history` | 聊天历史，待后端 |
| `uploadChatFile` | `POST /ai/upload` | 文件上传，后端已有、前端暂用 base64 直传 |
| `getAnalysisResult` | `GET /ai/analysis/{id}` | 分析结果查询，待后端 |

> `getChatHistory` / `deleteChatHistory` / `getAnalysisResult` / `uploadChatFile` 当前均**无视图调用**，属纯预留（未来功能），非阻塞。当前分析走同步返回字符串（`analyzeWrongQuestion` / `analyzeExam` 直接拿结果），暂无「按 id 查分析」的产品场景。

## 八、数据模型

| 域 | 表 |
|---|---|
| 认证 | users / teachers / organization / user_wechat |
| 学生管理 | students / teacher_student / student_enrollment / student_session |
| 英语 | 词库/语法/阅读等 14 表 |
| 题库 | knowledge_points / question_bank（含原图/配图/解析/掌握度）/ question_knowledge_point（题目-知识点关联）/ student_question_progress（共享题进度按学生隔离） |
| 知识点资源 | kp_resources |
| 学习资源 | resource_textbook / resource_chapter / resource_section / resource_file（教材→章节→小节→资源 4 级；种子数据见 `docs/sql/08-resource-catalog.sql`） |
| 收益/提现 | resource_downloads（下载去重）；teacher_earnings / withdraw_requests（提现隐藏保留，暂不写入） |
| 试卷 | exam_papers |
| 作业 | homework / homework_submissions |
| 反馈 | feedbacks |
| AI | ai_models / ai_config |
| 会员 | users.points / point_transactions / membership |
| 关系 | user_relations |

## 九、关键架构决策

- **动态学科路由** `/subject/:subject/*` 一个路由渲染 9 学科，避免 81 个页面。
- **角色 token 隔离**（多 tab 共存）。
- **前后端物理分离**：VS Code 只开 `web/`，IDEA 只开后端。
- **Nginx 反代**统一入口（5173/8080/8765），解决 SSE 与 OCR 跨域。**部署必做**：服务器 Nginx 必须反代 `/api/`、`/uploads/`、`/ocr` 三条路径到后端，否则上传图片 404、流式聊天失效（本仓库 `nginx.conf` 已含这三条 location 供参考）。
- **前端按需引入**（`web/`）：Element Plus 模板组件/样式由 `unplugin-vue-components` 自动按需导入；`main.js` 仅手动引入 `ElMessage`/`ElMessageBox` 样式、`v-loading` 指令（`ElLoading`）以及**实际用到的图标**。**勿**再 `app.use(ElementPlus)` 或 `import * from '@element-plus/icons-vue'` 整包引入（会把 ~1MB 打进主包、图标 ~170KB）。

## 十、本地运行

```bash
# 前端
cd web && npm install && npm run dev        # :5173

# 后端（独立仓库）
# IDEA 运行 eduai-system，:8080

# OCR（可选）
python ocr_server.py                          # :8765

# 反代（可选，SSE/OCR 用）
nginx -c nginx.conf                           # :8090

# 前端测试
PYTHONIOENCODING=utf-8 python web/tests/test_frontend.py
```

## 十一、状态与待办

- 业务接口基本完成：登录/注册（密码/短信/微信）、学生/老师/排课管理、题库/知识点（含知识点资源）、学习资源（教材→章节→小节→资源 4 级目录）、资源下载分成（老师 50% 分成，直接发放智学点）、试卷、作业、反馈、AI 双模型 + 流式、出卷 PDF、智学点/会员/支付（后端 Mock 已实现）、师生关系绑定、用户主页。
- AI 待补（后端侧）：聊天历史 `/ai/chat/history`、分析结果 `/ai/analysis/{id}` —— 需后端设计数据表；前端 `ai.js` 已预留封装，勿删。（`/ai/chat`、`/ai/chat/stream` 后端已实现）
- 待办（后端侧，非阻断）：密码明文→BCrypt、读写分离、AI 异步化、支付接入真实支付宝/微信 SDK。（`question_bank.knowledge_point_ids` CSV 列已建中间表 `question_knowledge_point` 作筛题来源，见下方性能优化节；CSV 列后续可删。）
- **学习资源后端已实现**（`/resource/*` 教材→章节→小节→资源 4 级 + 13 接口，见 §六）：管理员端 `/admin/resources` 与老师端「数学学科管理 → 学习资源」共用 `web/src/components/ResourceUploadPanel.vue`（学科选择[管理员端有 / 老师端固定数学] + 教材→章节→小节 三级级联 + 逐级「新增」+ 类型[课件/学案/作业/试卷] + 年份 + 定价 + 末级批量上传）。**定价**：管理员端统一 200 资源点；老师端三档（100/300/500）。`resourceService.js` 优先走 API、后端就绪后自动切后端。**前端待办**：ResourcesView 左侧目录树仍写死 mock（苏科/人教等），需改为读 `/resource/*` 目录接口。
- **收益策略（分成→智学点，提现隐藏保留）**：老师上传资源被下载时，50% 分成直接经 `pointService.charge(uploaderId, price/2, "gift", …)` 发放智学点（**不再写 `teacher_earnings` 表**）。提现功能**隐藏但保留**：前端 TeacherLayout.vue「收益中心」、AdminLayout.vue「提现审核」两处菜单已注释，后端 4 接口（§六）、`teacher_earnings` / `withdraw_requests` 表、TeacherRevenueService、RevenueController 全部保留（`teacher_earnings` 目前无写入、余额恒为 0）。**若重新开放提现**：① 取消两处前端菜单注释；② 把 `downloadResource` 里的 `pointService.charge(...)` 改回写 `teacher_earnings` 表。下载扣费：学生首次下载付费资源扣 `price` 智学点，余额不足返回 HTTP 400 + JSON（`{code,message}`）；前端用原生 `fetch` 按 `content-type` 判断并弹 `message`——**勿用 axios `responseType:'blob'`**（4xx 会被 error 拦截器 reject、拿不到 `res.data`）。

- **性能优化（本轮已完成，联调前必看）**：
  - 文件下载流式化：`/resource/resources/{id}/download`、`/knowledge-points/resources/{id}/download` 由整文件读内存改 `FileSystemResource` 流式输出（响应语义不变，仅多 `Content-Length`），前端下载逻辑无需改。
  - 连接池：HikariCP `maximum-pool-size` 20→50、`minimum-idle` 5→10。
  - 题库图片 base64 → 落盘 URL：`original_image_url`/`diagram_image_url`/`teacher_analysis_image` 三列不再存 base64，改存 `/uploads/question-images/**` URL（写入侧自动落盘，读侧 `/uploads/**` 反代直接渲染，`<img src>` 用法不变）。**审计点**：确认前端无 `startsWith('data:')` 等对这三字段的 base64 假设。
  - 共享题进度隔离：共享新题的 `mastery`/`completed` 改存新表 `student_question_progress`（`student_id+question_id` 隔离，不再互相覆盖）；`PUT /student/questions/{id}/mastery` 对共享题不再写 `answer`；私有错题仍写题行，VO 契约不变。
  - 知识点筛题改中间表：按知识点筛题由 CSV 列 `LIKE` 全表扫描 → 新表 `question_knowledge_point` 索引精确匹配（`5` 不误中 `15/25`）；`QuestionVO.knowledgePointIds/knowledgePointNames` 契约不变（CSV 列暂保留作展示缓存）。
  - 资源目录缓存：`/resource/textbooks|chapters|sections` 三个列表加 Redis 缓存，写操作级联失效。
  - **联调前执行**：① 跑 `docs/sql/migrate-question-progress.sql`、`docs/sql/migrate-question-knowledge-point.sql`（建表 + 回填存量）；② 图片存量迁移一次性启动 `--eduai.question-image-migrate=true`（幂等，迁移后去掉该参数）。

## 十二、文档

`docs/spec/`：`01-requirements` · `02-architecture` · `03-api-spec` · `04-database-auth` · `04-database-english` · `05-database-student` · `06-db-fix-guide` · `07-database-questionbank`
`docs/sql/`：`08-resource-catalog`（学习资源目录种子 SQL） · `09-resource-file`（学习资源文件表 DDL）
