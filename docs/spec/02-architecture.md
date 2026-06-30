# 02 — 项目架构设计

> **项目**: EduAI · 版本: v1.0 · 日期: 2026-06-30

---

## 一、总体架构

```
┌─────────────────────────────────────────────────────────┐
│                      用户层                              │
│    学生端         教师端        机构端       平台管理端    │
└──────┬──────────────┬──────────────┬──────────────┬──────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────┐
│                     Nginx 反向代理                       │
│              (静态资源 + API 路由 + HTTPS)                │
├──────────────────────┬──────────────────────────────────┤
│   前端 (Vue 3 SPA)    │   后端 (Spring Boot REST API)     │
│   localhost:5173      │   localhost:8080                  │
│                       │   /api/v1/*                       │
└───────────────────────┴──────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
               MySQL 8.0       Redis 7          阿里云 OSS
              (业务数据)       (缓存/会话)      (图片/文件)
                                    │
                                    ▼
                            DeepSeek API
                       (AI 分析/举一反三/反馈)
```

## 二、前端架构

### 2.1 路由树

```
/                            → 首页
/login                       → 登录
/register                    → 注册

/coach                       → 教练布局 (CoachLayout)
├── /dashboard               → 工作台
├── /classroom               → 课堂管理
├── /completed-classes       → 已完成课堂
├── /vocab-test              → 词汇测试
├── /word-memorize           → 单词记忆
├── /core-words              → 核心词汇
├── /reading-classroom       → 阅读课堂
├── /core1000-classroom      → 核心1000词课堂
├── /ai-reading              → AI 阅读理解
├── /deep-reading            → 深度阅读
├── /book-preview            → 书籍预览
├── /ai-dialogue             → AI 情境口语
├── /grammar                 → 语法体系
├── /sentence-practice       → 造句练习
├── /lexile (已移除)         → ~~蓝思值报告~~
├── /feedback                → 学习反馈
├── /tools                   → 工具箱
├── /student-reading         → 学生阅读数据
│
├── /subject/:subject        → 学科通用 (动态路由)
│   ├── /wrong-questions     → 错题整理
│   ├── /wrong-analysis      → 错题分析 + 举一反三
│   ├── /knowledge-points    → 知识点整理
│   ├── /exam-points         → 考点整理
│   ├── /solution-models     → 解题模型
│   ├── /question-bank       → 题库
│   ├── /ai-feedback         → AI 课堂反馈
│   ├── /ai-analysis         → AI 综合分析
│   └── /score-statistics    → 成绩统计

/agency                      → 机构布局 (AgencyLayout)
├── /dashboard               → 机构工作台
├── /students                → 学员管理
├── /coaches                 → 教练管理
│   └── /:id                 → 教练详情
├── /courses                 → 课程管理
├── /schedule                → 排课管理
├── /crm                     → 销售中心
│   └── /:id                 → 线索详情
├── /records                 → 学习记录
├── /study-plan              → 学习规划
├── /feedback                → 学习反馈
├── /certificate             → 证书管理
├── /referral                → 转介绍
├── /poster                  → 宣传物料
└── /tools                   → 工具箱

/platform                    → 平台布局 (PlatformLayout)
├── /dashboard               → 平台概览
├── /organizations           → 机构管理
├── /coaches                 → 教练管理
├── /students                → 学员管理
├── /word-libraries          → 词库管理
├── /grammar-libraries       → 语法库管理
├── /book-libraries          → 书库管理
├── /referral                → 转介绍
└── /poster                  → 宣传物料
```

### 2.2 组件层次

```
App.vue
├── LoginView.vue
├── RegisterView.vue
├── HomeView.vue
├── DefaultLayout.vue
│   ├── CoachLayout.vue          (教练侧边栏)
│   │   ├── CoachDashboard.vue
│   │   ├── SubjectPage.vue      ← 学科通用页容器
│   │   │   ├── WrongQuestions.vue
│   │   │   ├── WrongAnalysis.vue
│   │   │   ├── KnowledgePoints.vue
│   │   │   ├── ExamPoints.vue
│   │   │   ├── SolutionModels.vue
│   │   │   ├── QuestionBank.vue
│   │   │   ├── AIFeedback.vue
│   │   │   ├── AIAnalysis.vue
│   │   │   └── ScoreStats.vue
│   │   └── ... (英语专属页面)
│   ├── AgencyLayout.vue         (机构侧边栏)
│   │   └── ...
│   └── PlatformLayout.vue       (平台侧边栏)
│       └── ...
└── NotFound.vue
```

### 2.3 状态管理 (Pinia)

```
stores/
├── auth.js          ← 用户认证 (token, user, role)
├── subject.js       ← 当前学科 & 年级 (全局共享)
├── wrongbook.js     ← 错题本 (跨页面共享)
├── questionBank.js  ← 题库状态
└── app.js           ← 全局设置 (侧边栏折叠等)
```

---

## 三、后端架构

### 3.1 Maven 模块依赖图

