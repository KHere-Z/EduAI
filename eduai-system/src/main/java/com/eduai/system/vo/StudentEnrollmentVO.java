package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生端 - 报名科目列表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentVO {

    private List<EnrolledCourse> courses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrolledCourse {
        private Long tsId;          // teacher_student.id
        private String subject;
        private String teacherName;
        private Long teacherId;
        private Integer hoursLeft;
        private List<SessionVO> upcomingSessions; // 近期排课
    }
}