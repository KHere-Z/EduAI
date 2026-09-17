package com.eduai.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginDTO {

    /** 登录标识：用户名或手机号（后端两者都匹配，字段名保持不变） */
    @NotBlank(message = "用户名或手机号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}