-- ============================================================
-- EduAI 题库字段扩容 — title + answer 改为 MEDIUMTEXT
-- 支持 base64 图片嵌入
-- ============================================================

USE `eduai`;

ALTER TABLE question_bank MODIFY COLUMN title MEDIUMTEXT;
ALTER TABLE question_bank MODIFY COLUMN answer MEDIUMTEXT;
