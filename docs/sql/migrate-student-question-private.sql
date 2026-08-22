-- ============================================================
-- EduAI 学生错题恒私有 — 修复存量数据
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: question_bank.shared 列有 DEFAULT 1（公域），而学生录入错题(addWrongQuestion)
--       历史上未显式写 shared，导致 source='STUDENT' 的错题被 DB 默认成了 shared=1，
--       即所有老师可见。本次让「学生错题」严格走 teacher_student 绑定关系，且恒私有。
-- 说明: 生产需手动执行本脚本，把存量学生错题统一改为 shared=0。
-- ============================================================

USE `eduai`;

UPDATE `question_bank`
SET `shared` = 0
WHERE `source` = 'STUDENT' AND (`shared` IS NULL OR `shared` = 1);
