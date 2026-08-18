package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionVO {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String studentName;
    private String submittedImageUrl;
    private String correctedImageUrl;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime correctedAt;
}
