package com.eduai.security.enums;

import lombok.Getter;

/**
 * 认证模块业务错误码
 * <p>
 * 所有绑定冲突、验证码校验等场景统一使用此枚举，禁止硬编码错误码数字。
 */
@Getter
public enum AuthErrorCode {

    /** 该手机号已绑定其他微信账号 */
    PHONE_ALREADY_BOUND(40001, "该手机号已绑定其他微信账号"),

    /** 该微信已绑定其他手机号 */
    WECHAT_ALREADY_BOUND(40002, "该微信已绑定其他手机号"),

    /** 验证码错误或已过期 */
    SMS_CODE_INVALID(40003, "验证码错误或已过期"),

    /** 短信发送过于频繁 */
    SMS_RATE_LIMITED(40004, "发送过于频繁，请60秒后重试"),

    /** 短信发送失败 */
    SMS_SEND_FAILED(40005, "短信发送失败，请稍后重试"),

    /** 手机号格式不正确 */
    PHONE_FORMAT_INVALID(40006, "手机号格式不正确"),

    /** 用户不存在 */
    USER_NOT_FOUND(40007, "用户不存在"),

    /** 微信未绑定账号 */
    WECHAT_NOT_BOUND(40008, "微信未绑定账号，请先绑定手机号"),

    /** 新用户注册需选择角色 */
    ROLE_REQUIRED(40009, "新用户注册需选择角色");

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
