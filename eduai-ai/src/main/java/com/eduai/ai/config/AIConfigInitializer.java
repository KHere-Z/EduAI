package com.eduai.ai.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 配置自动初始化 — 首次启动时写入 ai_config 默认数据
 */
@Slf4j
@Component
public class AIConfigInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${eduai.ai.deepseek-api-key:}")
    private String deepseekApiKey;

    @Value("${eduai.ai.doubao-api-key:}")
    private String doubaoApiKey;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🔧 AIConfigInitializer 检查 ai_config...");

        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM ai_config").getSingleResult();
            log.info("ai_config 表存在，当前记录数: {}", count);
        } catch (Exception e) {
            log.info("ai_config 表尚未创建，跳过初始化。执行 docs/sql/migrate-ai-config.sql");
            return;
        }

        // 1. 初始化 ai_models 表（模型注册表）
        try {
            Number modelCount = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM ai_models").getSingleResult();
            if (modelCount.longValue() == 0) {
                String dsKey = deepseekApiKey != null ? deepseekApiKey : "";
                String dbKey = doubaoApiKey != null ? doubaoApiKey : "";
                entityManager.createNativeQuery(
                        "INSERT INTO ai_models (value, name, description, provider, api_url, api_key, tag) VALUES " +
                        "('deepseek-v4-pro','DeepSeek-V4-Pro','性价比最高','DeepSeek','https://api.deepseek.com/v1',?,'success')," +
                        "('doubao-seed-2-1-pro-260628','Doubao-Seed-2.1','支持识图','字节跳动','https://ark.cn-beijing.volces.com/api/v3/responses',?,'')"
                ).setParameter(1, dsKey)
                 .setParameter(2, dbKey)
                 .executeUpdate();
                log.info("✅ ai_models 默认数据已写入（DeepSeek + Doubao，api_key 来自环境变量 DEEPSEEK_API_KEY / DOUBAO_API_KEY）");
            } else {
                log.info("ai_models 已有 {} 条数据，跳过初始化", modelCount.longValue());
            }
        } catch (Exception e) {
            log.info("ai_models 表尚未创建，跳过初始化。请执行 docs/sql/migrate-ai-config.sql");
        }

        // 2. 初始化 ai_config 表（功能→模型映射）
        try {
            Number configCount = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM ai_config").getSingleResult();
            if (configCount.longValue() > 0) {
                log.info("ai_config 已有数据，跳过初始化");
                return;
            }

            entityManager.createNativeQuery(
                    "INSERT INTO ai_config (module, model) VALUES " +
                    "('wrong_analysis','deepseek-v4-pro')," +
                    "('exam_analysis','doubao-seed-2-1-pro-260628')"
            ).executeUpdate();
            log.info("✅ ai_config 默认配置已写入（三级模块→模型引用）");
        } catch (Exception e) {
            log.warn("ai_config 初始化失败: {}", e.getMessage());
        }
    }
}
