-- ============================================================
-- EduAI 学习反馈系统
-- ============================================================

USE `eduai`;

CREATE TABLE IF NOT EXISTS `feedbacks` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_id` BIGINT       NOT NULL COMMENT '老师用户ID',
    `student_id` BIGINT       NOT NULL COMMENT '学生ID',
    `subject`    VARCHAR(20)  COMMENT '学科',
    `period`     VARCHAR(100) COMMENT '时段标注，如: 7月第2周 / 期末总结',
    `content`    TEXT         NOT NULL COMMENT '反馈正文',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习反馈';
