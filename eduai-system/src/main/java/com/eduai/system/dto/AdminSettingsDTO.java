package com.eduai.system.dto;

import lombok.Data;

/**
 * 管理员 - 系统设置更新 DTO
 * <p>
 * 32.4 改造后：每项 AI 功能只存模型引用，apiUrl/apiKey 由 ai_models 表集中管理。
 */
@Data
public class AdminSettingsDTO {

    /** AI 模型名称（全局默认） */
    private String aiModel;

    /** AI API Key（全局默认，兼容旧版） */
    private String aiApiKey;

    /** AI API 地址（全局默认，兼容旧版） */
    private String aiApiUrl;

    /** 系统名称 */
    private String systemName;

    /** 最大并发数 */
    private String maxConcurrency;

    // ==================== 按场景选择模型（只存模型标识） ====================

    /** 错题分析模型（如 "deepseek-v4-pro"） */
    private String wrongAnalysisModel;

    /** 试卷分析模型（如 "doubao-seed-2-1-pro-260628"） */
    private String examAnalysisModel;
}
