-- ============================================================
-- EduAI 试卷分析功能 数据库迁移
-- 创建 exam_papers 表（学生上传试卷 + AI 分析结果）
-- ============================================================

USE `eduai`;

-- ============================================================
-- 试卷表
-- ============================================================
CREATE TABLE IF NOT EXISTS `exam_papers` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`      BIGINT       NOT NULL COMMENT '学生ID',
    `subject`         VARCHAR(20)  COMMENT '学科',
    `exam_type`       VARCHAR(20)  COMMENT '考试类型 月考/期中/期末/模拟考',
    `school`          VARCHAR(100) COMMENT '学校',
    `paper_images`    TEXT         COMMENT '原卷图片URL（逗号分隔）',
    `wrong_questions` TEXT         COMMENT 'AI识别的错题（JSON）',
    `paper_analysis`  TEXT         COMMENT 'AI试卷分析',
    `suggestions`     TEXT         COMMENT '提分建议',
    `score`           DECIMAL(5,1) COMMENT '成绩',
    `kp_list`         TEXT         COMMENT '知识点清单',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_student_id` (`student_id`),
    INDEX `idx_student_subject` (`student_id`, `subject`),
    CONSTRAINT `fk_exam_paper_student` FOREIGN KEY (`student_id`) REFERENCES `students`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷分析表';

DROP TABLE IF EXISTS `exam_papers`;
