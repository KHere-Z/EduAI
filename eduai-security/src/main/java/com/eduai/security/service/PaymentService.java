package com.eduai.security.service;

import com.eduai.common.Result;

import java.util.Map;

/**
 * 支付服务抽象 — 充值下单 / 查单 / 回调处理
 * <p>
 * 开发环境由 {@code MockPaymentServiceImpl}（{@code @Profile("!prod")}）实现；
 * 生产接入真实支付宝/微信 SDK 时，实现本接口并配合
 * {@link com.eduai.security.config.PaymentProperties} 指定渠道即可，前端无需改动。
 */
public interface PaymentService {

    /** 创建充值订单（返回 orderId / price / 二维码等） */
    Result<Map<String, Object>> createOrder(Long userId, Map<String, Object> body);

    /** 查询订单状态（前端轮询） */
    Result<Map<String, Object>> queryStatus(Long userId, String orderId);

    /** 模拟支付成功（仅开发环境，生产应移除） */
    Result<Void> mockPay(Long userId, String orderId);

    /**
     * 处理渠道异步回调，返回给渠道的应答文本（如 "success"）
     *
     * @param channel 渠道标识：wechat / alipay
     * @param params  表单参数（支付宝回调为 application/x-www-form-urlencoded，验签依赖此 Map），微信回调可为空
     * @param body    原始请求体（微信 APIv3 验签依赖此原文）
     * @param headers 关键请求头（微信 APIv3 验签依赖）
     */
    String handleNotify(String channel, Map<String, String> params, String body, Map<String, String> headers);
}
