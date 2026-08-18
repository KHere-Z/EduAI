package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习反馈 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackVO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private Long studentId;
    private String studentName;
    private String subject;
    private String period;
    private String content;
    private LocalDateTime createdAt;
}