```
                    ┌─────────────┐
                    │ eduai-system │ ← 启动模块，依赖所有
                    └──────┬──────┘
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ eduai-security│  │ eduai-file   │  │ eduai-ai     │
│ (Sa-Token)   │  │ (OSS/OCR)    │  │ (DeepSeek)   │
└──────┬───────┘  └──────────────┘  └──────────────┘
       │
       ▼
┌──────────────┐        ┌─────────────────────────┐
│ eduai-common │◄───────│  eduai-subject-common    │
│ (工具/基础类) │        │  (错题/题库/知识点通用)    │
└──────────────┘        └──────────┬──────────────┘
                                   │
       ┌───────────────────────────┼───────────────────────┐
       ▼               ▼           ▼           ▼           ▼
┌────────────┐  ┌────────────┐  ┌────────────┐  ┌─────┐  ┌─...
│eduai-subject│  │eduai-subject│  │eduai-subject│  │  ...│  │
│ -english   │  │ -chinese   │  │ -math      │  │     │  │
└────────────┘  └────────────┘  └────────────┘  └─────┘  └─...

       ┌──────────────┐
       │eduai-statistics│ ← 依赖 subject-* 收集成绩数据
       └──────────────┘

依赖规则:
  ● 所有模块 → eduai-common
  ● 学科模块 → eduai-subject-common (继承通用 CRUD)
  ● eduai-system → 所有模块 (打包)
  ● eduai-security → 横向拦截，不依赖业务
  ● eduai-ai → eduai-common (不依赖学科，接收参数即可)
```

### 3.2 模块职责

| 模块 | 包路径 | 职责 |
|------|--------|------|
| `eduai-system` | `com.eduai.system` | 启动入口、全局配置、跨模块装配 |
| `eduai-common` | `com.eduai.common` | 基础类：Result、BaseEntity、异常、工具 |
| `eduai-security` | `com.eduai.security` | Sa-Token 配置、RBAC 注解、登录拦截器 |
| `eduai-file` | `com.eduai.file` | 文件上传、OSS 适配、OCR 调用 |
| `eduai-ai` | `com.eduai.ai` | DeepSeek 客户端、SSE 流式、Prompt 管理 |
| `eduai-statistics` | `com.eduai.statistics` | 成绩统计、报表生成、图表数据 |
| `eduai-subject-common` | `com.eduai.subject.common` | 错题/题库/知识点/考点/解题模型 CRUD 模板 |
| `eduai-subject-english` | `com.eduai.subject.english` | 英语：课堂/词汇/AI阅读/AI口语/语法 |
| `eduai-subject-chinese` | `com.eduai.subject.chinese` | 语文：文言文/作文/名篇 |
| `eduai-subject-math` | `com.eduai.subject.math` | 数学：几何/公式 |
| `eduai-subject-physics` | `com.eduai.subject.physics` | 物理：实验/动态图 |
| `eduai-subject-chemistry` | `com.eduai.subject.chemistry` | 化学：方程式/分子 |
| `eduai-subject-biology` | `com.eduai.subject.biology` | 生物：细胞/进化树 |
| `eduai-subject-history` | `com.eduai.subject.history` | 历史：时间轴/史料 |
| `eduai-subject-politics` | `com.eduai.subject.politics` | 政治：时政/法条 |
| `eduai-subject-geography` | `com.eduai.subject.geography` | 地理：地图/气候 |

### 3.3 包内分层（以 eduai-subject-math 为例）

```
com.eduai.subject.math/
├── controller/
│   ├── MathWrongQuestionController.java
│   ├── MathKnowledgePointController.java
│   ├── MathQuestionBankController.java
│   └── MathStatisticsController.java
├── service/
│   ├── MathWrongQuestionService.java
│   └── impl/
│       └── MathWrongQuestionServiceImpl.java
├── repository/
│   └── MathWrongQuestionRepository.java       (JPA)
├── entity/
│   ├── MathWrongQuestion.java
│   ├── MathKnowledgePoint.java
│   └── MathQuestion.java
├── dto/
│   ├── MathWrongQuestionDTO.java
│   └── MathAnalysisResultDTO.java
└── vo/
    ├── MathWrongQuestionVO.java
    └── MathScoreVO.java
```

---

## 四、API 设计

### 4.1 版本策略

```
/api/v1/...   ← 当前版本
/api/v2/...   ← 未来不兼容变更时启用
```

### 4.2 URL 命名规范

```
GET    /api/v1/{subject}/wrong-questions          列表
POST   /api/v1/{subject}/wrong-questions          新增
GET    /api/v1/{subject}/wrong-questions/{id}     详情
PUT    /api/v1/{subject}/wrong-questions/{id}     修改
DELETE /api/v1/{subject}/wrong-questions/{id}     删除

POST   /api/v1/{subject}/wrong-questions/{id}/analysis    AI 错题分析
GET    /api/v1/{subject}/wrong-questions/{id}/similar      举一反三

GET    /api/v1/{subject}/knowledge-points                 知识点树
GET    /api/v1/{subject}/exam-points                      考点列表
GET    /api/v1/{subject}/solution-models                  解题模型
GET    /api/v1/{subject}/questions                        题库检索
POST   /api/v1/{subject}/ai-feedback                      AI 课堂反馈
GET    /api/v1/{subject}/ai-analysis/{studentId}          AI 综合分析
GET    /api/v1/{subject}/score-statistics                 成绩统计
```

