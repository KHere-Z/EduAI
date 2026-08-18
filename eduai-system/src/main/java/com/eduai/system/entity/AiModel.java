package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 模型注册表 — 集中管理所有可用 AI 模型
 * <p>
 * ai_config 表通过 model_value 引用此表的 value 字段，
 * 实现两级查找：ai_config.module → ai_models.value → apiUrl + apiKey
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_models")
public class AiModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 模型标识（唯一，如 "deepseek-chat"、"doubao-seed-2-1-pro-260628"） */
    @Column(nullable = false, length = 50, unique = true)
    private String value;

    /** 显示名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 简短描述（如 "性价比最高"、"支持识图"） */
    @Column(length = 200)
    private String description;

    /** 供应商（如 "DeepSeek"、"字节跳动"、"OpenAI"） */
    @Column(length = 50)
    private String provider;

    /** API 地址 */
    @Column(name = "api_url", length = 500)
    private String apiUrl;

    /** API Key */
    @Column(name = "api_key", length = 500)
    private String apiKey;

    /** 前端 tag 样式（如 "success"、"danger"、""） */
    @Column(length = 20)
    private String tag;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
