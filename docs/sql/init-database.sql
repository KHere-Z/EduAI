-- ============================================================
-- EduAI 数据库初始化脚本
-- 适用: MySQL 8.0+
-- 编码: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `eduai`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE `eduai`;

-- ============================================================
-- 1. 机构表
-- ============================================================
CREATE TABLE IF NOT EXISTS `organization` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(200) NOT NULL COMMENT '机构名称',
    `type`        TINYINT      DEFAULT 2 COMMENT '1=学校 2=培训机构',
    `address`     VARCHAR(500) COMMENT '地址',
    `contact`     VARCHAR(100) COMMENT '联系方式',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构表';

-- ============================================================
-- 2. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(200) NOT NULL COMMENT '密码（明文）',
    `real_name`   VARCHAR(50)  COMMENT '真实姓名',
    `phone`       VARCHAR(20)  COMMENT '手机号',
    `email`       VARCHAR(100) COMMENT '邮箱',
    `role_type`   TINYINT      NOT NULL DEFAULT 3 COMMENT '角色: 1=管理员 3=教师 4=学生',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态: 1=正常 0=禁用',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role_type` (`role_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 3. 教师信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `teachers` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT       NOT NULL COMMENT '关联用户ID',
    `subject_ids`  VARCHAR(500) NOT NULL DEFAULT 'math' COMMENT '任教学科，逗号分隔',
    `org_id`       BIGINT       COMMENT '所属机构ID',
    `title`        VARCHAR(100) COMMENT '职称',
    `bio`          TEXT         COMMENT '简介',
    `avatar`       VARCHAR(255) COMMENT '头像URL',
    `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_teacher_org`  FOREIGN KEY (`org_id`)  REFERENCES `organization`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师信息表';

-- ============================================================
-- 4. 测试数据
-- ============================================================

-- 机构
INSERT INTO `organization` (`id`, `name`, `type`) VALUES (1, '第一实验中学', 1);

-- 管理员 (密码: admin123，明文)
INSERT INTO `users` (`id`, `username`, `password`, `real_name`, `role_type`, `status`) VALUES
(1, 'admin', 'admin123', '系统管理员', 1, 1);

-- 教师 (密码均为用户名+123，明文)
INSERT INTO `users` (`id`, `username`, `password`, `real_name`, `role_type`, `status`) VALUES
(2, 'coach',  'coach123',   '李老师', 3, 1),
(3, 'english','english123', '王老师', 3, 1),
(4, 'math',   'math123',    '张老师', 3, 1),
(5, 'multi',  'multi123',   '陈老师', 3, 1),
(6, 'allsub', 'allsub123',  '赵老师', 3, 1);

-- 教师信息
INSERT INTO `teachers` (`id`, `user_id`, `subject_ids`, `org_id`, `title`) VALUES
(1, 2, 'math,physics',          1, '高级教师'),
(2, 3, 'english',               1, '一级教师'),
(3, 4, 'math',                  1, '二级教师'),
(4, 5, 'math,physics,chemistry',1, '特级教师'),
(5, 6, 'math,physics,chemistry,biology,chinese,english,geography,history,politics', 1, '全科教师');

-- ============================================================
-- 5. 学生表（v2: 全局唯一，管理员维护基本档案）
-- ============================================================
CREATE TABLE IF NOT EXISTS `students` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`      VARCHAR(10)  DEFAULT '男' COMMENT '性别',
    `contact`     VARCHAR(20)  COMMENT '联系方式',
    `grade`       VARCHAR(20)  COMMENT '年级',
    `school`      VARCHAR(100) COMMENT '所在学校',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 老师-学生关系表
CREATE TABLE IF NOT EXISTS `teacher_student` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_id`  BIGINT   NOT NULL COMMENT '老师ID',
    `student_id`  BIGINT   NOT NULL COMMENT '学生ID',
    `hours_left`  INT      DEFAULT 0 COMMENT '剩余课时',
    `reg_date`    DATE     COMMENT '报名时间',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_student` (`teacher_id`, `student_id`),
    INDEX `idx_teacher` (`teacher_id`),
    INDEX `idx_student` (`student_id`),
    CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_ts_student` FOREIGN KEY (`student_id`) REFERENCES `students`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老师-学生关系表';

-- 学生报名科目表（外键指向 teacher_student）
CREATE TABLE IF NOT EXISTS `student_enrollment` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_student_id`  BIGINT      NOT NULL COMMENT '老师-学生关系ID',
    `subject`             VARCHAR(50) NOT NULL COMMENT '科目',
    `created_at`          DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ts` (`teacher_student_id`),
    CONSTRAINT `fk_enrollment_ts` FOREIGN KEY (`teacher_student_id`) REFERENCES `teacher_student`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生报名科目';

-- 上课时间表
CREATE TABLE IF NOT EXISTS `student_session` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `enrollment_id`  BIGINT      NOT NULL COMMENT '报名科目ID',
    `class_date`     DATE        NOT NULL COMMENT '上课日期',
    `start_time`     VARCHAR(10) NOT NULL COMMENT '开始时间(HH)',
    `end_time`       VARCHAR(10) NOT NULL COMMENT '结束时间(HH)',
    PRIMARY KEY (`id`),
    INDEX `idx_enrollment` (`enrollment_id`),
    INDEX `idx_date` (`class_date`),
    CONSTRAINT `fk_session_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `student_enrollment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上课时间';

-- 测试数据: allsub(teacher_id=6) 添加张顺仪(英语+数学)
INSERT INTO `students` (`id`, `name`, `gender`, `contact`, `grade`, `school`) VALUES
(1, '张三', '男', '13800001111', '初一', '第一实验中学');

INSERT INTO `teacher_student` (`id`, `teacher_id`, `student_id`, `hours_left`, `reg_date`) VALUES
(1, 6, 1, 15, '2026-03-15');

INSERT INTO `student_enrollment` (`id`, `teacher_student_id`, `subject`) VALUES
(1, 1, '英语'), (2, 1, '数学');

INSERT INTO `student_session` (`enrollment_id`, `class_date`, `start_time`, `end_time`) VALUES
(1, '2026-07-03', '14', '16'),
(1, '2026-07-05', '14', '16'),
(2, '2026-07-03', '10', '12');






-- ============================================================
-- 6. 测试账号速查
-- ============================================================
-- | 用户名   | 密码      | 角色   | 学科              |
-- | admin   | admin123  | 管理员 | -                 |
-- | coach   | coach123  | 教师   | 数学、物理         |
-- | english | english123| 教师   | 英语              |
-- | math    | math123   | 教师   | 数学              |
-- | multi   | multi123  | 教师   | 数学、物理、化学    |
-- | allsub  | allsub123 | 教师   | 全科(9门)          |