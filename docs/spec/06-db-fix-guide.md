# 06 — 数据库修复指引

> **问题**: 同一学生报多科时产生重复学生记录；`hours_left` 放在 students 表不合理  
> **根因**: 表结构设计缺陷 — students 与 teacher 绑定过紧，课时维度错误

---

## 一、问题分析

### 当前结构（有问题）

```
students                           ← 绑了 teacher_id + hours_left
├── teacher_id      → 每个老师都要存一份学生，导致同一学生多份记录
├── hours_left      → 课时应该是"每个科目"的，不是"每个学生"的
├── grade/school    → 这些应该由管理员统一维护
└── reg_date        → 报名时间是 teacher-student 的关系属性
```

**导致的问题**:
1. allsub 添加张顺仪(英语) → students 插入一条 (teacher_id=6, hours_left=18)
2. allsub 再添加张顺仪(数学) → 又插入一条 (teacher_id=6, hours_left=18) ← **重复了**
3. coach 也添加张顺仪 → 又一条, teacher_id=2 ← **又重复**

### 修正后结构

```
students (全局唯一，管理员维护)
├── name, gender, contact, grade, school    ← 基本档案
└── (无 teacher_id, 无 hours_left)

teacher_student (老师-学生关系)
├── teacher_id, student_id, reg_date        ← 哪个老师收了哪个学生
└── hours_left                              ← 总剩余课时

student_enrollment (报名科目)
├── teacher_student_id, subject             ← 某学生的某科目
└── (可选: per_subject_hours)

student_session (排课时间)
├── enrollment_id, class_date, start_time, end_time
```

---

## 二、需要修改的表

### 2.1 students 表 — 精简

```sql
-- 移除 teacher_id, hours_left, reg_date
ALTER TABLE students DROP FOREIGN KEY students_ibfk_1;
ALTER TABLE students DROP COLUMN teacher_id;
ALTER TABLE students DROP COLUMN hours_left;
ALTER TABLE students DROP COLUMN reg_date;
```

### 2.2 新增 teacher_student 表

```sql
CREATE TABLE teacher_student (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  hours_left INT DEFAULT 0 COMMENT '剩余课时',
  reg_date DATE COMMENT '报名时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_student (teacher_id, student_id),
  FOREIGN KEY (teacher_id) REFERENCES users(id),
  FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老师-学生关系';
```

### 2.3 student_enrollment 表 — 修改外键

```sql
-- 改 student_id → teacher_student_id
ALTER TABLE student_enrollment DROP FOREIGN KEY student_enrollment_ibfk_1;
ALTER TABLE student_enrollment CHANGE student_id teacher_student_id BIGINT NOT NULL;
ALTER TABLE student_enrollment ADD FOREIGN KEY (teacher_student_id) REFERENCES teacher_student(id) ON DELETE CASCADE;
```

---

## 三、API 行为变更

### 旧
```
POST /students  → 创建 Student + Enrollment + Session（一条龙）
GET  /students  → 查 students WHERE teacher_id = ?
```
问题：每次 POST 都建新 student，不判断是否已存在。

### 新
```
POST   /teacher/students          → 给老师添加学生: INSERT teacher_student + enrollments + sessions
GET    /teacher/students          → 查 teacher_student JOIN students WHERE teacher_id = ?
DELETE /teacher/students/{tsId}   → 删除 teacher_student（级联删 enrollments 和 sessions）
```

**核心变化**: POST 不再创建 Student，而是创建 teacher_student 关系。Student 由管理员端维护。

---

## 四、StudentService 修改要点

```java
// 旧: 每次创建新 Student
Student student = Student.builder().teacherId(teacherId).name(dto.getName())...

// 新: 查找或创建 Student，然后创建 teacher_student 关系
Student student = studentRepository.findByNameAndSchool(dto.getName(), dto.getSchool())
    .orElseGet(() -> studentRepository.save(new Student(dto.getName(), ...)));

TeacherStudent ts = new TeacherStudent();
ts.setTeacherId(teacherId);
ts.setStudentId(student.getId());
ts.setHoursLeft(dto.getHoursLeft());
ts.setRegDate(dto.getRegDate());
ts.setEnrollments(buildEnrollments(dto.getEnrollments()));
teacherStudentRepository.save(ts);
```

---

## 五、前端对应调整（后端改完后再做）

| 前端 | 旧 | 新 |
|------|-----|-----|
| 添加按钮 | "添加报名" POST student | "添加学生" POST teacher_student |
| 学生列表 | students[].hoursLeft | teacherStudents[].hoursLeft |
| 学生选择器 | 搜 students | 搜系统所有 students（不按 teacher 过滤） |
| 课时显示 | row.hoursLeft | row.hoursLeft（仍在 TS 上） |

---

## 六、优先级

🔴 **P0**: 先建 `teacher_student` 表 + 改 `student_enrollment` 外键  
🔴 **P0**: 重写 `StudentService.create()` 逻辑  
🟡 **P1**: 更新 GET/DELETE 查询  
🟢 **P2**: 前端同步改字段路径
