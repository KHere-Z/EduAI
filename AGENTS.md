# EduAI — 项目上下文

> 喂给 IDEA 中的 Claude，让后端 AI 理解全局

## 一、项目是什么

**全学科 AI 智能教育平台**。覆盖 9 大学科（语文/数学/英语/物理/化学/生物/历史/政治/地理）的在线教育系统。

- 前端：Vue 3 + Element Plus（`/web`，VS Code 编写）
- 后端：Spring Boot 3.4.5 + JDK 21（`/server`，IDEA 编写）
- 同一仓库：`https://github.com/KHere-Z/EduAI`

## 二、当前进度（7 步流程）

| 步骤 | 状态 | 文档 |
|------|------|------|
| ① 需求文档 & 技术栈 | ✅ 完成 | `docs/spec/01-requirements.md` |
| ② 项目架构设计 | ✅ 完成 | `docs/spec/02-architecture.md` |
| ③ 页面设计 & 前端编写 | ⏳ 下一步 | — |
| ④ 数据库设计与建立 | ⏳ | — |
| ⑤ 后端设计与编写 | ⏳ | — |
| ⑥ 配置 AI 服务 | ⏳ | — |
| ⑦ 部署上线 | ⏳ | — |

## 三、用户角色

```
admin  (平台管理员) — 全平台管理
agency (机构管理员) — 本校师生、排课、成绩
teacher(教练/教师)  — 课堂、作业、AI 反馈
student(学生)       — 学习、错题、AI 分析
```

## 四、后端项目结构（本次重点）

```
server/
├── pom.xml                    ← Spring Boot 3.4.5 父 POM
├── eduai-system/              ← 🔥 启动模块（EduAIApplication，端口8080）
├── eduai-common/              ← 工具类/BaseEntity/Result
├── eduai-security/            ← Sa-Token 认证鉴权
├── eduai-file/                ← 文件上传/OSS/OCR
├── eduai-ai/                  ← DeepSeek API 客户端
├── eduai-statistics/          ← 成绩统计/报表
├── eduai-subject-common/      ← 学科通用 CRUD 模板
├── eduai-subject-english/     ← 英语（课堂/词汇/阅读/AI口语/语法）
├── eduai-subject-chinese/     ← 语文（文言文/作文）
├── eduai-subject-math/        ← 数学（几何/公式 LaTeX）
├── eduai-subject-physics/     ← 物理（实验模拟）
├── eduai-subject-chemistry/   ← 化学（方程式配平）
├── eduai-subject-biology/     ← 生物（细胞结构）
├── eduai-subject-history/     ← 历史（时间轴）
├── eduai-subject-politics/    ← 政治（时政分析）
└── eduai-subject-geography/   ← 地理（地图交互）
```

### 包内分层（以 math 为例）

```
com.eduai.subject.math/
├── controller/   → REST 接口
├── service/      → 业务逻辑
├── repository/   → JPA 数据访问
├── entity/       → 数据库实体
├── dto/          → 入参对象
└── vo/           → 出参对象
```

### 核心依赖

| 依赖 | 用途 |
|------|------|
| Sa-Token 1.40 | 认证鉴权（比 Spring Security 轻量） |
| MyBatis-Plus 3.5.10 | ORM（复杂查询） |
| Knife4j 4.5 | API 文档 `http://localhost:8080/doc.html` |
| MapStruct 1.6.3 | Entity ↔ DTO ↔ VO |
| Hutool 5.8.35 | Java 工具集 |
| EasyExcel 4.0.3 | Excel 导入导出 |

## 五、每个学科 9 个通用功能

```
1. 错题整理      WrongQuestion     POST/GET /api/v1/{subject}/wrong-questions
2. 错题分析      WrongAnalysis     POST /api/v1/{subject}/wrong-questions/{id}/analysis
3. 知识点整理    KnowledgePoint    GET  /api/v1/{subject}/knowledge-points
4. 考点整理      ExamPoint         GET  /api/v1/{subject}/exam-points
5. 解题模型      SolutionModel     GET  /api/v1/{subject}/solution-models
6. 题库          Question          GET  /api/v1/{subject}/questions
7. AI课堂反馈    AIFeedback        POST /api/v1/{subject}/ai-feedback
8. AI综合分析    AIAnalysis        GET  /api/v1/{subject}/ai-analysis/{studentId}
9. 成绩统计      ScoreStatistics   GET  /api/v1/{subject}/score-statistics
```

## 六、数据库 11 张核心表

```
User / Organization / UserRole
WrongQuestion (student_id, subject, error_type, knowledge_point_id)
KnowledgePoint (subject, parent_id, name, grade_level)
ExamPoint (subject, frequency, weight)
Question (subject, type, difficulty, content, answer)
ScoreRecord (student_id, subject, score, rank)
AIAnalysis (student_id, subject, radar_data JSON)
Classroom (teacher_id, subject, student_ids JSON)
SolutionModel (subject, name, template)
```

## 七、英语学科特殊说明

参照 `airunword.com` 架构，但**移除蓝思值（Lexile）接口**。保留：
- 课堂管理 / 词汇测试 / 词汇记忆 / 核心词汇
- AI 阅读理解 / AI 情境口语 / 语法体系
- 深度阅读 / 造句练习 / 阅读课堂 / 学习反馈

## 八、前端协作约定

- 前端端口：`localhost:5173`
- 后端端口：`localhost:8080`
- API 前缀：`/api/v1/{subject}/...`
- 认证方式：`Authorization: Bearer <token>`（Header）
- 响应格式：`{ code: 200, message: "success", data: {...} }`

## 九、下一步工作（第三步 — 前端）

1. 重构路由加入动态学科路由 `/coach/subject/:subject/*`
2. 创建 9 个通用组件（复用 `route.params.subject` 区分数据源）
3. 数学引入 KaTeX，图表引入 ECharts
4. 英语页面去掉蓝思值
