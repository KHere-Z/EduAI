package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVO {

    private Long id;
    private String name;
    private String gender;
    private String contact;
    private Integer hoursLeft;
    private String grade;
    private String school;
    private LocalDate regDate;
    private List<EnrollmentVO> enrollments;
}