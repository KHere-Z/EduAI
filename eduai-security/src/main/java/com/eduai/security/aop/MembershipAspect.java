package com.eduai.security.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.common.annotation.RequireMembership;
import com.eduai.security.enums.AuthErrorCode;
import com.eduai.security.service.PointService;
import com.eduai.security.vo.MembershipVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 会员门控切面 —— 处理 {@link RequireMembership} 注解。
 * <p>
 * 两种拒绝路径：
 * <ul>
 *   <li>未登录 → {@code StpUtil.getLoginIdAsLong()} 抛 NotLoginException → 全局异常处理器返回 401</li>
 *   <li>已登录但非会员 → 抛业务码 40011 → HTTP 400 + {@code {code:40011, message:"该功能为会员专享"}}</li>
 * </ul>
 * <p>
 * 会员判定复用 {@link PointService#getMembership}（status=active 且 expiresAt 未过期），
 * 不在此处重复实现口径，避免两处判定漂移。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MembershipAspect {

    private final PointService pointService;

    @Around("@annotation(requireMembership)")
    public Object around(ProceedingJoinPoint joinPoint, RequireMembership requireMembership) throws Throwable {
        Long userId = StpUtil.getLoginIdAsLong();

        MembershipVO membership = pointService.getMembership(userId);
        if (membership == null || !membership.isActive()) {
            log.warn("🚫 会员门控拦截: uid={}, method={}", userId,
                    joinPoint.getSignature().toShortString());
            throw new BusinessException(
                    AuthErrorCode.MEMBERSHIP_REQUIRED.getCode(),
                    AuthErrorCode.MEMBERSHIP_REQUIRED.getMessage());
        }

        return joinPoint.proceed();
    }
}
