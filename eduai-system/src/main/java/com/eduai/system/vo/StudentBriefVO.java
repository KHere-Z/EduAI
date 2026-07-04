package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生简要信息 VO（用于下拉选择器等场景）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentBriefVO {

    private Long studentId;
    private String studentName;
    private String grade;
}