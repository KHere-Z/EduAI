package com.eduai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 对象存储配置
 * <p>
 * 密钥/桶名/地域/访问域名通过环境变量注入，禁止硬编码。
 * <p>
 * 用途：图片上传从「本地磁盘落盘」改为「上传 COS」，前端直接访问 COS 公共 URL，
 * 把图片流量卸载到 COS/CDN，减轻 ECS 公网带宽压力（见 ImageStorageService / AIController）。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cos")
public class CosConfig {

    /** 腾讯云 SecretId */
    private String secretId;

    /** 腾讯云 SecretKey */
    private String secretKey;

    /** 桶名（含 APPID，如 eduai-images-1250000000） */
    private String bucket;

    /** 地域（如 ap-guangzhou，与 ECS 同地域） */
    private String region;

    /** 公共读访问 URL 前缀（如 https://img.example.com 或 COS 默认域名），末尾不带 / */
    private String publicBaseUrl;
}
