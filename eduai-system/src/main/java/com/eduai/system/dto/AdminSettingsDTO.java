package com.eduai.system.dto;

import lombok.Data;

/**
 * 管理员 - 系统设置更新 DTO
 */
@Data
public class AdminSettingsDTO {

    /** AI 模型名称 */
    private String aiModel;

    /** AI API Key */
    private String aiApiKey;

    /** AI API 地址 */
    private String aiApiUrl;

    /** 系统名称 */
    private String systemName;

    /** 最大并发数 */
    private String maxConcurrency;
}