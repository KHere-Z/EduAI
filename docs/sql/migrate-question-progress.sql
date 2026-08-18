-- ============================================================
-- EduAI 学生题目掌握度 — 共享题进度按学生隔离
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: question_bank.mastery/completed 直接挂在题行上，共享新题被多个
--       学生练习时会互相覆盖进度。本表按 (student_id, question_id) 隔离。
-- ============================================================

USE `eduai`;

CREATE TABLE IF NOT EXISTS `student_question_progress` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_id`  BIGINT       NOT NULL COMMENT '学生ID → students.id',
    `question_id` BIGINT       NOT NULL COMMENT '题目ID → question_bank.id',
    `mastery`     ENUM('UNMASTERED','FAMILIAR','MASTERED') DEFAULT 'UNMASTERED' COMMENT '掌握度',
    `completed`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否完成',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_question` (`student_id`, `question_id`),
    INDEX `idx_question` (`question_id`),
    CONSTRAINT `fk_sqp_student`  FOREIGN KEY (`student_id`)  REFERENCES `students`(`id`)      ON DELETE CASCADE,
    CONSTRAINT `fk_sqp_question` FOREIGN KEY (`question_id`) REFERENCES `question_bank`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题目掌握度(共享题按学生隔离)';
