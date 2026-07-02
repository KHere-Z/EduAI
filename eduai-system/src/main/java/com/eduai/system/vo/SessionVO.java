package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 上课时间 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    private Long id;
    private LocalDate classDate;
    private String startTime;
    private String endTime;
}