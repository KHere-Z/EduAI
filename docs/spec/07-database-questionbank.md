# 07 — 题库 & 知识点数据库设计 (v2)

> **数据库**: MySQL 8.0 · 引擎 InnoDB  
> **前端**: 学生端题库 + 老师端数学学科管理

---

## 一、ER 图

```
┌──────────────────────────────┐
│      knowledge_points         │  全老师共享
│──────────────────────────────│
│ id           BIGINT PK       │
│ subject      VARCHAR(20)     │  数学/英语...
│ name         VARCHAR(100)    │  知识点名称
│ grade_level  VARCHAR(50)     │  年级·学期
│ parent_id    BIGINT FK ──────│──→ knowledge_points.id (树形)
│ sort_order   INT             │
│ created_at   DATETIME        │
│ updated_at   DATETIME        │
└──────────────────────────────┘
         │
         │ 1:N (关联知识点)
         ▼
┌──────────────────────────────┐
│       question_bank           │
│──────────────────────────────│
│ id              BIGINT PK    │
│ subject         VARCHAR(20)  │
│ type            ENUM         │  WRONG / NEW
│ source          ENUM         │  STUDENT / TEACHER
│ student_id      BIGINT FK ───│──→ students.id (错题专属)
│ teacher_id      BIGINT FK ───│──→ users.id (上传老师)
│ title           TEXT         │  AI识别文字
│═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═│
│ original_image_url  VARCHAR  │  原上传图片 (学生/老师拍照)
│ diagram_image_url   VARCHAR  │  配图 (AI截取或老师手动/画图)
│ diagram_status      ENUM     │  NONE / AUTO / MANUAL
│═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═│
│ ai_extracted_text   TEXT     │  AI识别原始文字
│ knowledge_point_ids VARCHAR  │  "3,7,12" 逗号分隔
│═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═│
│ answer             TEXT      │  正确答案
│ analysis           TEXT      │  AI错因分析
│ solution           TEXT      │  AI解题步骤
│ similar_json       JSON      │  举一反三题目
│═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═│
│ teacher_analysis   TEXT      │  老师文字解析
│ teacher_analysis_image VARCHAR│ 老师解析配图 (PNG/JPG本地文件)
│═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═ ═│
│ difficulty         ENUM      │  EASY / MEDIUM / HARD
│ mastery            ENUM      │  UNMASTERED / FAMILIAR / MASTERED
│ error_type         VARCHAR   │  错误类型 (错题)
│ grade_level        VARCHAR   │  年级·学期
│ created_at         DATETIME  │
│ updated_at         DATETIME  │
└──────────────────────────────┘

关系说明:
  ● students 1:N question_bank (错题)
  ● users 1:N question_bank (新题上传者)
  ● knowledge_points N:M question_bank (通过 knowledge_point_ids)
  ● 配图流程: AI自动截取原图→diagram_status=AUTO, 老师画图/上传覆盖→diagram_status=MANUAL
```

---

## 二、知识点表（knowledge_points）

> 老师端统一管理，**全老师账号共享**，增删改立即全平台生效

```sql
CREATE TABLE knowledge_points (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subject VARCHAR(20) NOT NULL COMMENT '学科',
  name VARCHAR(100) NOT NULL COMMENT '知识点名称',
  grade_level VARCHAR(50) COMMENT '年级·学期',
  parent_id BIGINT COMMENT '父知识点(树形结构)',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_subject_name (subject, name),
  INDEX idx_subject_grade (subject, grade_level),
  FOREIGN KEY (parent_id) REFERENCES knowledge_points(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点(全平台共享)';
```

---

## 三、题库表（question_bank）v2

