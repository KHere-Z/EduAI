package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 模型 DTO（新增/编辑共用）
 */
@Data
public class AiModelDTO {

    @NotBlank(message = "模型标识不能为空")
    private String value;

    @NotBlank(message = "模型名称不能为空")
    private String name;

    private String description;
    private String provider;
    private String apiUrl;
    private String apiKey;
    private String tag;
}
