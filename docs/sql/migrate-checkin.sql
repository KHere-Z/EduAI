-- 学生打卡记录表（/student/checkin · /student/streak）
-- 开发环境由 JPA ddl-auto=update 自动建表；生产(validate)请手动执行本文件。
-- 若已用 init-database.sql 全量建库，则无需再执行本文件（表已包含在内）。

-- 1. 建表（新库用；含唯一键）
CREATE TABLE IF NOT EXISTS `student_checkin` (
  `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`   BIGINT NOT NULL COMMENT '学生ID（→ students.id）',
  `checkin_date` DATE   NOT NULL COMMENT '打卡日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sc_student_date` (`student_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生打卡记录';

-- 2. 老库升级：表已存在但缺唯一键 → 幂等补加（已存在则跳过）
--    若老库已有 (student_id, checkin_date) 重复行，需先手动去重，否则 ADD UNIQUE KEY 会报 Duplicate entry。
SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `student_checkin` ADD UNIQUE KEY `uk_sc_student_date` (`student_id`, `checkin_date`)',
    'SELECT ''uk_sc_student_date 已存在，跳过'' AS msg'
  )
  FROM information_schema.STATISTICS
  WHERE table_schema = DATABASE()
    AND table_name = 'student_checkin'
    AND index_name = 'uk_sc_student_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
