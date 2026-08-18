package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeworkDTO {

    @NotBlank(message = "学科不能为空")
    private String subject;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String description;
}
