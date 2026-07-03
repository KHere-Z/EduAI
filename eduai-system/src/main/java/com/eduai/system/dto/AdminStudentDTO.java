package com.eduai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 管理员 - 学生创建/更新 DTO
 * <p>
 * POST/PUT 时遍历 teacherIds，每个对应一条 teacher_student 记录
 */
@Data
public class AdminStudentDTO {

    /** 姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 性别：男/女 */
    private String gender;

    /** 联系方式 */
    private String contact;

    /** 年级 */
    private String grade;

    /** 所在学校 */
    private String school;

    /** 任课老师ID列表（多对多，每个ID创建一条 teacher_student 记录） */
    private List<Long> teacherIds;

    /** 登录用户名（选填，填了会同时创建 roleType=4 的学生用户） */
    private String username;

    /** 登录密码（选填，与 username 配合） */
    private String password;
}