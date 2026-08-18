-- ============================================================
-- EduAI 题目-知识点关联表（多对多中间表）
-- 版本: v1
-- 适用: MySQL 8.0+
-- 背景: 替代 question_bank.knowledge_point_ids CSV 列做「按知识点筛题」，
--       使 4 个 LIKE 全表扫描改为索引精确匹配，消除前缀歧义（5 vs 15/25）。
--       CSV 列暂保留作展示缓存，后续可删。
-- ============================================================

USE `eduai`;

CREATE TABLE IF NOT EXISTS `question_knowledge_point` (
    `id`                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `question_id`        BIGINT NOT NULL COMMENT '题目ID → question_bank.id',
    `knowledge_point_id` BIGINT NOT NULL COMMENT '知识点ID → knowledge_points.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_kp` (`question_id`, `knowledge_point_id`),
    INDEX `idx_qkp_kp` (`knowledge_point_id`),
    CONSTRAINT `fk_qkp_question` FOREIGN KEY (`question_id`)        REFERENCES `question_bank`(`id`)   ON DELETE CASCADE,
    CONSTRAINT `fk_qkp_kp`       FOREIGN KEY (`knowledge_point_id`) REFERENCES `knowledge_points`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目-知识点关联表';

-- 回填：从 question_bank.knowledge_point_ids (CSV) 迁移到关联表。
-- FIND_IN_SET 按整项匹配（'5' 不会命中 '15'），与筛题语义一致。
INSERT IGNORE INTO `question_knowledge_point` (`question_id`, `knowledge_point_id`)
SELECT q.id, kp.id
FROM `question_bank` q
JOIN `knowledge_points` kp ON FIND_IN_SET(kp.id, q.knowledge_point_ids) > 0
WHERE q.knowledge_point_ids IS NOT NULL AND TRIM(q.knowledge_point_ids) != '';
