-- =====================================================
-- AI 服务初始化配置（DeepSeek）
-- 在 MySQL 中执行此 SQL 即可完成 AI 接入
-- =====================================================

-- 插入/更新 AI API Key
-- 密钥勿硬编码：运行时经环境变量 DEEPSEEK_API_KEY 或管理后台 /admin/settings 注入。
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES ('ai_api_key', '', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 插入/更新 AI API 地址
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES ('ai_api_url', 'https://api.deepseek.com/v1', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 插入/更新 AI 模型
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES ('ai_model', 'deepseek-chat', NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 验证
SELECT config_key, config_value FROM system_config WHERE config_key LIKE 'ai_%';


ALTER TABLE system_config ADD COLUMN wrong_analysis_api_url VARCHAR(500);
ALTER TABLE system_config ADD COLUMN wrong_analysis_api_key VARCHAR(500);
ALTER TABLE system_config ADD COLUMN exam_analysis_api_url VARCHAR(500);
ALTER TABLE system_config ADD COLUMN exam_analysis_api_key VARCHAR(500);