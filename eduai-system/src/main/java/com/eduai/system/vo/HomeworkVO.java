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
public class HomeworkVO {
    private Long id;
    private Long teacherId;

    /** 前端兼容 homeworkId 字段名 */
    public Long getHomeworkId() { return id; }
    private String teacherName;
    private String subject;
    private String title;
    private String description;
    private String answerFileUrl;
    private String answerFileName;

    /** 学生提交状态（学生端用） */
    private String submitStatus;  // pending / submitted / corrected
    private String submittedImageUrl;
    private String correctedImageUrl;

    private LocalDateTime createdAt;
}
