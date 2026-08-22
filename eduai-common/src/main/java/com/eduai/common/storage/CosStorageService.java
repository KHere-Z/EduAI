package com.eduai.common.storage;

import com.eduai.common.config.CosConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * 腾讯云 COS 对象存储客户端封装。
 * <p>
 * 设计原则：<b>上传失败返回 {@code null}，由调用方回退本地磁盘</b>。COS 未配置 / 未初始化 /
 * 网络异常时均不抛异常、不中断业务，保证生产在 COS 尚未开通或临时故障时依然可用。
 * <p>
 * key 沿用原本地目录结构（如 {@code question-images/original/2026-08-21/uuid.png}），
 * 便于存量数据迁移时一一对应。
 */
@Slf4j
@Component
public class CosStorageService {

    private final CosConfig config;
    private volatile COSClient cosClient;

    public CosStorageService(CosConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            log.warn("⚠️ COS 未配置（cos.secret-id / cos.bucket 缺失），图片将回退本地磁盘存储");
            return;
        }
        try {
            COSCredentials cred = new BasicCOSCredentials(config.getSecretId(), config.getSecretKey());
            ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
            this.cosClient = new COSClient(cred, clientConfig);
            log.info("✅ COS 已初始化: bucket={} region={}", config.getBucket(), config.getRegion());
        } catch (Exception e) {
            log.error("COS 初始化失败，图片将回退本地磁盘: {}", e.getMessage());
            this.cosClient = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

    /** COS 是否已配置可用 */
    public boolean isConfigured() {
        return config != null
                && config.getSecretId() != null && !config.getSecretId().isBlank()
                && config.getSecretKey() != null && !config.getSecretKey().isBlank()
                && config.getBucket() != null && !config.getBucket().isBlank()
                && config.getRegion() != null && !config.getRegion().isBlank();
    }

    /**
     * 上传字节流到 COS，返回公共访问 URL；失败或未配置时返回 {@code null}。
     *
     * @param bytes 文件字节
     * @param key   对象 key（如 question-images/original/2026-08-21/uuid.png）
     */
    public String upload(byte[] bytes, String key) {
        if (!isConfigured() || cosClient == null || bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentTypeFor(key));
            metadata.setContentLength(bytes.length);

            PutObjectRequest request = new PutObjectRequest(
                    config.getBucket(), key, new ByteArrayInputStream(bytes), metadata);
            cosClient.putObject(request);

            return buildUrl(key);
        } catch (Exception e) {
            log.error("COS 上传失败: key={}, msg={}", key, e.getMessage());
            return null;
        }
    }

    /** 拼接公共访问 URL：优先 publicBaseUrl，其次 COS 默认域名 */
    private String buildUrl(String key) {
        String base = config.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://" + config.getBucket() + ".cos." + config.getRegion() + ".myqcloud.com";
        }
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return base + "/" + key;
    }

    /** 根据 key 后缀推断 Content-Type（影响浏览器/IMG 直接访问渲染） */
    private String contentTypeFor(String key) {
        String lower = key == null ? "" : key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }
}
