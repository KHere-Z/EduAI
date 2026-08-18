package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学习资源 · 小节 创建 DTO
 */
@Data
public class ResourceSectionDTO {

    /** 小节名 */
    @NotBlank(message = "小节名不能为空")
    private String name;
}
