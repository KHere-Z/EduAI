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
    /** 所属老师 UID（8位字符串，与 auth.user.uid 同源，前端 isOwnKp 用） */
    private String teacherId;
    private String gradeLevel;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}