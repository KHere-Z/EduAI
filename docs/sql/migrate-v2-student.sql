-- ============================================================
-- 学生管理数据库迁移（修复多对多关系）
-- 执行方式: 在 MySQL 中手动执行此脚本
-- ⚠️ 会清空学生相关数据
-- ============================================================
USE `eduai`;

-- 1. 删除所有旧的 student 相关表（含外键约束）
DROP TABLE IF EXISTS `student_session`;
DROP TABLE IF EXISTS `student_enrollment`;
DROP TABLE IF EXISTS `students`;

-- 2. 重建 students 表（v2: 全局档案，去掉 teacher_id/hours_left/reg_date）
CREATE TABLE `students` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`      VARCHAR(10)  DEFAULT '男' COMMENT '性别',
    `contact`     VARCHAR(20)  COMMENT '联系方式',
    `grade`       VARCHAR(20)  COMMENT '年级',
    `school`      VARCHAR(100) COMMENT '所在学校',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 3. 新增老师-学生关系表
CREATE TABLE `teacher_student` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_id`  BIGINT   NOT NULL COMMENT '老师ID',
    `student_id`  BIGINT   NOT NULL COMMENT '学生ID',
    `hours_left`  INT      DEFAULT 0 COMMENT '剩余课时',
    `reg_date`    DATE     COMMENT '报名时间',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_student` (`teacher_id`, `student_id`),
    INDEX `idx_teacher` (`teacher_id`),
    INDEX `idx_student` (`student_id`),
    CONSTRAINT `fk_ts_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_ts_student` FOREIGN KEY (`student_id`) REFERENCES `students`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老师-学生关系表';

-- 4. 重建 student_enrollment（外键改为 teacher_student_id）
CREATE TABLE `student_enrollment` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_student_id`  BIGINT      NOT NULL COMMENT '老师-学生关系ID',
    `subject`             VARCHAR(50) NOT NULL COMMENT '科目',
    `created_at`          DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_ts` (`teacher_student_id`),
    CONSTRAINT `fk_enrollment_ts` FOREIGN KEY (`teacher_student_id`) REFERENCES `teacher_student`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生报名科目';

-- 5. 重建 student_session
CREATE TABLE `student_session` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `enrollment_id`  BIGINT      NOT NULL COMMENT '报名科目ID',
    `class_date`     DATE        NOT NULL COMMENT '上课日期',
    `start_time`     VARCHAR(10) NOT NULL COMMENT '开始时间(HH)',
    `end_time`       VARCHAR(10) NOT NULL COMMENT '结束时间(HH)',
    PRIMARY KEY (`id`),
    INDEX `idx_enrollment` (`enrollment_id`),
    INDEX `idx_date` (`class_date`),
    CONSTRAINT `fk_session_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `student_enrollment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上课时间';

-- 6. 测试数据: allsub(teacher_id=6) 添加张顺仪
INSERT INTO `students` (`id`, `name`, `gender`, `contact`, `grade`, `school`) VALUES
(1, '张顺仪', '男', '15161603880', '初一', '夏港中学');

INSERT INTO `teacher_student` (`id`, `teacher_id`, `student_id`, `hours_left`, `reg_date`) VALUES
(1, 6, 1, 15, '2026-06-11');

INSERT INTO `student_enrollment` (`id`, `teacher_student_id`, `subject`) VALUES
(1, 1, '英语'), (2, 1, '数学');

INSERT INTO `student_session` (`enrollment_id`, `class_date`, `start_time`, `end_time`) VALUES
(1, '2026-07-03', '14', '16'),
(1, '2026-07-04', '14', '16'),
(2, '2026-07-03', '10', '12');