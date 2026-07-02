# 05 — 学生管理数据库设计

> **数据库**: MySQL 8.0 · 引擎 InnoDB  
> **前端**: `/teacher/students` 页面已就绪 · 字段完全对应  
> **后端**: 待实现 CRUD API

---

## 一、ER 图

```
┌──────────────────────┐
│      students         │
│──────────────────────│
│ id          BIGINT PK │
│ teacher_id  BIGINT FK │──→ users.id (老师)
│ name        VARCHAR   │
│ gender      VARCHAR   │  男/女
│ contact     VARCHAR   │
│ hours_left  INT       │  剩余课时 (前端±按钮控制)
│ grade       VARCHAR   │  一年级~高三
│ school      VARCHAR   │
│ reg_date    DATE      │  报名时间
│ created_at  DATETIME  │
│ updated_at  DATETIME  │
└──────────┬───────────┘
           │ 1:N
           ▼
┌──────────────────────┐
│ student_enrollment    │
│──────────────────────│
│ id          BIGINT PK │
│ student_id  BIGINT FK │──→ students.id
│ subject     VARCHAR   │  英语/数学/物理...
│ created_at  DATETIME  │
└──────────┬───────────┘
           │ 1:N
           ▼
┌──────────────────────┐
│ student_session       │
│──────────────────────│
│ id             BIGINT │
│ enrollment_id BIGINT  │──→ student_enrollment.id
│ class_date    DATE   │  上课日期 2026-07-03
│ start_time    VARCHAR│  开始时间 14 (小时)
│ end_time      VARCHAR│  结束时间 16 (小时)
└──────────────────────┘
```

---

## 二、建表 SQL

```sql
-- 1. 学生表
CREATE TABLE students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL COMMENT '所属老师',
  name VARCHAR(50) NOT NULL COMMENT '姓名',
  gender VARCHAR(10) DEFAULT '男' COMMENT '性别',
  contact VARCHAR(20) COMMENT '联系方式',
  hours_left INT DEFAULT 0 COMMENT '剩余课时',
  grade VARCHAR(20) COMMENT '年级',
  school VARCHAR(100) COMMENT '所在学校',
  reg_date DATE COMMENT '报名时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_teacher (teacher_id),
  FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- 2. 学生报名科目表（一个学生可报多科）
CREATE TABLE student_enrollment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL COMMENT '学生ID',
  subject VARCHAR(50) NOT NULL COMMENT '科目',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_student (student_id),
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生报名科目';

-- 3. 课程时间表（每科多个日期，每个日期一个时间段）
CREATE TABLE student_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enrollment_id BIGINT NOT NULL COMMENT '报名科目ID',
  class_date DATE NOT NULL COMMENT '上课日期',
  start_time VARCHAR(10) NOT NULL COMMENT '开始时间(HH)',
  end_time VARCHAR(10) NOT NULL COMMENT '结束时间(HH)',
  INDEX idx_enrollment (enrollment_id),
  INDEX idx_date (class_date),
  FOREIGN KEY (enrollment_id) REFERENCES student_enrollment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上课时间';
```

---

## 三、API 接口

### 3.1 学生 CRUD

```
GET    /api/v1/students?page=1&pageSize=20&keyword=&subject=&grade=
POST   /api/v1/students
GET    /api/v1/students/{id}
PUT    /api/v1/students/{id}
DELETE /api/v1/students/{id}
```

### 3.2 POST/PUT 请求体

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
    },
    {
      "subject": "数学",
      "sessions": [
        { "classDate": "2026-07-03", "startTime": "10", "endTime": "12" }
      ]
    }
  ]
}
```

### 3.3 GET 列表响应

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "name": "张三",
        "gender": "男",
        "contact": "13800001111",
        "hoursLeft": 15,
        "grade": "初一",
        "school": "第一实验中学",
        "regDate": "2026-03-15",
        "enrollments": [
          {
            "id": 1,
            "subject": "英语",
            "sessions": [
              { "id": 1, "classDate": "2026-07-03", "startTime": "14", "endTime": "16" },
              { "id": 2, "classDate": "2026-07-05", "startTime": "14", "endTime": "16" }
            ]
          }
        ]
      }
    ],
    "total": 28,
    "page": 1,
    "pageSize": 20
  }
}
```

