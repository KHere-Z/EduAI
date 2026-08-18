-- ============================================================
-- EduAI AI 配置 — 模型集中管理（32.4）
-- ai_models : 模型注册表（url/key 的真相源）
-- ai_config : 功能→模型映射（只存 module→model_value 引用）
-- 替代旧 system_config 表
-- ============================================================

USE `eduai`;

-- 1. 模型注册表（先建，ai_config 依赖它）
CREATE TABLE IF NOT EXISTS `ai_models` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `value`       VARCHAR(50)  NOT NULL COMMENT '模型标识（唯一，如 deepseek-v4-pro）',
    `name`        VARCHAR(100) NOT NULL COMMENT '显示名称',
    `description` VARCHAR(200) COMMENT '简短描述',
    `provider`    VARCHAR(50)  COMMENT '供应商',
    `api_url`     VARCHAR(500) COMMENT 'API 地址',
    `api_key`     VARCHAR(500) COMMENT 'API Key',
    `tag`         VARCHAR(20)  COMMENT '前端 tag 样式（success/danger/空）',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_value` (`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型注册表';

-- 初始模型数据
-- api_key 留空：运行时由环境变量 DEEPSEEK_API_KEY / DOUBAO_API_KEY（经 AIConfigInitializer 种子）
-- 或管理后台 /admin/settings 注入，勿在 SQL 中提交真实密钥。
INSERT INTO `ai_models` (`value`, `name`, `description`, `provider`, `api_url`, `api_key`, `tag`) VALUES
('deepseek-v4-pro',                'DeepSeek-V4-Pro', '性价比最高', 'DeepSeek', 'https://api.deepseek.com/v1',                                     '', 'success'),
('doubao-seed-2-1-pro-260628',     'Doubao-Seed-2.1', '支持识图',   '字节跳动', 'https://ark.cn-beijing.volces.com/api/v3/responses',           '', '')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 2. 功能配置表（只存 module → model 引用）

CREATE TABLE IF NOT EXISTS `ai_config` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `module`     VARCHAR(30)  NOT NULL COMMENT '功能模块 wrong_analysis/exam_analysis',
    `model`      VARCHAR(100) NOT NULL COMMENT '模型标识（引用 ai_models.value）',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI功能配置（功能→模型映射）';

-- 初始功能配置
INSERT INTO `ai_config` (`module`, `model`) VALUES
('wrong_analysis', 'deepseek-v4-pro'),
('exam_analysis',  'doubao-seed-2-1-pro-260628')
ON DUPLICATE KEY UPDATE `model` = VALUES(`model`);

SELECT * FROM ai_config WHERE module = 'exam_analysis';
SELECT * FROM ai_models WHERE value = 'doubao-seed-2-1-pro-260628';
