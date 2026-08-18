package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 模型 VO（返回前端）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelVO {
    private Long id;
    private String value;
    private String name;
    private String description;
    private String provider;
    private String apiUrl;
    private String apiKey;
    private String tag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
