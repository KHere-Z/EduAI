package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识点 创建/更新 DTO
 */
@Data
public class KnowledgePointDTO {

    /** 知识点名称 */
    @NotBlank(message = "知识点名称不能为空")
    private String name;

    /** 学科 */
    @NotBlank(message = "学科不能为空")
    private String subject;

    /** 年级·学期（如"初三·上学期"） */
    private String gradeLevel;

    /** 父知识点ID（树形结构） */
    private Long parentId;

    /** 排序 */
    private Integer sortOrder;
}