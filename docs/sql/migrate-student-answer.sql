-- ============================================================
-- EduAI 学生答案存档（不批改、不扣点）— 建表
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: 前端「💾 不批改，直接保存」需要独立存档表，与 AI 批改记录
--       (question_grade_records) 分离。按 (question_id, student_id) 唯一、
--       覆盖式保存，只留最近一次作答。
-- 说明: 生产 ddl-auto=validate，需手动执行本脚本后再重启。
-- ============================================================

USE `eduai`;

DROP TABLE IF EXISTS `student_answers`;

CREATE TABLE `student_answers` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `question_id`      BIGINT       NOT NULL COMMENT '题目ID → question_bank.id',
    `student_id`       BIGINT       NOT NULL COMMENT '学生ID → students.id',
    `answer_image_url` VARCHAR(1000) DEFAULT NULL COMMENT '作答图片 URL（COS 或 /uploads/...）',
    `answer_text`      TEXT          DEFAULT NULL COMMENT '作答文字（可选）',
    `created_at`       DATETIME     NOT NULL,
    `updated_at`       DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_answer_question` (`question_id`, `student_id`),
    KEY `idx_student_answer_student` (`student_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生答案存档（只保存不批改）';
