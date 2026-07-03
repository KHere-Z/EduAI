package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员视角 - 学生 VO（全局视图，含关联教师信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudentVO {

    private Long id;
    private String name;
    private String gender;
    private String contact;
    private String grade;
    private String school;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联的教师列表（通过 teacher_student 表） */
    private List<TeacherRelation> teacherRelations;

    /**
     * 教师关联信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherRelation {
        private Long teacherId;
        private String teacherName;
        private List<String> subjects;
        private Integer hoursLeft;
    }
}