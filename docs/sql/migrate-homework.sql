-- ============================================================
-- EduAI 作业系统 数据库迁移
-- ============================================================

USE `eduai`;

CREATE TABLE IF NOT EXISTS `homework` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_id`      BIGINT       NOT NULL COMMENT '老师ID → users.id',
    `subject`         VARCHAR(20)  COMMENT '学科',
    `title`           VARCHAR(200) COMMENT '作业标题',
    `description`     TEXT         COMMENT '作业描述',
    `answer_file_url` TEXT         COMMENT '答案解析文件URL',
    `answer_file_name` VARCHAR(200) COMMENT '答案文件名',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_teacher_id` (`teacher_id`),
    INDEX `idx_teacher_subject` (`teacher_id`, `subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';

CREATE TABLE IF NOT EXISTS `homework_submissions` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `homework_id`          BIGINT       NOT NULL COMMENT '作业ID',
    `student_id`           BIGINT       NOT NULL COMMENT '学生ID',
    `submitted_image_url`  TEXT         COMMENT '学生上传的作业图片',
    `corrected_image_url`  TEXT         COMMENT '老师批改后的图片',
    `status`               VARCHAR(20)  DEFAULT 'pending' COMMENT 'pending/submitted/corrected',
    `submitted_at`         DATETIME     COMMENT '提交时间',
    `corrected_at`         DATETIME     COMMENT '批改时间',
    `created_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_hw_student` (`homework_id`, `student_id`),
    INDEX `idx_homework` (`homework_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';


