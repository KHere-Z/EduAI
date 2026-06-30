# 03 — 接口文档 · 安文AI教育

> **读者**：后端开发同事  
> **前端地址**：`http://localhost:5173`  
> **后端地址**：`http://localhost:8080`  
> **API 前缀**：`/api/v1`  
> **认证方式**：`Authorization: Bearer <token>`  
> **日期**：2026-06-30

---

## 一、通用规范

### 1.1 响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 失败
{ "code": 400, "message": "参数错误", "data": null }

// 分页
{ "code": 200, "data": { "list": [...], "total": 156, "page": 1, "pageSize": 20 } }
```

### 1.2 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数校验失败 |
| 401 | 未认证 / Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.3 学科枚举

`subject` 路径参数取值为：

| 值 | 学科 |
|----|------|
| `chinese` | 语文 |
| `math` | 数学 |
| `english` | 英语 |
| `physics` | 物理 |
| `chemistry` | 化学 |
| `biology` | 生物 |
| `history` | 历史 |
| `politics` | 政治 |
| `geography` | 地理 |

---

## 二、认证模块

### 2.1 登录

```
POST /api/v1/auth/login
```

**请求体**
```json
{ "username": "coach", "password": "coach123" }
```

**响应**
```json
{
  "code": 200,
  "data": {
    "user": {
      "id": 150,
      "username": "coach",
      "real_name": "李教练",
      "role_type": 3,
      "org_id": 1,
      "org_name": "第一实验中学",
      "status": 1
    },
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**字段说明**
- `role_type`: 1=平台管理员, 2=机构管理员, 3=教师, 4=学生

### 2.2 获取当前用户

```
GET /api/v1/auth/me
```

### 2.3 注册

```
POST /api/v1/auth/register
{ "username": "...", "password": "...", "real_name": "...", "role_type": 3 }
```

### 2.4 退出

```
POST /api/v1/auth/logout
```

---

## 三、学科通用接口

所有 9 个学科复用以下接口。URL 中 `{subject}` 替换为学科枚举值。

### 3.1 错题整理

```
GET    /api/v1/{subject}/wrong-questions              # 列表（分页+筛选）
POST   /api/v1/{subject}/wrong-questions              # 新增
GET    /api/v1/{subject}/wrong-questions/{id}         # 详情
PUT    /api/v1/{subject}/wrong-questions/{id}         # 修改
DELETE /api/v1/{subject}/wrong-questions/{id}         # 删除
```

**查询参数（GET 列表）**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| keyword | string | 否 | 题目关键字搜索 |
| errorType | string | 否 | 错误类型：concept/calc/reading/careless |
| difficulty | string | 否 | 难度：easy/medium/hard |
| kpId | int | 否 | 关联知识点ID |

**请求体（POST/PUT）**
```json
{
  "title": "题目内容",
  "correctAnswer": "正确答案",
  "wrongAnswer": "学生错误答案",
  "errorType": "concept",
  "difficulty": "medium",
  "knowledgePointId": 32,
  "tags": ["高频", "易错"]
}
```

**响应（列表）**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "若 a²+b²=c²，a=3, b=4，求 c",
        "errorType": "calc",
        "errorTypeLabel": "计算失误",
        "difficulty": "easy",
        "difficultyLabel": "简单",
        "knowledgePointId": 11,
        "kpName": "勾股定理",
        "wrongAnswer": "c=7",
        "correctAnswer": "c=5",
        "studentId": 201,
        "studentName": "张三",
        "createdAt": "2026-06-28T10:30:00Z"
      }
    ],
    "total": 48,
    "page": 1,
    "pageSize": 20
  }
}
```

### 3.2 错题分析 + 举一反三

```
POST /api/v1/{subject}/wrong-questions/{id}/analysis   # AI 分析错题
GET  /api/v1/{subject}/wrong-questions/{id}/similar     # 获取举一反三题目
```

**POST analysis 请求体**
```json
{ "questionText": "原题内容", "wrongAnswer": "错误答案", "correctAnswer": "正确答案" }
```

**POST analysis 响应**
```json
{
  "code": 200,
  "data": {
    "rootCause": "概念混淆",
    "rootCauseTag": "danger",
    "analysis": "学生对二次函数顶点式概念理解有偏差...",
    "mastery": 42,
    "similarQuestions": [
      "已知二次函数 y=2(x-3)²+1，求顶点坐标。",
      "把 y=x²+6x+5 化为顶点式。",
      "已知顶点 (2,-3)，过点 (0,1)，求解析式。"
    ]
  }
}
```

### 3.3 知识点整理

```
GET  /api/v1/{subject}/knowledge-points                # 知识点树
GET  /api/v1/{subject}/knowledge-points/{id}          # 知识点详情
POST /api/v1/{subject}/knowledge-points               # 新增知识点
```

**响应（树形结构）**
```json
{
  "code": 200,
  "data": {
    "tree": [
      {
        "id": 1,
        "label": "数与代数",
        "mastery": 82,
        "gradeLevel": 8,
        "semester": "上",
        "unit": 1,
        "examPoints": "实数运算·代数式化简",
        "children": [
          { "id": 11, "label": "实数与运算", "mastery": 90 },
          { "id": 12, "label": "代数式", "mastery": 74 }
        ]
      }
    ]
  }
}
```

### 3.4 考点整理

```
GET /api/v1/{subject}/exam-points?frequency=high&year=2025   # 列表
```

**响应**
```json
{
  "data": {
    "list": [
      {
        "id": 1,
        "name": "勾股定理及其应用",
        "frequency": "近5年8次",
        "freqTag": "danger",
        "weight": "12-15分",
        "stars": 4,
        "years": [2021,2022,2023,2024,2025],
        "knowledgePointId": 11,
        "relatedQuestions": [101, 102, 103]
      }
    ]
  }
}
```

### 3.5 解题模型

```
GET    /api/v1/{subject}/solution-models              # 列表
GET    /api/v1/{subject}/solution-models/{id}         # 详情
```

**响应**
```json
{
  "data": {
    "list": [
      {
        "id": 1,
        "icon": "📐",
        "name": "函数模型",
        "description": "建立函数关系解决实际问题",
        "template": "1. 设未知数\n2. 列函数关系\n3. 利用性质求解\n4. 检验实际意义",
        "exampleCount": 12,
        "examples": [
          { "questionId": 201, "title": "某商品定价问题..." }
        ]
      }
    ]
  }
}
```

### 3.6 题库

```
GET    /api/v1/{subject}/questions                    # 列表（支持多条件筛选）
POST   /api/v1/{subject}/questions                    # 新增
GET    /api/v1/{subject}/questions/{id}               # 详情
```

**查询参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| keyword | string | 关键字搜索 |
| type | string | 题型：choice/fill/answer |
| difficulty | string | 难度：easy/medium/hard |
| kpId | int | 知识点ID |
| page/pageSize | int | 分页 |

**响应**
```json
{
  "data": {
    "list": [
      {
        "id": 1,
        "title": "在 Rt△ABC 中，∠C=90°，AC=3，BC=4，求 AB。",
        "type": "fill",
        "typeLabel": "填空题",
        "difficulty": "easy",
        "difficultyLabel": "简单",
        "answer": "5",
        "analysis": "根据勾股定理，AB² = AC² + BC² = 3² + 4² = 25",
        "knowledgePointId": 11,
        "kpName": "勾股定理",
        "options": null,
        "source": "历年真题"
      }
    ],
    "total": 156
  }
}
```

### 3.7 AI 课堂反馈

```
POST /api/v1/{subject}/ai-feedback                    # 生成课堂反馈
GET  /api/v1/{subject}/ai-feedback/{classId}          # 查看历史反馈
```

**POST 请求体**
```json
{ "classId": 101, "studentIds": [201,202,203], "lessonContent": "二次函数顶点式" }
```

**响应**
```json
{
  "data": {
    "knowledgeCoverage": 87,
    "studentEngagement": 76,
    "weakPointCount": 3,
    "classRating": "A",
    "feedbacks": [
      { "type": "warning", "icon": "Warning", "title": "薄弱点：二次函数最值", "content": "60%学生回答错误..." },
      { "type": "success", "icon": "SuccessFilled", "title": "掌握良好：定义域", "content": "92%正确率" }
    ]
  }
}
```

### 3.8 AI 综合分析

```
GET /api/v1/{subject}/ai-analysis/{studentId}?period=semester   # 学生综合分析
```

**响应**
```json
{
  "data": {
    "studentId": 201,
    "studentName": "张三",
    "subject": "math",
    "period": "semester",
    "radarData": { "代数": 85, "几何": 72, "函数": 52, "概率": 68, "统计": 78 },
    "trend": [ {"month":"3月","score":72}, {"month":"4月","score":75}, {"month":"5月","score":78} ],
    "diagnosis": "该生总体良好。二次函数掌握扎实(85%)，概率统计薄弱(52%)...",
    "suggestions": ["每日1道概率应用题", "每周2次函数专题", "考前做3套模拟卷"]
  }
}
```

### 3.9 成绩统计

```
GET /api/v1/{subject}/score-statistics?examId={examId}&scope=class
```

**响应**
```json
{
  "data": {
    "examName": "期末模拟一",
    "scope": "class",
    "summary": { "avg": 78.5, "max": 98, "min": 52, "stdDev": 12.3 },
    "distribution": [
      { "range": "90-100", "count": 8 },
      { "range": "80-89", "count": 15 },
      { "range": "70-79", "count": 12 },
      { "range": "60-69", "count": 7 },
      { "range": "0-59", "count": 3 }
    ],
    "details": [
      { "rank": 1, "studentId": 201, "name": "王小明", "score": 98, "trend": 5 }
    ]
  }
}
```

---

## 四、英语学科专用接口

英语学科除上述通用接口外，还有以下专属接口（参照 airunword.com）：

```
GET    /api/v1/english/classrooms                      # 课堂列表
POST   /api/v1/english/classrooms                      # 创建课堂
GET    /api/v1/english/classrooms/{id}                 # 课堂详情

