package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@Builder
public class LoginVO {

    /** 用户信息（含教师专属字段） */
    private UserVO user;

    /** Sa-Token token 字符串 */
    private String token;
}