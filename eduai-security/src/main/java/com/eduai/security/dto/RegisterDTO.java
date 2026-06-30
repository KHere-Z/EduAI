package com.eduai.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 真实姓名 */
    private String realName;

    @NotNull(message = "角色类型不能为空")
    private Integer roleType;

    // ===== 教师注册专属字段 =====
    /** 任教学科，逗号分隔，如 "math,physics" */
    private String subjectIds;
    /** 所属机构ID */
    private Long orgId;
    /** 职称 */
    private String title;
}