### 3.4 课时加减

```
PATCH /api/v1/students/{id}/hours
{ "delta": -1 }   // 上完一节课 -1 或 +1
```

### 3.5 日历查询（某月所有排课）

```
GET /api/v1/students/calendar?year=2026&month=7&teacherId={teacherId}
```

**响应**
```json
{
  "code": 200,
  "data": {
    "dates": {
      "2026-07-03": [
        { "studentId": 1, "studentName": "张三", "subject": "英语", "startTime": "14", "endTime": "16" },
        { "studentId": 1, "studentName": "张三", "subject": "数学", "startTime": "10", "endTime": "12" }
      ],
      "2026-07-05": [
        { "studentId": 1, "studentName": "张三", "subject": "英语", "startTime": "14", "endTime": "16" }
      ]
    }
  }
}
```

---

## 四、字段完整清单

### students 表

| 字段 | 类型 | 必填 | 说明 | 前端对应 |
|------|------|------|------|----------|
| id | BIGINT | ✅ | 主键 | `id` |
| teacher_id | BIGINT | ✅ | 所属老师 | 从Token获取 |
| name | VARCHAR(50) | ✅ | 姓名 | `name` |
| gender | VARCHAR(10) | — | 性别 | `gender` ("男"/"女") |
| contact | VARCHAR(20) | — | 联系方式 | `contact` |
| hours_left | INT | — | 剩余课时 | `hoursLeft` (前端±按钮) |
| grade | VARCHAR(20) | — | 年级 | `grade` |
| school | VARCHAR(100) | — | 学校 | `school` |
| reg_date | DATE | — | 报名时间 | `regDate` |

### student_enrollment 表

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | BIGINT | ✅ | 主键 |
| student_id | BIGINT | ✅ | 关联学生 |
| subject | VARCHAR(50) | ✅ | 科目 (英语/数学/物理...) |

### student_session 表

| 字段 | 类型 | 必填 | 说明 | 前端对应 |
|------|------|------|------|----------|
| id | BIGINT | ✅ | 主键 | — |
| enrollment_id | BIGINT | ✅ | 关联科目 | — |
| class_date | DATE | ✅ | 上课日期 | `session.date` |
| start_time | VARCHAR(10) | ✅ | 开始小时 | `session.start` (HH) |
| end_time | VARCHAR(10) | ✅ | 结束小时 | `session.end` (HH) |

---

## 五、Spring Boot 实现要点

1. **Entity 三层嵌套**：Student → @OneToMany enrollments → @OneToMany sessions
2. **CascadeType.ALL + orphanRemoval**：删除学生自动删科目和排课
3. **更新时先删后插**：PUT 请求中 enrollments 传完整列表，后端删除旧的、插入新的
4. **老师隔离**：所有查询加 `WHERE teacher_id = ?`，从 Sa-Token 获取当前老师ID
5. **日历接口**：直接 JOIN 查 `student_session WHERE class_date BETWEEN ? AND ?`，按日期分组返回

## 六、数据示例

```sql
INSERT INTO students (id, teacher_id, name, gender, contact, hours_left, grade, school, reg_date) VALUES
(1, 2, '张三', '男', '13800001111', 15, '初一', '第一实验中学', '2026-03-15');

INSERT INTO student_enrollment (id, student_id, subject) VALUES
(1, 1, '英语'), (2, 1, '数学');

INSERT INTO student_session (enrollment_id, class_date, start_time, end_time) VALUES
(1, '2026-07-03', '14', '16'),
(1, '2026-07-05', '14', '16'),
(2, '2026-07-03', '10', '12');
```
