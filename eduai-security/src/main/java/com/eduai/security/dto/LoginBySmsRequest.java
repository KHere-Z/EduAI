package com.eduai.security.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 短信验证码登录请求
 * <p>
 * 后端只读 {@code phone} + {@code code}：手机号未注册时返回 40012，不再自动建号。
 * <p>
 * 下面 role / teacherInfo / studentInfo 三个字段**当前已不被后端使用**，
 * 保留仅为兼容仍在发送它们的旧前端（Jackson 会忽略未知字段，删掉也不会报错）。
 * 真正需要移除它们的前提是：微信绑定重建时（见 AuthServiceImpl#bindPhone 注释）
 * 不再复用本 DTO。在那之前先留着，避免同时改两处契约。
 */
@Data
public class LoginBySmsRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 【已不使用】原：新用户自动建号时的角色 */
    @Deprecated
    private String role;

    /** 【已不使用】原：新用户自动建号时的教师资料 */
    @Deprecated
    @Valid
    private TeacherRegisterInfo teacherInfo;

    /** 【已不使用】原：新用户自动建号时的学生资料 */
    @Deprecated
    @Valid
    private StudentRegisterInfo studentInfo;
}
