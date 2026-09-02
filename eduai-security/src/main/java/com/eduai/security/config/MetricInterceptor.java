package com.eduai.security.config;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.service.MetricService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在线心跳拦截器 — 为「当前在线人数」指标打点。
 * <p>
 * 每个已登录请求进来时刷新该用户的在线时间戳（metric:online ZSET）。
 * 未登录请求 {@code StpUtil.getLoginIdAsLong()} 抛异常，天然跳过不打点。
 */
@Component
@RequiredArgsConstructor
public class MetricInterceptor implements HandlerInterceptor {

    private final MetricService metricService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Long uid = StpUtil.getLoginIdAsLong();
            metricService.heartbeat(uid);
        } catch (Exception ignored) {
            // 未登录 / token 无效：不打点
        }
        return true;
    }
}
