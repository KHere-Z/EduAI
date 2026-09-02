package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付接口
 * <p>
 * 开发环境 {@code channel=mock}（{@link com.eduai.security.service.impl.MockPaymentServiceImpl}），
 * 生产环境 {@code channel=wechat}（{@link com.eduai.security.service.impl.WechatPaymentServiceImpl}），
 * 由 Spring 按 Profile 自动装配，前端无需改动。
 */
@Slf4j
@RestController
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
    public String alipayNotify(@RequestBody String body, HttpServletRequest request) {
        return paymentService.handleNotify("alipay", body, collectNotifyHeaders(request));
    }

    /** 微信支付异步回调（生产环境实现） */
    @PostMapping("/notify/wechat")
    public String wechatNotify(@RequestBody String body, HttpServletRequest request) {
        return paymentService.handleNotify("wechat", body, collectNotifyHeaders(request));
    }

    /** 收集回调验签所需的关键请求头（微信 APIv3 回调验签依赖） */
    private Map<String, String> collectNotifyHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Wechatpay-Signature", request.getHeader("Wechatpay-Signature"));
        headers.put("Wechatpay-Timestamp", request.getHeader("Wechatpay-Timestamp"));
        headers.put("Wechatpay-Nonce", request.getHeader("Wechatpay-Nonce"));
        headers.put("Wechatpay-Serial", request.getHeader("Wechatpay-Serial"));
        return headers;
    }
}
