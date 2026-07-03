package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员视角 - 教师详情 VO（含学生列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTeacherDetailVO {

    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private List<String> subjects;
    private String title;
    private String orgName;
    private String avatar;
    private String bio;
    private Integer status;

    /** 学生数量 */
    private Long studentCount;

    /** 总剩余课时 */
    private Long totalHours;

    /** 学生列表 */
    private List<TeacherStudentItem> students;

    /**
     * 该老师下的学生
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherStudentItem {
        private Long tsId;          // teacher_student.id
        private Long studentId;     // student.id
        private String studentName;
        private String gender;
        private String grade;
        private String school;
        private Integer hoursLeft;
        private List<String> subjects; // 该学生报的科目
    }
}