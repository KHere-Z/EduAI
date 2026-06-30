# 04-A — 用户认证数据库设计

> **数据库**: MySQL 8.0 · 引擎 InnoDB  
> **后端**: Spring Boot JPA + Sa-Token  
> **当前范围**: 老师角色（后续扩展学生/管理员）

---

## 一、ER 图

```
┌──────────────────────┐
│       users           │
│──────────────────────│
│ id          BIGINT PK│
│ username    VARCHAR  │  UNIQUE
│ password    VARCHAR  │  BCrypt 加密
│ real_name   VARCHAR  │
│ phone       VARCHAR  │
│ email       VARCHAR  │
│ role_type   TINYINT  │  1=admin 3=teacher 4=student
│ status      TINYINT  │  1=正常 0=禁用
│ created_at  DATETIME │
│ updated_at  DATETIME │
└──────────┬───────────┘
           │ 1:1
           ▼
┌──────────────────────┐
│      teachers         │
│──────────────────────│
│ id          BIGINT PK│
│ user_id     BIGINT FK│──→ users.id
│ subject_ids VARCHAR  │  "math,physics,chemistry"
│ org_id      BIGINT   │  所属机构
│ title       VARCHAR  │  职称（高级教师）
│ bio         TEXT     │  简介
│ avatar      VARCHAR  │  头像URL
│ created_at  DATETIME │
└──────────────────────┘

┌──────────────────────┐
│   organizations       │
│──────────────────────│
│ id          BIGINT PK│
│ name        VARCHAR  │
│ type        TINYINT  │  1=学校 2=培训机构
│ address     VARCHAR  │
│ contact     VARCHAR  │
│ status      TINYINT  │
│ created_at  DATETIME │
└──────────────────────┘
```

---

## 二、表关系

```
users ──1:1──→ teachers ──N:1──→ organizations
  │
  └── role_type: 1=admin  3=teacher  4=student

teachers.subject_ids: "math,physics"  → 前端据此过滤学科菜单
```

---

## 三、建表 SQL

```sql
-- 机构表
CREATE TABLE organization (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(200) NOT NULL COMMENT '机构名称',
  type TINYINT DEFAULT 2 COMMENT '1=学校 2=培训机构',
  address VARCHAR(500),
  contact VARCHAR(100),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构';

-- 用户表
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(200) NOT NULL COMMENT 'BCrypt加密',
  real_name VARCHAR(50) COMMENT '真实姓名',
  phone VARCHAR(20),
  email VARCHAR(100),
  role_type TINYINT NOT NULL DEFAULT 3 COMMENT '1=admin 3=teacher 4=student',
  status TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_role (role_type),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 老师表
CREATE TABLE teachers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE COMMENT '关联用户',
  subject_ids VARCHAR(500) NOT NULL DEFAULT 'math' COMMENT '任教学科，逗号分隔',
  org_id BIGINT COMMENT '所属机构',
  title VARCHAR(100) COMMENT '职称',
  bio TEXT COMMENT '简介',
  avatar VARCHAR(255) COMMENT '头像URL',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (org_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老师信息';

-- 插入测试数据
INSERT INTO organization (id, name, type) VALUES (1, '第一实验中学', 1);

-- 数学+物理老师
INSERT INTO users (username, password, real_name, role_type, status)
VALUES ('coach', '$2a$10$dummy_hash_coach123', '李老师', 3, 1);
INSERT INTO teachers (user_id, subject_ids, org_id, title)
VALUES (1, 'math,physics', 1, '高级教师');

-- 纯英语老师
INSERT INTO users (username, password, real_name, role_type, status)
VALUES ('english', '$2a$10$dummy_hash_english123', '王老师', 3, 1);
INSERT INTO teachers (user_id, subject_ids, org_id, title)
VALUES (2, 'english', 1, '一级教师');

-- 管理员
INSERT INTO users (username, password, real_name, role_type, status)
VALUES ('admin', '$2a$10$dummy_hash_admin123', '管理员', 1, 1);
```

---

## 四、Spring Boot 对应实体

```java
// Users.java
@Entity @Table(name = "users")
@Data
public class Users {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false, length = 200)
    private String password;  // BCrypt
    
    @Column(length = 50)
    private String realName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String email;
    
    @Column(nullable = false)
    private Integer roleType;  // 1=admin 3=teacher 4=student
    
    @Column(nullable = false)
    private Integer status = 1;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

// Teachers.java
@Entity @Table(name = "teachers")
@Data
public class Teachers {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne @JoinColumn(name = "user_id")
    private Users user;
    
    @Column(length = 500)
    private String subjectIds;  // "math,physics"
    
    @ManyToOne @JoinColumn(name = "org_id")
    private Organization organization;
    
    @Column(length = 100)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(length = 255)
    private String avatar;
}
```

---

## 五、登录 API

```
POST /api/v1/auth/login
{ "username": "coach", "password": "coach123" }

→ Sa-Token 校验密码
→ 查询 users JOIN teachers
→ 返回:
{
  "code": 200,
  "data": {
    "user": {
      "id": 1, "username": "coach", "real_name": "李老师",
      "role_type": 3, "status": 1,
      "subjects": ["math", "physics"],   ← 从 teachers.subject_ids 解析
      "org_id": 1, "title": "高级教师"
    },
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

**关键**: `subjects` 字段从 `teachers.subject_ids` 逗号分割转为 JSON 数组返回，前端据此渲染侧边栏。

---

## 六、当前前端模拟登录对照

前端 `auth.js demoLogin()` 中 `subjectMap` 是临时模拟。后端完成后替换为：

```javascript
// auth.js — 真登录
async function login(username, password) {
  const res = await loginAPI(username, password)
  // res.user.subjects = ["math", "physics"]  ← 后端返回
  token.value = res.token
  user.value = res.user
}
```

完全取代 `demoLogin`，无需其他改动。
