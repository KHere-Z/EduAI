package com.eduai.common.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 限流 AOP 切面 — Redis 滑动窗口实现
 * <p>
 * Key 格式: rate_limit:{userId}:{methodName}:{windowSec}s
 * 首次访问 SET + EXPIRE，后续 INCR，超限抛 BusinessException(429)。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String userId;
        try {
            userId = "u:" + StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            // 未登录请求按客户端 IP 限流（Nginx 反代取 X-Forwarded-For 首段），避免全站共享一个桶
            userId = "ip:" + clientIp();
        }

        String key = "rate_limit:" + userId + ":" + methodName + ":" + rateLimit.windowSec() + "s";
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, rateLimit.windowSec(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.limit()) {
            log.warn("🚫 限流触发: key={}, count={}, limit={}", key, count, rateLimit.limit());
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /** 获取客户端真实 IP（Nginx 反代优先取 X-Forwarded-For 首段） */
    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
