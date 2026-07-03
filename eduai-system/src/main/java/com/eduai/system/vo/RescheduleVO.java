package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课申请 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleVO {

    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentName;
    private Long teacherId;
    private String teacherName;
    private String subject;
    private LocalDate originalDate;
    private String originalStart;
    private String originalEnd;
    private LocalDate requestedDate;
    private String requestedStart;
    private String requestedEnd;
    private String reason;
    private String status; // pending/approved/deferred/closed
    private LocalDateTime createdAt;
}