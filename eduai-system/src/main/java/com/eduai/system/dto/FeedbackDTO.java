package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学习反馈 DTO
 */
@Data
public class FeedbackDTO {

    @NotNull(message = "学生不能为空")
    private Long studentId;

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private String subject;
    private String period;
}
