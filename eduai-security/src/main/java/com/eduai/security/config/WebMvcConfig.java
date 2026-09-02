package com.eduai.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置 — 上传文件静态资源映射
 * <p>
 * 前端上传的图片/PDF 经相对路径 {@code /uploads/**} 访问（见 AGENTS.md §五），
 * 此处映射到本地 uploads 目录，与 Nginx 反代 {@code /uploads/} → 后端 保持一致。
 * <p>
 * 注意：仅靠 application.yml 的 {@code spring.web.resources.static-locations=file:./uploads/}
 * 会把 uploads 目录内容暴露在 URL 根路径（即 {@code /ai/xx.png}），而非 {@code /uploads/ai/xx.png}，
 * 导致 {@code /uploads/**} 404。因此必须显式注册 {@code /uploads/**} → uploads 目录。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    private final MetricInterceptor metricInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 在线心跳：所有请求进来刷新在线时间戳；未登录请求在拦截器内自然跳过
        registry.addInterceptor(metricInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location + "/");
        log.info("✅ 静态资源映射已注册: /uploads/** → {}", location);
    }
}