### 4.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1719676800000
}
```

### 4.4 分页响应

```json
{
  "code": 200,
  "data": {
    "list": [ ... ],
    "total": 156,
    "page": 1,
    "pageSize": 20
  }
}
```

---

## 五、数据库 ER 概要

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│   User   │────→│  UserRole    │     │  WrongQuestion   │
│──────────│     │──────────────│     │──────────────────│
│ id       │     │ user_id      │     │ id               │
│ username │     │ role (ADMIN/ │     │ student_id ──────│──→ User
│ password │     │  AGENCY/     │     │ subject          │
│ real_name│     │  TEACHER/    │     │ question_text    │
│ phone    │     │  STUDENT)    │     │ answer           │
│ org_id   │     └──────────────┘     │ wrong_answer     │
│ status   │                          │ error_type       │
└──────────┘                          │ knowledge_point_id│
      │                               │ difficulty       │
      │                               │ created_at       │
      ▼                               └──────────────────┘
┌──────────────┐                              │
│ Organization │                              ▼
│──────────────│     ┌──────────────────┐
│ id           │     │ KnowledgePoint   │
│ name         │     │──────────────────│
│ type         │     │ id               │
│ status       │     │ subject          │
└──────────────┘     │ parent_id (树)   │
                     │ name             │
                     │ grade_level      │
                     │ semester         │
                     └──────────────────┘
                              │
┌──────────────┐              ▼
│  Question    │     ┌──────────────────┐
│──────────────│     │  ExamPoint       │
│ id           │     │──────────────────│
│ subject      │     │ id               │
│ type         │     │ subject          │
│ difficulty   │     │ name             │
│ content      │     │ frequency (高频)  │
│ options      │     │ weight (分值)    │
│ answer       │     │ knowledge_point_id│
│ analysis     │     └──────────────────┘
│ kp_id ───────│──→ KnowledgePoint
└──────────────┘

┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ ScoreRecord  │     │  AIAnalysis      │     │  Classroom       │
│──────────────│     │──────────────────│     │──────────────────│
│ id           │     │ id               │     │ id               │
│ student_id   │     │ student_id       │     │ teacher_id       │
│ subject      │     │ subject          │     │ subject          │
│ exam_name    │     │ radar_data(JSON) │     │ name             │
│ score        │     │ weak_points(JSON)│     │ student_ids(JSON)│
│ total_score  │     │ suggestions(JSON)│     │ schedule         │
│ rank         │     │ created_at       │     │ status           │
│ exam_date    │     └──────────────────┘     └──────────────────┘
└──────────────┘

┌──────────────┐
│ SolutionModel│
│──────────────│
│ id           │
│ subject      │
│ name (函数模型)│
│ description  │
│ template     │
│ example_qid  │──→ Question
└──────────────┘
```

---

## 六、关键指引 → 第三步

> 📋 **第二步完成标志**: 前后端架构图、路由树、模块依赖、数据库 ER 概要已完成  
> ⏭️ **第三步**: 页面设计 → 前端编写
> - 按路由树逐个实现页面，优先教练端 `SubjectPage` 容器 + 9 学科通用功能
> - 英语学科页面在现有 airunword 结构上去掉蓝思值，对接新 API
> - 数学公式渲染引入 KaTeX，图表引入 ECharts
> 🔑 **第三步关键决策**: 学科通用页 `SubjectPage` 做成动态路由组件，`/coach/subject/:subject/*` 一个容器渲染 9 学科，避免 9×9=81 个页面爆炸

---

## 七、路由重构说明（第三步执行时使用）

当前 `web/src/router/index.js` 中的路由只覆盖英语学科。第三步需重构为：

```js
// 动态学科路由 — 一个路由处理所有学科通用功能
{
  path: '/coach/subject/:subject',
  component: () => import('@/views/coach/subject/SubjectLayout.vue'),
  children: [
    { path: 'wrong-questions', component: () => import('@/views/coach/subject/WrongQuestions.vue') },
    { path: 'wrong-analysis',  component: () => import('@/views/coach/subject/WrongAnalysis.vue') },
    { path: 'knowledge-points', component: () => import('@/views/coach/subject/KnowledgePoints.vue') },
    { path: 'exam-points',     component: () => import('@/views/coach/subject/ExamPoints.vue') },
    { path: 'solution-models', component: () => import('@/views/coach/subject/SolutionModels.vue') },
    { path: 'question-bank',   component: () => import('@/views/coach/subject/QuestionBank.vue') },
    { path: 'ai-feedback',     component: () => import('@/views/coach/subject/AIFeedback.vue') },
    { path: 'ai-analysis',     component: () => import('@/views/coach/subject/AIAnalysis.vue') },
    { path: 'score-statistics', component: () => import('@/views/coach/subject/ScoreStats.vue') },
  ]
}
```

路由 `/coach/subject/math/wrong-questions` 自动渲染数学错题页，`/coach/subject/physics/question-bank` 渲染物理题库——9 个学科复用同一套组件，通过 `route.params.subject` 区分数据源。
