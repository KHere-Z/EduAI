package com.eduai.security.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * <p>
 * 每个响应自动添加基础安全头，防御常见 Web 攻击。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 禁止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止被 iframe 嵌入（防点击劫持）
        httpResponse.setHeader("X-Frame-Options", "DENY");
        // 启用浏览器 XSS 过滤器
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        // HSTS（仅 HTTPS 环境生效）
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(request, response);
    }
}
