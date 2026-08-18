package com.eduai.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解（Redis 滑动窗口）
 * <p>
 * 基于当前用户 ID + 方法名作为 key，在指定时间窗口内限制调用次数。
 * 超限返回 HTTP 429。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口（秒），默认 60 秒 */
    int windowSec() default 60;

    /** 窗口内允许的最大调用次数，默认 10 次 */
    int limit() default 10;

    /** 限流提示，默认 "请求过于频繁，请稍后再试" */
    String message() default "请求过于频繁，请稍后再试";
}
