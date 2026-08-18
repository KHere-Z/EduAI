package com.eduai.ai.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 全局配置缓存 — 两级查找：ai_config.module → ai_models.value → apiUrl + apiKey
 * <p>
 * 启动时加载，管理员修改设置后通过 {@link #refresh()} 刷新缓存。
 */
@Slf4j
@Getter
@Component
public class DeepSeekConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /** API Key（默认缓存值，各功能模块通过 resolveModel 自行覆盖） */
    private volatile String apiKey;

    /** DeepSeek API 地址 */
    private volatile String apiUrl;

    /** 模型名称 */
    private volatile String model;

    /** 默认值（ai_models / ai_config 表均无数据时兜底） */
    private static final String DEFAULT_MODEL = "deepseek-v4-pro";
    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1";

    /** DeepSeek API Key 兜底（环境变量 DEEPSEEK_API_KEY 注入，勿硬编码） */
    @Value("${eduai.ai.deepseek-api-key:}")
    private String defaultApiKey;

    @PostConstruct
    public void init() {
        log.info("🔧 DeepSeekConfig 初始化开始...");
        refresh();
        if (isConfigured()) {
            log.info("✅ DeepSeek 配置加载成功: model={}, url={}, keyPrefix={}",
                    getEffectiveModel(), getEffectiveApiUrl(),
                    apiKey.substring(0, Math.min(8, apiKey.length())) + "***");
        } else {
            log.warn("⚠️ DeepSeek API Key 未配置！请前往 /admin/settings 配置 AI 服务");
        }
    }

    /**
     * 刷新配置（重置为默认值）
     * <p>
     * 各功能模块（wrong_analysis / exam_analysis）通过 resolveModel 自行查找，
     * 此处的缓存值仅作为兜底。
     */
    public void refresh() {
        this.model = DEFAULT_MODEL;
        this.apiUrl = DEFAULT_API_URL;
        this.apiKey = defaultApiKey;
        log.info("DeepSeekConfig: model={}, url={}, keyPrefix={}",
                model, apiUrl, apiKey.substring(0, Math.min(8, apiKey.length())) + "***");
    }

    /**
     * 通用模型解析 — 根据模块名两级查找模型凭证
     *
     * @param module 功能模块（wrong_analysis / exam_analysis）
     * @return Map 包含 model, apiUrl, apiKey（永不返回 null，失败时填充默认值）
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> resolveModel(String module) {
        Map<String, String> result = new LinkedHashMap<>();
        String resolvedModel = null;
        String resolvedUrl = null;
        String resolvedKey = null;

        try {
            // Step 1: 查 ai_config 获取模块对应的模型名
            List<?> configRows = entityManager.createNativeQuery(
                    "SELECT model FROM ai_config WHERE module = ? LIMIT 1")
                    .setParameter(1, module)
                    .getResultList();

            String modelName = null;
            if (!configRows.isEmpty()) {
                modelName = (String) configRows.get(0);
            }

            // Step 2: 查 ai_models 获取模型凭证
            if (modelName != null && !modelName.isBlank()) {
                List<?> modelRows = entityManager.createNativeQuery(
                        "SELECT api_url, api_key FROM ai_models WHERE value = ? LIMIT 1")
                        .setParameter(1, modelName)
                        .getResultList();
                if (!modelRows.isEmpty()) {
                    Object[] mRow = (Object[]) modelRows.get(0);
                    if (mRow[0] != null && !((String) mRow[0]).isBlank()) {
                        resolvedUrl = (String) mRow[0];
                    }
                    if (mRow[1] != null && !((String) mRow[1]).isBlank()) {
                        resolvedKey = (String) mRow[1];
                    }
                }
            }

            resolvedModel = modelName;

        } catch (Exception e) {
            log.warn("解析模块 {} 模型配置失败: {}", module, e.getMessage());
        }

        // 兜底默认值
        boolean fallbackModel = (resolvedModel == null || resolvedModel.isBlank());
        boolean fallbackUrl = (resolvedUrl == null || resolvedUrl.isBlank());
        boolean fallbackKey = (resolvedKey == null || resolvedKey.isBlank());
        if (fallbackModel) resolvedModel = DEFAULT_MODEL;
        if (fallbackUrl) resolvedUrl = DEFAULT_API_URL;
        if (fallbackKey) resolvedKey = defaultApiKey;

        log.info("🔎 resolveModel({}): model={} (dbFound={}), url={} (dbFound={}), keyPrefix={} (dbFound={})",
                module, resolvedModel, !fallbackModel,
                resolvedUrl, !fallbackUrl,
                resolvedKey != null ? resolvedKey.substring(0, Math.min(8, resolvedKey.length())) + "***" : "NULL",
                !fallbackKey);

        result.put("model", resolvedModel);
        result.put("apiUrl", resolvedUrl);
        result.put("apiKey", resolvedKey);
        return result;
    }

    /**
     * 是否已配置 API Key
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 获取默认模型（未配置时兜底）
     */
    public String getEffectiveModel() {
        return (model != null && !model.isBlank()) ? model : "deepseek-chat";
    }

    /**
     * 获取默认 API 地址（未配置时兜底）
     */
    public String getEffectiveApiUrl() {
        return (apiUrl != null && !apiUrl.isBlank()) ? apiUrl : "https://api.deepseek.com/v1";
    }

    /**
     * 根据模型名从 ai_models 表解析 API URL
     */
    public String resolveApiUrl(String modelName) {
        if (modelName == null || modelName.isBlank()) return getEffectiveApiUrl();
        try {
            List<?> rows = entityManager.createNativeQuery(
                    "SELECT api_url FROM ai_models WHERE value = ? LIMIT 1")
                    .setParameter(1, modelName)
                    .getResultList();
            if (!rows.isEmpty() && rows.get(0) != null) {
                String url = (String) rows.get(0);
                if (!url.isBlank()) return url;
            }
        } catch (Exception e) {
            log.debug("resolveApiUrl({}) 失败: {}", modelName, e.getMessage());
        }
        return getEffectiveApiUrl();
    }

    /**
     * 根据模型名从 ai_models 表解析 API Key
     */
    public String resolveApiKey(String modelName) {
        if (modelName == null || modelName.isBlank()) return apiKey;
        try {
            List<?> rows = entityManager.createNativeQuery(
                    "SELECT api_key FROM ai_models WHERE value = ? LIMIT 1")
                    .setParameter(1, modelName)
                    .getResultList();
            if (!rows.isEmpty() && rows.get(0) != null) {
                String key = (String) rows.get(0);
                if (!key.isBlank()) return key;
            }
        } catch (Exception e) {
            log.debug("resolveApiKey({}) 失败: {}", modelName, e.getMessage());
        }
        return apiKey;
    }
}
