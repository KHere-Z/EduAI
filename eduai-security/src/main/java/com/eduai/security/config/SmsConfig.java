package com.eduai.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云短信配置
 * <p>
 * 密钥通过环境变量注入，禁止硬编码。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms.tencent")
public class SmsConfig {

    /** 腾讯云 SecretId */
    private String secretId;

    /** 腾讯云 SecretKey */
    private String secretKey;

    /** 短信应用ID */
    private String sdkAppId;

    /** 短信签名 */
    private String signName = "EduAI";

    /** 短信模板ID */
    private String templateId;
}
