package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@Builder
public class LoginVO {

    /** 用户信息 */
    private UserVO user;

    /** Sa-Token token 字符串 */
    private String token;

    /** 微信登录特有：是否需要绑定手机号 */
    private Boolean needBindPhone;

    /** 微信登录特有：unionid（绑定手机时回传） */
    private String unionid;
}
