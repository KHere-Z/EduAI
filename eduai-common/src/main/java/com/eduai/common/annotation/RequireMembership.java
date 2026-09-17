package com.eduai.common.annotation;

import java.lang.annotation.*;

/**
 * 会员专享接口门控注解
 * <p>
 * 标注在 controller 方法上：切面会校验当前登录用户的会员状态，非会员直接拒绝，
 * 返回业务码 {@code 40011}。
 * <p>
 * 为什么需要它：前端的会员门控（弹「会员专享」确认框 + 跳充值页）是纯客户端 UI 拦截，
 * 不产生任何后端请求，直接调接口即可绕过。凡是收费功能，真正的门必须落在后端。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireMembership {
}
