package com.eduai.security.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 短信验证码登录/注册请求
 * <p>
 * 新用户（手机号不存在）必须携带 role + 对应角色资料；
 * 老用户仅需 phone + code。
 */
@Data
public class LoginBySmsRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 角色类型：teacher / student（新用户必填，老用户忽略） */
    private String role;

    /** 教师注册信息（role=teacher 时使用） */
    @Valid
    private TeacherRegisterInfo teacherInfo;

    /** 学生注册信息（role=student 时使用） */
    @Valid
    private StudentRegisterInfo studentInfo;
}
