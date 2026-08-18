-- ============================================================
-- EduAI 知识点资源 数据库迁移
-- 版本: v1
-- 适用: MySQL 8.0+
-- ============================================================

USE `eduai`;


-- ============================================================
-- 知识点资源表（学案/讲义/习题/试卷等文件）
-- ============================================================
CREATE TABLE IF NOT EXISTS `kp_resources` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kp_id`       BIGINT       NOT NULL COMMENT '知识点ID',
    `file_name`   VARCHAR(200) NOT NULL COMMENT '原文件名',
    `file_size`   BIGINT       COMMENT '文件大小(字节)',
    `file_url`    VARCHAR(500) NOT NULL COMMENT '存储路径',
    `file_type`   VARCHAR(20)  COMMENT '文件类型 pdf/docx/doc',
    `tag`         VARCHAR(10)  COMMENT '标签 学案/讲义/习题/试卷',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_kp` (`kp_id`),
    CONSTRAINT `fk_kpr_kp` FOREIGN KEY (`kp_id`) REFERENCES `knowledge_points`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点资源表';
