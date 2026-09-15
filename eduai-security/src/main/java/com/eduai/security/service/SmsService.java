package com.eduai.security.service;

/**
 * 短信服务接口
 */
public interface SmsService {

    /**
     * 发送短信验证码
     *
     * @param phone        手机号（已校验格式）
     * @param code         6位验证码
     * @param validMinutes 有效期（分钟），用于填充模板文案中的"请于{2}分钟内填写"
     * @throws com.eduai.common.BusinessException 发送失败时抛出
     */
    void sendVerifyCode(String phone, String code, int validMinutes);
}
