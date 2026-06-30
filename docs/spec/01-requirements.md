# 01 — 需求文档与技术栈

> **项目名称**: 安文AI教育  
> **版本**: v1.0  
> **日期**: 2026-06-30  
> **预计用户**: 5,000+

---

## 一、项目概述

**安文AI教育** — 全学科 AI 智能学习平台。覆盖 9 大学科，3 个角色（老师/学生/管理员）。为教师和学生提供从错题管理到 AI 综合分析的全链路学习支持。

### 核心目标

| 维度 | 目标 |
|------|------|
| 学科覆盖 | 语文 / 数学 / 英语 / 物理 / 化学 / 生物 / 历史 / 政治 / 地理 |
| AI 赋能 | 错题举一反三、AI 课堂反馈、AI 综合分析、智能题库 |
| 规模支撑 | 5,000+ 并发用户 |
| 角色架构 | 老师端 / 学生端 / 管理员端 |

---

## 二、用户角色

| 角色 | role_type | 路由前缀 | 说明 |
|------|-----------|----------|------|
| 管理员 | 1 | `/admin` | 老师/学生/学科/题库管理 |
| 老师 | 3 | `/teacher` | 课堂管理、学科教学、AI 工具、学习反馈 |
| 学生 | 4 | `/student` | 学习中心、错题本、AI 分析、成绩 |

测试账号：`coach/123456`(数学+物理) `english/123456`(英语) `math/123456`(数学) `multi/123456`(三科) `admin/123456`(管理员)

---

## 三、学科功能矩阵

### 3.1 通用功能（每学科 9 大模块）

| # | 功能 | 路由 | 说明 |
|---|------|------|------|
| 1 | 错题整理 | `wrong-questions` | 表格+筛选+分页+CRUD |
| 2 | 错题分析 | `wrong-analysis` | AI 根因分析 + 举一反三 |
| 3 | 知识点整理 | `knowledge-points` | 五级树形结构 |
| 4 | 考点整理 | `exam-points` | 考频/分值/年份 |
| 5 | 解题模型 | `solution-models` | 卡片+弹窗模板 |
| 6 | 题库 | `question-bank` | 多条件筛选 |
| 7 | AI课堂反馈 | `ai-feedback` | 4指标+诊断报告 |
| 8 | AI综合分析 | `ai-analysis` | 雷达图+趋势+冲刺建议 |
| 9 | 成绩统计 | `score-statistics` | 核心指标+分布+明细 |

学科通过动态路由实现：`/teacher/subject/{subject}/{feature}` 渲染，9学科复用同一套组件。

### 3.2 英语学科（已完成前端）

参照 airunword.com 架构，**已移除蓝思值**。当前实现：

| 页面 | 路由 | 状态 |
|------|------|------|
| 课堂管理 | `/teacher/classroom` | ✅ |
| 词汇测试 | `/teacher/vocab-test` | ✅ |
| 单词记忆 | `/teacher/word-memorize` | ✅ |
| AI 阅读理解 | `/teacher/ai-reading` | ✅ |
| AI 情境口语 | `/teacher/ai-dialogue` | ✅ |
| 语法体系 | `/teacher/grammar` | ✅ |
| 造句练习 | `/teacher/sentence-practice` | ✅ |
| 学习反馈 | `/teacher/feedback` | ✅ |

---

## 四、当前实现状态

| 模块 | 进度 | 详情 |
|------|------|------|
| 前端框架 | ✅ | Vue 3 + Element Plus + Pinia + 设计系统 |
| 三角色路由 | ✅ | teacher/student/admin + 路由守卫 |
| 登录 | ✅ | 前后端联通，BCrypt 验证 |
| 老师端英语 | ✅ | 8 页面完整实现 |
| 学科通用 | ⏳ | 9 个通用组件已完成，待其余 8 学科 |
| 学生端 | ⏳ | 布局就绪，页面存根 |
| 管理员端 | ⏳ | 布局就绪，页面存根 |
| 后端认证 | ✅ | Sa-Token + BCrypt，users/teachers 表 |
| 英语数据库 | ✅ | 14 张表设计完成，待建表 |
| AI 集成 | ⏳ | DeepSeek 待接入 |

---

## 五、技术栈

### 5.1 前端（VS Code）

| 技术 | 用途 |
|------|------|
| Vue 3 + Vite | SPA 框架 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Vue Router | 路由 |
| Axios | HTTP 客户端 |
| ECharts（待引入） | 图表 |
| KaTeX（待引入） | 数学公式 |

### 5.2 后端 — Spring Boot（IDEA）

| 技术 | 用途 |
|------|------|
| JDK 21 + Spring Boot 3.4 | 框架 |
| Sa-Token + JWT | 认证鉴权 |
| Spring Data JPA + MyBatis-Plus | ORM |
| MySQL 8.0 | 数据库 |
| Knife4j | API 文档 |
| Hutool + Lombok + MapStruct | 工具链 |
| DeepSeek API（待接入） | AI 服务 |

### 5.3 项目结构

```
EduAI/
├── web/           ← VS Code 前端（Vue 3）
├── server/        ← IDEA 后端（Spring Boot 16模块）
│   ├── eduai-system/    启动模块
│   ├── eduai-security/  认证（已实现登录）
│   ├── eduai-common/    工具/Result
│   └── eduai-subject-*/ 9学科模块（待开发）
├── docs/spec/     ← 需求/架构/API/DB 文档
└── AGENTS.md      ← 共享上下文
```

---

## 六、术语表

| 术语 | 说明 |
|------|------|
| 错题整理 | 学生错题收集归类 |
| 举一反三 | AI 分析错误根因 + 同类题推荐 |
| 解题模型 | 标准化题型解法的抽象模板 |
| 蓝思值 | ~~英语阅读能力评分（已移除）~~ |
