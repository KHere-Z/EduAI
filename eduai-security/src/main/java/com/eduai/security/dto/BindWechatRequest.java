package com.eduai.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 已登录用户绑定微信请求
 */
@Data
public class BindWechatRequest {

    @NotBlank(message = "微信 openid 不能为空")
    private String openid;

    @NotBlank(message = "微信 unionid 不能为空")
    private String unionid;

    /** 微信昵称（可选） */
    private String nickname;

    /** 微信头像（可选） */
    private String avatar;
}
