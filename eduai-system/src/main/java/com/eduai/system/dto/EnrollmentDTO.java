package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 报名科目 DTO
 */
@Data
public class EnrollmentDTO {

    /** 科目 */
    @NotBlank(message = "科目不能为空")
    private String subject;

    /** 上课时间列表 */
    private List<SessionDTO> sessions;
}