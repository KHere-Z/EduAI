package com.eduai.security.service.impl;

import com.eduai.common.Result;
import com.eduai.security.config.PaymentProperties;
import com.eduai.security.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付渠道路由（生产环境）
 * <p>
 * 微信与支付宝两个实现都是 {@code @Profile("prod")} 的 {@link PaymentService} bean，
 * 同时存在会让控制器注入产生歧义。本类标 {@link Primary}，成为生产环境下
 * {@link PaymentService} 的唯一注入点，再按渠道把请求分发给具体实现。
 * <p>
 * 分发规则：下单按请求体里的 {@code channel}（前端所选渠道），缺省回落到
 * {@code eduai.payment.channel} 配置；回调本身就带着渠道标识，直接按其分发。
 */
@Slf4j
@Service
@Primary
@Profile("prod")
@RequiredArgsConstructor
public class PaymentServiceRouter implements PaymentService {

    private final PaymentProperties paymentProperties;
    private final WechatPaymentServiceImpl wechatPaymentService;
    private final AlipayPaymentServiceImpl alipayPaymentService;

    /** 按渠道标识取实现，未知渠道返回 null */
    private PaymentService of(String channel) {
        if ("alipay".equalsIgnoreCase(channel)) {
            return alipayPaymentService;
        }
        if ("wechat".equalsIgnoreCase(channel)) {
            return wechatPaymentService;
        }
        return null;
    }

    /** 请求未指定渠道时，回落到配置的默认渠道 */
    private String defaultChannel() {
        return paymentProperties.getChannel();
    }

    @Override
    public Result<Map<String, Object>> createOrder(Long userId, Map<String, Object> body) {
        Object requested = body.get("channel");
        String channel = requested != null ? requested.toString() : defaultChannel();
        PaymentService service = of(channel);
        if (service == null) {
            return Result.error("不支持的支付渠道: " + channel);
        }
        return service.createOrder(userId, body);
    }

    /** 查单只看订单表，与渠道无关 */
    @Override
    public Result<Map<String, Object>> queryStatus(Long userId, String orderId) {
        PaymentService service = of(defaultChannel());
        if (service == null) {
            return Result.error("不支持的支付渠道: " + defaultChannel());
        }
        return service.queryStatus(userId, orderId);
    }

    @Override
    public Result<Void> mockPay(Long userId, String orderId) {
        PaymentService service = of(defaultChannel());
        if (service == null) {
            return Result.error("不支持的支付渠道: " + defaultChannel());
        }
        return service.mockPay(userId, orderId);
    }

    @Override
    public String handleNotify(String channel, Map<String, String> params, String body, Map<String, String> headers) {
        PaymentService service = of(channel);
        if (service == null) {
            log.error("[支付路由] 未知回调渠道: {}", channel);
            return "failure";
        }
        return service.handleNotify(channel, params, body, headers);
    }
}
