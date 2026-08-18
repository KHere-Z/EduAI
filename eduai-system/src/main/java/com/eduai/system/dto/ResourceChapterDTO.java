package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学习资源 · 章节 创建 DTO
 */
@Data
public class ResourceChapterDTO {

    /** 章节名 */
    @NotBlank(message = "章节名不能为空")
    private String name;
}
