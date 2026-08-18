package com.eduai.security.service.impl;

import com.eduai.security.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 开发环境 Mock 短信服务（控制台打印验证码）
 * <p>
 * 当未配置腾讯云短信密钥时自动启用。
 */
@Slf4j
@Service
@ConditionalOnMissingBean(SmsService.class)
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void sendVerifyCode(String phone, String code) {
        log.info("============================================");
        log.info("  [Mock短信] 手机号: {}", phone);
        log.info("  [Mock短信] 验证码: {}", code);
        log.info("  [Mock短信] 请使用上方验证码完成登录");
        log.info("============================================");
    }
}
