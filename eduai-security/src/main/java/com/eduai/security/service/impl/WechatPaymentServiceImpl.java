package com.eduai.security.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.eduai.common.Result;
import com.eduai.security.config.PaymentProperties;
import com.eduai.security.entity.PaymentOrder;
import com.eduai.security.repository.PaymentOrderRepository;
import com.eduai.security.service.PaymentService;
import com.eduai.security.service.PointService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.core.http.HttpClient;
import com.wechat.pay.java.core.http.HttpMethod;
import com.wechat.pay.java.core.http.HttpRequest;
import com.wechat.pay.java.core.http.HttpResponse;
import com.wechat.pay.java.core.http.JsonRequestBody;
import com.wechat.pay.java.core.http.JsonResponseBody;
import com.wechat.pay.java.core.http.ResponseBody;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 Native 实现（生产环境注册，channel=wechat）
 * <p>
 * 依赖官方 wechatpay-java SDK 完成 APIv3 下单签名与回调验签/解密，
 * 落库复用 {@link PointService#activateMembership} / {@link PointService#charge}。
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class WechatPaymentServiceImpl implements PaymentService {

    private final PaymentProperties paymentProperties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PointService pointService;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String NATIVE_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/native";

    /** 会员方案价格（分） */
    private static final Map<String, Integer> PRICES = Map.of(
            "month", 2900,      // 29元
            "quarter", 7900,    // 79元
            "halfyear", 13900,  // 139元
            "year", 19900       // 199元
    );

    /** 懒加载 SDK Config（微信支付公钥模式，下单签名用商户私钥） */
    private volatile Config config;

    /** 懒加载通知验签配置（公钥模式验签回调用微信支付公钥） */
    private volatile NotificationConfig notificationConfig;

    private Config config() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    PaymentProperties.Wechat w = paymentProperties.getWechat();
                    config = new RSAPublicKeyConfig.Builder()
                            .merchantId(w.getMchId())
                            .privateKeyFromPath(w.getPrivateKeyPath())
                            .merchantSerialNumber(w.getCertSerialNo())
                            .publicKeyFromPath(w.getPublicKeyPath())
                            .publicKeyId(w.getPublicKeyId())
                            .build();
                }
            }
        }
        return config;
    }

    private NotificationConfig notificationConfig() {
        if (notificationConfig == null) {
            synchronized (this) {
                if (notificationConfig == null) {
                    PaymentProperties.Wechat w = paymentProperties.getWechat();
                    notificationConfig = new RSAPublicKeyNotificationConfig.Builder()
                            .publicKeyFromPath(w.getPublicKeyPath())
                            .publicKeyId(w.getPublicKeyId())
                            .apiV3Key(w.getApiV3Key())
                            .build();
                }
            }
        }
        return notificationConfig;
    }

    private HttpClient client() {
        return new DefaultHttpClientBuilder().config(config()).build();
    }

    @Override
    public Result<Map<String, Object>> createOrder(Long userId, Map<String, Object> body) {
        String plan = body.get("plan") != null ? body.get("plan").toString() : null;
        Integer buyPoints = body.get("points") != null
                ? Integer.parseInt(body.get("points").toString()) : null;

        if (plan == null && buyPoints == null) {
            return Result.error("请选择会员方案或输入点数");
        }
        if (plan != null && !PRICES.containsKey(plan)) {
            return Result.error("无效的会员方案: " + plan);
        }

        // 金额计算：会员价 + 点数金额（1元=10点，1点=10分）
        int totalCents = 0;
        if (plan != null) totalCents += PRICES.get(plan);
        if (buyPoints != null) totalCents += buyPoints * 10;

        // 生成商户订单号（out_trade_no，≤32字符）
        String orderId = "wx" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
        paymentOrderRepository.save(PaymentOrder.builder()
                .orderId(orderId)
                .userId(userId)
                .plan(plan)
                .points(buyPoints != null ? buyPoints : 0)
                .totalCents(totalCents)
                .status("pending")
                .build());

        PaymentProperties.Wechat w = paymentProperties.getWechat();
        String description = plan != null ? ("会员充值-" + plan) : "智学点充值";
        try {
            Map<String, Object> amount = new LinkedHashMap<>();
            amount.put("total", totalCents);
            amount.put("currency", "CNY");

            Map<String, Object> req = new LinkedHashMap<>();
            req.put("appid", w.getAppId());
            req.put("mchid", w.getMchId());
            req.put("description", description);
            req.put("out_trade_no", orderId);
            req.put("notify_url", w.getNotifyUrl());
            req.put("amount", amount);

            String respBody = postJson(NATIVE_URL, JSON.writeValueAsString(req));
            JsonNode node = JSON.readTree(respBody);
            String codeUrl = node.path("code_url").asText();
            if (codeUrl == null || codeUrl.isBlank()) {
                throw new RuntimeException("微信下单未返回 code_url: " + respBody);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("orderId", orderId);
            resp.put("price", totalCents);
            resp.put("qrCode", codeUrl);   // 与 Mock 字段名一致，前端无需改动
            resp.put("status", "pending");
            if (plan != null) resp.put("plan", plan);
            if (buyPoints != null) resp.put("points", buyPoints);

            log.info("[微信支付] Native下单成功: orderId={} userId={} price={}分", orderId, userId, totalCents);
            return Result.ok(resp);
        } catch (Exception e) {
            log.error("[微信支付] Native下单失败: orderId={}", orderId, e);
            paymentOrderRepository.findByOrderId(orderId).ifPresent(o -> {
                o.setStatus("closed");
                paymentOrderRepository.save(o);
            });
            return Result.error("创建支付订单失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> queryStatus(Long userId, String orderId) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("无权查看他人订单");
        }
        return Result.ok(Map.of(
                "orderId", order.getOrderId(),
                "status", order.getStatus(),
                "price", order.getTotalCents()
        ));
    }

    @Override
    public Result<Void> mockPay(Long userId, String orderId) {
        return Result.error("生产环境不支持模拟支付");
    }

    @Override
    public String handleNotify(String channel, String body, Map<String, String> headers) {
        try {
            // 1. 验签 + 解密（依赖回调 HTTP 头）
            NotificationParser parser = new NotificationParser(notificationConfig());
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(headers.get("Wechatpay-Serial"))
                    .nonce(headers.get("Wechatpay-Nonce"))
                    .signature(headers.get("Wechatpay-Signature"))
                    .timestamp(headers.get("Wechatpay-Timestamp"))
                    .body(body)
                    .build();
            Transaction tx = parser.parse(requestParam, Transaction.class);

            String orderId = tx.getOutTradeNo();
            Transaction.TradeStateEnum tradeState = tx.getTradeState();
            String transactionId = tx.getTransactionId();

            log.info("[微信支付] 回调: orderId={} tradeState={} txId={}", orderId, tradeState, transactionId);

            if (!Transaction.TradeStateEnum.SUCCESS.equals(tradeState)) {
                log.warn("[微信支付] 非成功回调，忽略: orderId={} tradeState={}", orderId, tradeState);
                return "SUCCESS";
            }

            PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                log.error("[微信支付] 订单不存在: orderId={}", orderId);
                return "FAIL";
            }
            if ("paid".equals(order.getStatus())) {
                // 幂等：重复回调直接返回成功，避免重复发点
                return "SUCCESS";
            }

            // 2. 落库：开会员 + 充智学点
            if (order.getPlan() != null) {
                pointService.activateMembership(order.getUserId(), order.getPlan());
            }
            if (order.getPoints() != null && order.getPoints() > 0) {
                pointService.charge(order.getUserId(), order.getPoints(), "charge",
                        "充值 " + order.getPoints() + " 智学点");
            }

            order.setStatus("paid");
            order.setTransactionId(transactionId);
            paymentOrderRepository.save(order);

            log.info("[微信支付] 支付成功落库: orderId={} userId={} plan={} points={}",
                    orderId, order.getUserId(), order.getPlan(), order.getPoints());
            return "SUCCESS";
        } catch (Exception e) {
            log.error("[微信支付] 回调处理失败", e);
            return "FAIL";
        }
    }

    /** POST JSON 并返回响应体字符串 */
    private String postJson(String url, String json) throws Exception {
        HttpRequest request = new HttpRequest.Builder()
                .url(url)
                .httpMethod(HttpMethod.POST)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .body(new JsonRequestBody.Builder().body(json).build())
                .build();

        // 注意：不能用 getServiceResponse().getBody()——SDK 对 JsonResponseBody 反序列化后 body 为 null，
        // 真实响应体在 response.getBody()（原始响应体）里。
        // 下单失败（非 2xx）时 SDK 会抛 ServiceException（含错误码/信息），由上层 catch 打印。
        HttpResponse<JsonResponseBody> response = client().execute(request, JsonResponseBody.class);
        String respBody = rawBodyString(response);

        if (respBody == null || respBody.isBlank()) {
            log.error("[微信支付] 下单响应体为空，请求: {}", json);
            throw new RuntimeException("微信下单失败: 空响应");
        }
        return respBody;
    }

    /** 从原始响应体提取字符串（response.getBody()，而非 getServiceResponse()） */
    private String rawBodyString(HttpResponse<JsonResponseBody> response) {
        ResponseBody rawBody = response.getBody();
        if (rawBody instanceof JsonResponseBody) {
            return ((JsonResponseBody) rawBody).getBody();
        }
        return String.valueOf(rawBody);
    }
}
