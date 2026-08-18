package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 学习资源 · 教材 创建 DTO
 */
@Data
public class ResourceTextbookDTO {

    /** 学科 key，缺省 math */
    private String subject;

    /** 学段：primary/junior/senior（缺省 junior） */
    private String stage;

    /** 版本 key：suke/renjiao/bsd/zj/hk（缺省 suke） */
    private String version;

    /** 教材名 */
    @NotBlank(message = "教材名不能为空")
    private String name;
}
