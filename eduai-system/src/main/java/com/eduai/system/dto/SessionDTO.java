package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 上课时间 DTO
 */
@Data
public class SessionDTO {

    /** 上课日期 */
    @NotNull(message = "上课日期不能为空")
    private LocalDate classDate;

    /** 开始时间（小时） */
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    /** 结束时间（小时） */
    @NotBlank(message = "结束时间不能为空")
    private String endTime;
}