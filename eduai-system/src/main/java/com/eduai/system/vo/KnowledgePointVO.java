package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识点 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVO {

    private Long id;
    private String subject;
    private String name;
    private String gradeLevel;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}