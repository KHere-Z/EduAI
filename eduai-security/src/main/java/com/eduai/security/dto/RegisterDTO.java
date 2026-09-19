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

    // ===== 短信验证码（**必填**） =====
    // 2026-09-19 起由选填改为必填：注册即送 49 智学点 + 7 天体验会员，
    // 不绑手机号就能反复匿名注册薅礼包。绑手机号后一个号码只能注册一次。
    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;
    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;

    // ===== 教师注册专属字段 =====
    /** 任教学科，逗号分隔，如 "math,physics" */
    private String subjectIds;
    /** 所属机构ID */
    private Long orgId;
    /** 职称 */
    private String title;
}