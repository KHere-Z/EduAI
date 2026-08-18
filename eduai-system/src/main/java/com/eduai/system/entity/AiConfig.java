package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 功能配置实体 — 每项功能选择使用哪个模型
 * <p>
 * 32.4 改造后：只存 module → model_value 引用，
 * apiUrl/apiKey 由 {@link AiModel} 表集中管理。
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_config")
public class AiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 功能模块：wrong_analysis / exam_analysis */
    @Column(nullable = false, length = 30, unique = true)
    private String module;

    /** 模型标识（引用 ai_models.value，如 "deepseek-v4-pro"） */
    @Column(nullable = false, length = 100)
    private String model;

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
