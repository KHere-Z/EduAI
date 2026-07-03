package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 调课申请 DTO
 */
@Data
public class RescheduleDTO {

    /** 原排课 session ID */
    @NotNull(message = "原排课ID不能为空")
    private Long sessionId;

    /** 申请调至日期 */
    @NotNull(message = "新日期不能为空")
    private LocalDate requestedDate;

    /** 申请调至开始时间 */
    @NotBlank(message = "新开始时间不能为空")
    private String requestedStart;

    /** 申请调至结束时间 */
    @NotBlank(message = "新结束时间不能为空")
    private String requestedEnd;

    /** 调课原因 */
    private String reason;
}