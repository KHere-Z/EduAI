package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 报名科目 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentVO {

    private Long id;
    private String subject;
    private List<SessionVO> sessions;
}