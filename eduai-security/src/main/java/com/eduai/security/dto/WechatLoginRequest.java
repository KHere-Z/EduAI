package com.eduai.security.dto;

import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WechatLoginRequest {

    /** 微信 openid（必填） */
    private String openid;

    /** 微信 unionid（必填，用于唯一绑定校验） */
    private String unionid;

    /** 微信昵称（可选，用于首次绑定时记录） */
    private String nickname;

    /** 微信头像URL（可选） */
    private String avatar;
}
