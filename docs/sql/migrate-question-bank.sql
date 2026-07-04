-- ============================================================
-- EduAI 题库 & 知识点 数据库迁移
-- 版本: v1
-- 适用: MySQL 8.0+
-- 参考: docs/spec/07-database-questionbank.md
-- ============================================================

USE `eduai`;

-- ============================================================
-- 1. 知识点表（全平台共享）
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_points` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject`     VARCHAR(20)  NOT NULL COMMENT '学科',
    `name`        VARCHAR(100) NOT NULL COMMENT '知识点名称',
    `grade_level` VARCHAR(50)  COMMENT '年级·学期',
    `parent_id`   BIGINT       COMMENT '父知识点(树形结构)',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_name` (`subject`, `name`),
    INDEX `idx_subject_grade` (`subject`, `grade_level`),
    CONSTRAINT `fk_kp_parent` FOREIGN KEY (`parent_id`) REFERENCES `knowledge_points`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点(全平台共享)';

-- ============================================================
-- 2. 题库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `question_bank` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject`           VARCHAR(20)  NOT NULL COMMENT '学科',
    `type`              ENUM('WRONG','NEW') NOT NULL COMMENT '错题/新题',
    `source`            ENUM('STUDENT','TEACHER') NOT NULL COMMENT '上传来源',

    `student_id`        BIGINT       COMMENT '错题专属学生',
    `teacher_id`        BIGINT       COMMENT '新题上传老师',

    -- 图片相关
    `original_image_url`         VARCHAR(500) COMMENT '原上传图片URL',
    `diagram_image_url`          VARCHAR(500) COMMENT '配图(AI截取/老师手动上传/画图)',
    `diagram_status`             ENUM('NONE','AUTO','MANUAL') DEFAULT 'NONE' COMMENT '配图状态',

    -- AI 相关
    `ai_extracted_text`          TEXT COMMENT 'AI从图片识别出的原始文字',
    `title`                      TEXT         NOT NULL COMMENT '题目文字',
    `knowledge_point_ids`        VARCHAR(500) COMMENT '关联知识点ID(逗号分隔,如"3,7,12")',

    -- 答案与解析
    `answer`                     TEXT COMMENT '正确答案',
    `analysis`                   TEXT COMMENT 'AI错因分析',
    `solution`                   TEXT COMMENT 'AI解题步骤',
    `similar_json`               JSON COMMENT '举一反三题目',

    -- 老师解析
    `teacher_analysis`           TEXT COMMENT '老师文字解析',
    `teacher_analysis_image`     VARCHAR(500) COMMENT '老师解析配图(PNG/JPG)',
    `teacher_analysis_image_type`VARCHAR(20)  COMMENT '文件类型(image/png或image/jpeg)',

    -- 分类
    `difficulty`                 ENUM('EASY','MEDIUM','HARD') DEFAULT 'MEDIUM',
    `mastery`                    ENUM('UNMASTERED','FAMILIAR','MASTERED') DEFAULT 'UNMASTERED',
    `error_type`                 VARCHAR(50)  COMMENT '错误类型(错题)',
    `grade_level`                VARCHAR(50)  COMMENT '年级·学期',

    `created_at`                 DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`                 DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_subject_type` (`subject`, `type`),
    INDEX `idx_teacher` (`teacher_id`),
    INDEX `idx_grade_level` (`grade_level`),
    INDEX `idx_kp` (`knowledge_point_ids`),
    CONSTRAINT `fk_qb_student` FOREIGN KEY (`student_id`) REFERENCES `students`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_qb_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库';

-- ============================================================
-- 3. 测试数据 — 知识点
-- ============================================================
INSERT INTO `knowledge_points` (`id`, `subject`, `name`, `grade_level`, `sort_order`) VALUES
(1,  'math', '有理数运算',       '初一·上学期', 1),
(2,  'math', '整式加减',         '初一·上学期', 2),
(3,  'math', '一元一次方程',     '初一·上学期', 3),
(4,  'math', '二元一次方程组',   '初一·下学期', 1),
(5,  'math', '不等式与不等式组', '初一·下学期', 2),
(6,  'math', '三角形',           '初二·上学期', 1),
(7,  'math', '全等三角形',       '初二·上学期', 2),
(8,  'math', '勾股定理',         '初二·下学期', 1),
(9,  'math', '一次函数',         '初二·下学期', 2),
(10, 'math', '反比例函数',       '初三·上学期', 1),
(11, 'math', '二次函数',         '初三·上学期', 2),
(12, 'math', '相似三角形',       '初三·下学期', 1),
(13, 'math', '锐角三角函数',     '初三·下学期', 2);

-- ============================================================
-- 4. 测试数据 — 题库（基于当前数据库实际 users/students 数据）
-- ============================================================



-- 4a. 老师上传的新题（全校共享，teacher_id 取最小 role_type=3 的用户）
INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `teacher_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `grade_level`)
SELECT 1, 'math', 'NEW', 'TEACHER', u.id, '计算：(-3)² + |-5| - 2×(-4)', '1', '20', 'EASY', '初一·上学期'
FROM `users` u WHERE u.role_type = 3 ORDER BY u.id LIMIT 1;

INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `teacher_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `grade_level`, `analysis`, `solution`)
SELECT 2, 'math', 'NEW', 'TEACHER', u.id, '解方程：2(x-3)+5=3x-4', '3', 'x=3', 'MEDIUM', '初一·上学期',
       '去括号时注意符号：2x-6+5=3x-4，移项后得 x=3',
       '1. 去括号: 2x-6+5=3x-4\n2. 合并同类项: 2x-1=3x-4\n3. 移项: -1+4=3x-2x\n4. 得 x=3'
FROM `users` u WHERE u.role_type = 3 ORDER BY u.id LIMIT 1;

INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `teacher_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `grade_level`)
SELECT 3, 'math', 'NEW', 'TEACHER', u.id, '已知抛物线过(1,0),(2,3),(0,-1)，求解析式', '11', 'y=2x²-3x-1', 'HARD', '初三·上学期'
FROM `users` u WHERE u.role_type = 3 ORDER BY u.id LIMIT 1;

INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `teacher_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `grade_level`)
SELECT 4, 'math', 'NEW', 'TEACHER', u.id, 'Rt△ABC中∠C=90°,AC=3,BC=4,求AB', '8', '5', 'EASY', '初二·下学期'
FROM `users` u WHERE u.role_type = 3 ORDER BY u.id LIMIT 1;

-- 4b. 学生错题（关联现有学生，按 id 取前2个）
INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `student_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `error_type`, `grade_level`)
SELECT 5, 'math', 'WRONG', 'STUDENT', s.id, '若|x-2|=3，则x=？', '1', 'x=5或x=-1', 'MEDIUM', '概念混淆', '初一·上学期'
FROM `students` s ORDER BY s.id LIMIT 1;

INSERT IGNORE INTO `question_bank` (`id`, `subject`, `type`, `source`, `student_id`, `title`, `knowledge_point_ids`, `answer`, `difficulty`, `error_type`, `grade_level`)
SELECT 6, 'math', 'WRONG', 'STUDENT', s.id, '解不等式：3x-7>2x+5', '5', 'x>12', 'EASY', '计算错误', '初一·下学期'
FROM `students` s ORDER BY s.id LIMIT 1 OFFSET 1;