package com.eduai.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 学生创建/更新 DTO
 */
@Data
public class StudentDTO {

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：男/女 */
    private String gender;

    /** 联系方式 */
    private String contact;

    /** 剩余课时 */
    private Integer hoursLeft;

    /** 年级 */
    private String grade;

    /** 所在学校 */
    private String school;

    /** 报名时间 */
    private LocalDate regDate;

    /** 报名科目列表 */
    @Valid
    private List<EnrollmentDTO> enrollments;
}