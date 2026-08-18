-- ============================================================
-- EduAI 认证升级 数据库迁移
-- 手机号+短信验证码登录 + 微信开放平台绑定
-- 版本: v1
-- 适用: MySQL 8.0+
-- ============================================================

USE `eduai`;

-- ============================================================
-- 1. users 表新增字段
-- ============================================================
ALTER TABLE `users`
    ADD COLUMN `uid`           BIGINT       COMMENT '对外唯一ID(雪花算法)' AFTER `id`,
    ADD COLUMN `nickname`      VARCHAR(50)  COMMENT '昵称' AFTER `real_name`,
    ADD COLUMN `avatar`        VARCHAR(500) COMMENT '头像URL' AFTER `nickname`,
    ADD COLUMN `subjects`      JSON         COMMENT '学科数组 JSON' AFTER `avatar`,
    ADD COLUMN `student_uids`  JSON         COMMENT '教师专属-学生UID列表' AFTER `subjects`,
    ADD COLUMN `teacher_uid`   BIGINT       COMMENT '学生专属-老师UID' AFTER `student_uids`,
    ADD COLUMN `last_login`    DATETIME     COMMENT '最后登录时间' AFTER `updated_at`;

ALTER TABLE `users`
    ADD UNIQUE INDEX `idx_uid` (`uid`),
    ADD INDEX `idx_phone` (`phone`);

-- 手机号登录时 username / password 允许为 NULL
ALTER TABLE `users`
    MODIFY COLUMN `username` VARCHAR(50) NULL COMMENT '用户名',
    MODIFY COLUMN `password` VARCHAR(200) NULL COMMENT '密码（BCrypt 哈希）';

-- ============================================================
-- 2. 微信绑定表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_wechat` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_uid`    BIGINT       NOT NULL COMMENT '用户UID → users.uid',
    `openid`      VARCHAR(100) NOT NULL COMMENT '微信 openid',
    `unionid`     VARCHAR(100) COMMENT '微信 unionid（唯一索引）',
    `nickname`    VARCHAR(100) COMMENT '微信昵称',
    `avatar`      VARCHAR(500) COMMENT '微信头像URL',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_unionid` (`unionid`),
    INDEX `idx_user_uid` (`user_uid`),
    INDEX `idx_openid` (`openid`),
    CONSTRAINT `fk_wx_user` FOREIGN KEY (`user_uid`) REFERENCES `users`(`uid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户微信绑定表';