```sql
CREATE TABLE question_bank (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subject VARCHAR(20) NOT NULL COMMENT '学科',
  type ENUM('WRONG','NEW') NOT NULL COMMENT '错题/新题',
  source ENUM('STUDENT','TEACHER') NOT NULL COMMENT '上传来源',
  student_id BIGINT COMMENT '错题专属学生',
  teacher_id BIGINT COMMENT '新题上传老师',

  -- 图片相关
  original_image_url VARCHAR(500) COMMENT '原上传图片URL',
  diagram_image_url VARCHAR(500) COMMENT '配图(AI截取/老师手动上传/画图)',
  diagram_status ENUM('NONE','AUTO','MANUAL') DEFAULT 'NONE' COMMENT '配图状态',

  -- AI 相关
  ai_extracted_text TEXT COMMENT 'AI从图片识别出的原始文字',
  title TEXT NOT NULL COMMENT '题目文字',
  knowledge_point_ids VARCHAR(500) COMMENT '关联知识点ID(逗号分隔,如"3,7,12")',

  -- 答案与解析
  answer TEXT COMMENT '正确答案',
  analysis TEXT COMMENT 'AI错因分析',
  solution TEXT COMMENT 'AI解题步骤',
  similar_json JSON COMMENT '举一反三题目',

  -- 老师解析
  teacher_analysis TEXT COMMENT '老师文字解析',
  teacher_analysis_image VARCHAR(500) COMMENT '老师解析配图(PNG/JPG)',
  teacher_analysis_image_type VARCHAR(20) COMMENT '文件类型(image/png或image/jpeg)',

  -- 分类
  difficulty ENUM('EASY','MEDIUM','HARD') DEFAULT 'MEDIUM',
  mastery ENUM('UNMASTERED','FAMILIAR','MASTERED') DEFAULT 'UNMASTERED',
  error_type VARCHAR(50) COMMENT '错误类型(错题)',
  grade_level VARCHAR(50) COMMENT '年级·学期',

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,

  INDEX idx_student (student_id),
  INDEX idx_subject_type (subject, type),
  INDEX idx_teacher (teacher_id),
  INDEX idx_grade_level (grade_level),
  INDEX idx_kp (knowledge_point_ids),
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';
```

---

## 四、API

### 知识点（全老师共享）
```
GET    /api/v1/knowledge-points?subject=math&gradeLevel=初三·上学期   ← 列表(分页15,按年级筛选)
POST   /api/v1/knowledge-points   { name, subject, gradeLevel }        ← 新增
PUT    /api/v1/knowledge-points/{id}                                      ← 修改
DELETE /api/v1/knowledge-points/{id}                                      ← 删除
```

### 题库（老师端管理）
```
GET    /api/v1/teacher/questions?subject=math&kpId=&type=&studentId=&gradeLevel=&date=&page=1&pageSize=15
GET    /api/v1/teacher/questions/{id}        ← 题目详情(含全部字段)
PUT    /api/v1/teacher/questions/{id}        ← 更新(知识点/老师解析/配图)
POST   /api/v1/teacher/questions/upload      ← 老师上传新题(全校共享)
DELETE /api/v1/teacher/questions/{id}        ← 删除
```

### 学生端
```
GET  /api/v1/student/questions/wrong?subject=math&page=1    ← 我的错题库
GET  /api/v1/student/questions/new?subject=math&gradeLevel=  ← 待做题(新题)
GET  /api/v1/knowledge-points?subject=math&grades=初一·上学期,初一·下学期,...   ← 按多学期查询
```

---

## 五、字段说明

| 字段 | 类型 | 说明 | 前端使用 |
|------|------|------|----------|
| original_image_url | VARCHAR | 原拍照/截图 | 老师端详情页左侧展示 |
| diagram_image_url | VARCHAR | 配图 | AI截取→老师可覆盖 |
| diagram_status | ENUM | NONE/AUTO/MANUAL | 表格中标签显示 |
| teacher_analysis | TEXT | 老师文字解析 | 老师端详情页输入 |
| teacher_analysis_image | VARCHAR | 老师解析配图 | 本地上传 PNG/JPG |
| knowledge_point_ids | VARCHAR | "3,7,12" | JOIN 查询知识点名称 |
