-- ============================================================
-- EduAI 题目 AI 批改记录 — 幂等防重复扣点
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: 学生提交题目作答，后端调用 AI 批改并扣 5 智学点。一题一学生只批改
--       一次：重复提交直接返回缓存，不再扣点、不再调 AI。唯一键
--       (question_id, student_id) 作幂等键，兜底并发下的重复插入。
-- 说明: 生产 ddl-auto=validate，需在服务器手动执行本脚本。
-- ============================================================

USE `eduai`;
DROP TABLE IF EXISTS `question_grade_records`;
CREATE TABLE IF NOT EXISTS `question_grade_records` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`      BIGINT       NOT NULL COMMENT '题目ID → question_bank.id',
    `student_id`       BIGINT       NOT NULL COMMENT '学生ID → students.id',
    `answer_image_url` VARCHAR(1000)         DEFAULT NULL COMMENT '学生作答图片 URL（COS 或 /uploads/...）',
    `answer_text`      TEXT                  DEFAULT NULL COMMENT '学生作答文字（可选）',
    `correct`          TINYINT(1)            DEFAULT NULL COMMENT '批改结论：1=正确 0=错误',
    `result`           TEXT                  DEFAULT NULL COMMENT '批改说明',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_student` (`question_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目AI批改记录(幂等防重复扣点)';

