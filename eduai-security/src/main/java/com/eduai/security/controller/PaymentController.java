package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付接口（开发阶段为 Mock 实现）
 * <p>
 * 生产环境接入真实支付宝/微信 SDK 时，实现 {@link PaymentService} 并新增
 * {@code @Profile("prod")} 的 Controller（本 Mock Controller 不注册）。
 */
@Slf4j
@RestController
@Profile("!prod")   // Mock 支付仅在非生产环境注册，生产环境不暴露 mock 接口
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建充值订单，返回（mock）二维码
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        return paymentService.createOrder(userId, body);
    }

    /**
     * 查询支付状态（前端轮询）
     */
    @GetMapping("/status/{orderId}")
    public Result<Map<String, Object>> queryStatus(@PathVariable String orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return paymentService.queryStatus(userId, orderId);
    }

    /**
     * 模拟支付成功（开发用，生产环境删除）
     */
    @PostMapping("/mock-pay/{orderId}")
    public Result<Void> mockPay(@PathVariable String orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return paymentService.mockPay(userId, orderId);
    }

    /** 支付宝异步回调（生产环境实现） */
    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestBody String body) {
        return paymentService.handleNotify("alipay", body);
    }

    /** 微信支付异步回调（生产环境实现） */
    @PostMapping("/notify/wechat")
    public String wechatNotify(@RequestBody String body) {
        return paymentService.handleNotify("wechat", body);
    }
}