GET    /api/v1/english/vocab-tests                      # 词汇测试列表
POST   /api/v1/english/vocab-tests                      # 创建词汇测试

GET    /api/v1/english/ai-reading                       # AI 阅读理解文章列表
POST   /api/v1/english/ai-reading/{id}/submit           # 提交阅读答案（AI 批改）

GET    /api/v1/english/ai-dialogue                      # AI 情境口语场景列表
POST   /api/v1/english/ai-dialogue/chat                 # 口语对话（SSE 流式）

GET    /api/v1/english/grammar                          # 语法知识点列表
GET    /api/v1/english/grammar/{id}                     # 语法详情

GET    /api/v1/english/deep-reading                     # 深度阅读文章列表
POST   /api/v1/english/completed-classes                # 已完成课堂记录
GET    /api/v1/english/learning-feedback/{studentId}    # 学生学习反馈

GET    /api/v1/english/vocab-books                      # 词库
GET    /api/v1/english/book-libraries                   # 书库
```

> ⚠️ 英语学科**无** Lexile（蓝思值）接口

---

## 五、机构端接口

```
GET    /api/v1/agency/dashboard                        # 机构仪表盘
GET    /api/v1/agency/students                         # 学员列表
POST   /api/v1/agency/students                         # 新增学员
GET    /api/v1/agency/coaches                          # 教练列表
GET    /api/v1/agency/coaches/{id}                     # 教练详情
GET    /api/v1/agency/courses                          # 课程列表
POST   /api/v1/agency/courses                          # 创建课程
GET    /api/v1/agency/schedule                         # 排课查询
POST   /api/v1/agency/schedule                         # 新增排课
GET    /api/v1/agency/crm/leads                        # 销售线索
GET    /api/v1/agency/crm/leads/{id}                   # 线索详情
GET    /api/v1/agency/records                          # 学习记录
GET    /api/v1/agency/study-plans                      # 学习规划
GET    /api/v1/agency/certificates                     # 证书管理
GET    /api/v1/agency/referrals                        # 转介绍
GET    /api/v1/agency/posters                          # 宣传物料
```

## 六、平台端接口

```
GET    /api/v1/platform/dashboard                      # 平台仪表盘
GET    /api/v1/platform/organizations                  # 机构列表
POST   /api/v1/platform/organizations                  # 新增机构
PUT    /api/v1/platform/organizations/{id}/status      # 审核机构
GET    /api/v1/platform/coaches                        # 教练列表
GET    /api/v1/platform/students                       # 学员列表
GET    /api/v1/platform/word-libraries                 # 词库管理
POST   /api/v1/platform/word-libraries                 # 新增词库
GET    /api/v1/platform/grammar-libraries              # 语法库
GET    /api/v1/platform/book-libraries                 # 书库
```

---

## 七、SSE 流式接口（AI 对话）

```
POST /api/v1/{subject}/ai/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

**请求体**
```json
{ "studentId": 201, "message": "什么是勾股定理？", "context": "正在学习直角三角形" }
```

**响应（SSE）**
```
data: {"type":"thinking","content":"正在分析问题..."}
data: {"type":"token","content":"勾"}
data: {"type":"token","content":"股"}
data: {"type":"token","content":"定"}
data: {"type":"done"}
```

---

## 八、后端实现优先级建议

| 优先级 | 模块 | 预估工时 |
|--------|------|----------|
| 🔴 P0 | 认证模块（auth） | 1天 |
| 🔴 P0 | 学科通用 CRUD（subject-common → 9学科） | 3天 |
| 🟡 P1 | AI 分析接口（ai） | 2天 |
| 🟡 P1 | 英语专属接口 | 2天 |
| 🟢 P2 | 机构端/平台端 | 3天 |
| 🟢 P2 | 成绩统计/报表 | 2天 |
