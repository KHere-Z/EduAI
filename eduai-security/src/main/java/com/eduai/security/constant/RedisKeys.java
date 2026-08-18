package com.eduai.security.constant;

/**
 * Redis Key 常量
 */
public final class RedisKeys {

    private RedisKeys() {}

    /** 短信验证码 key: sms:code:{phone}，TTL=300s */
    public static final String SMS_CODE = "sms:code:%s";

    /** 短信发送限流 key: sms:limit:{phone}，TTL=60s */
    public static final String SMS_LIMIT = "sms:limit:%s";

    /** 短信验证码有效期（秒） */
    public static final long SMS_CODE_TTL = 300;

    /** 短信发送间隔（秒） */
    public static final long SMS_LIMIT_TTL = 60;

    /** 手机号正则 */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    public static String smsCodeKey(String phone) {
        return String.format(SMS_CODE, phone);
    }

    public static String smsLimitKey(String phone) {
        return String.format(SMS_LIMIT, phone);
    }
}
