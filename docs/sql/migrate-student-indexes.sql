-- ============================================================
-- EduAI 性能优化 — students 表索引
-- 覆盖高频查询：姓名搜索、年级筛选、学校筛选
-- ============================================================

USE `eduai`;

-- 管理员/老师搜索学生按姓名
ALTER TABLE `students` ADD INDEX `idx_name` (`name`);

-- 年级筛选（管理员列表、学生端年级过滤）
ALTER TABLE `students` ADD INDEX `idx_grade` (`grade`);

-- 学校筛选（按姓名+学校去重）
ALTER TABLE `students` ADD INDEX `idx_school` (`school`);
