package com.eduai.security.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 微信绑定手机号请求
 * <p>
 * 微信登录后未绑定账号 → 弹窗输入手机号+验证码 → 此接口完成绑定（含注册）。
 */
@Data
public class BindPhoneRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 微信 openid */
    @NotBlank(message = "微信 openid 不能为空")
    private String openid;

    /** 微信 unionid（从微信登录步骤传入） */
    @NotBlank(message = "微信 unionid 不能为空")
    private String unionid;

    /** 角色类型（手机号不存在时注册用） */
    private String role;

    /** 教师注册信息 */
    @Valid
    private TeacherRegisterInfo teacherInfo;

    /** 学生注册信息 */
    @Valid
    private StudentRegisterInfo studentInfo;
}
