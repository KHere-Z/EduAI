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
            // DAU/MAU 埋点：与在线心跳同源（每个已登录请求），但按自然日分桶、长期保留。
            // 刻意不塞进 heartbeat 内部：两者保留策略完全不同（5 分钟 vs 31 天），
            // 合并会让「当前在线」这个只读短期窗口的指标也被写进日桶。
            metricService.markDailyActive(uid);
        } catch (Exception ignored) {
            // 未登录 / token 无效：不打点
        }
        return true;
    }
}
