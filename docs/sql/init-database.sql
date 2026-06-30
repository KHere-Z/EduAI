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
(5, 'multi',  'multi123',   '陈老师', 3, 1);

-- 教师信息
INSERT INTO `teachers` (`id`, `user_id`, `subject_ids`, `org_id`, `title`) VALUES
(1, 2, 'math,physics',          1, '高级教师'),
(2, 3, 'english',               1, '一级教师'),
(3, 4, 'math',                  1, '二级教师'),
(4, 5, 'math,physics,chemistry',1, '特级教师');

-- ============================================================
-- 5. 测试账号速查
-- ============================================================
-- | 用户名   | 密码      | 角色   | 学科              |
-- | admin   | admin123  | 管理员 | -                 |
-- | coach   | coach123  | 教师   | 数学、物理         |
-- | english | english123| 教师   | 英语              |
-- | math    | math123   | 教师   | 数学              |
-- | multi   | multi123  | 教师   | 数学、物理、化学    |