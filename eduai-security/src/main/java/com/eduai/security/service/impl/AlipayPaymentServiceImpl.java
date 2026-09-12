package com.eduai.security.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.eduai.common.Result;
import com.eduai.security.config.PaymentPrices;
import com.eduai.security.config.PaymentProperties;
import com.eduai.security.entity.PaymentOrder;
import com.eduai.security.repository.PaymentOrderRepository;
import com.eduai.security.service.PaymentService;
import com.eduai.security.service.PointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝「电脑网站支付」实现（生产环境注册）
 * <p>
 * 走官方 SDK 的证书模式（{@link CertAlipayRequest}）：下单用 {@code alipay.trade.page.pay}
 * 生成跳转收银台的 URL，回调用 {@link AlipaySignature#rsaCertCheckV1} 验签。
 * 落库复用 {@link PointService#activateMembership} / {@link PointService#charge}，与微信渠道语义一致。
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class AlipayPaymentServiceImpl implements PaymentService {

    private final PaymentProperties paymentProperties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PointService pointService;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CHARSET = "UTF-8";

    /** 电脑网站支付产品码 */
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";

    /** 懒加载 SDK 客户端（证书模式构造时会读取证书文件，故延迟初始化） */
    private volatile AlipayClient client;

    private AlipayClient client() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    PaymentProperties.Alipay a = paymentProperties.getAlipay();
                    CertAlipayRequest cert = new CertAlipayRequest();
                    cert.setServerUrl(a.getGateway());
                    cert.setAppId(a.getAppId());
                    cert.setPrivateKey(resolvePrivateKey(a));
                    cert.setFormat("json");
                    cert.setCharset(CHARSET);
                    cert.setSignType(a.getSignType());
                    cert.setCertPath(a.getCertPath());
                    cert.setAlipayPublicCertPath(a.getAlipayPublicCertPath());
                    cert.setRootCertPath(a.getRootCertPath());
                    client = new DefaultAlipayClient(cert);
                }
            }
        }
        return client;
    }

    /** 应用私钥：优先读文件（多行 PEM），未配路径时回落到内联字符串 */
    private String resolvePrivateKey(PaymentProperties.Alipay a) throws Exception {
        String path = a.getPrivateKeyPath();
        if (path != null && !path.isBlank()) {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        }
        return a.getPrivateKey();
    }

    @Override
    public Result<Map<String, Object>> createOrder(Long userId, Map<String, Object> body) {
        String plan = body.get("plan") != null ? body.get("plan").toString() : null;
        Integer buyPoints = body.get("points") != null
                ? Integer.parseInt(body.get("points").toString()) : null;

        if (plan == null && buyPoints == null) {
            return Result.error("请选择会员方案或输入点数");
        }
        if (plan != null && !PaymentPrices.isPlan(plan)) {
            return Result.error("无效的会员方案: " + plan);
        }

        int totalCents = PaymentPrices.totalCents(plan, buyPoints);
        if (totalCents <= 0) {
            return Result.error("订单金额必须大于 0");
        }

        // 生成商户订单号（out_trade_no，≤32字符）
        String orderId = "ali" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
        paymentOrderRepository.save(PaymentOrder.builder()
                .orderId(orderId)
                .userId(userId)
                .plan(plan)
                .points(buyPoints != null ? buyPoints : 0)
                .totalCents(totalCents)
                .status("pending")
                .build());

        PaymentProperties.Alipay a = paymentProperties.getAlipay();
        String description = plan != null ? ("会员充值-" + plan) : "智学点充值";
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(a.getNotifyUrl());
            request.setReturnUrl(a.getReturnUrl());

            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", orderId);
            biz.put("total_amount", centsToYuan(totalCents));
            biz.put("subject", description);
            biz.put("product_code", PRODUCT_CODE);
            request.setBizContent(JSON.writeValueAsString(biz));

            // GET 模式返回可直接跳转的 URL（POST 模式返回 HTML 表单，不适合 SPA）
            AlipayTradePagePayResponse response = client().pageExecute(request, "GET");
            String payUrl = response != null ? response.getBody() : null;
            if (payUrl == null || payUrl.isBlank()) {
                String detail = response != null
                        ? (response.getSubMsg() != null ? response.getSubMsg() : response.getMsg())
                        : "无响应";
                throw new RuntimeException("支付宝下单未返回跳转地址: " + detail);
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("orderId", orderId);
            resp.put("price", totalCents);
            resp.put("payUrl", payUrl);   // 前端据此跳转支付宝收银台
            resp.put("status", "pending");
            if (plan != null) resp.put("plan", plan);
            if (buyPoints != null) resp.put("points", buyPoints);

            log.info("[支付宝] 下单成功: orderId={} userId={} price={}分", orderId, userId, totalCents);
            return Result.ok(resp);
        } catch (Exception e) {
            log.error("[支付宝] 下单失败: orderId={}", orderId, e);
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
    @Transactional
    public String handleNotify(String channel, Map<String, String> params, String body, Map<String, String> headers) {
        try {
            if (params == null || params.isEmpty()) {
                log.error("[支付宝] 回调参数为空");
                return "failure";
            }

            // 1. 证书模式验签
            PaymentProperties.Alipay a = paymentProperties.getAlipay();
            boolean verified = AlipaySignature.rsaCertCheckV1(
                    params, a.getAlipayPublicCertPath(), CHARSET, a.getSignType());
            if (!verified) {
                log.error("[支付宝] 回调验签失败: out_trade_no={}", params.get("out_trade_no"));
                return "failure";
            }

            String orderId = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            log.info("[支付宝] 回调: orderId={} tradeStatus={} tradeNo={}", orderId, tradeStatus, tradeNo);

            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.warn("[支付宝] 非成功回调，忽略: orderId={} tradeStatus={}", orderId, tradeStatus);
                return "success";
            }

            PaymentOrder order = paymentOrderRepository.findByOrderId(orderId).orElse(null);
            if (order == null) {
                log.error("[支付宝] 订单不存在: orderId={}", orderId);
                return "failure";
            }

            // 2. 原子认领：仅 pending → paid 才继续发点，靠 DB 原子性挡住渠道重复回调
            if (paymentOrderRepository.markPaid(orderId, tradeNo) == 0) {
                log.info("[支付宝] 订单已处理，幂等跳过: orderId={}", orderId);
                return "success";
            }

            // 3. 落库：开会员 + 充智学点
            if (order.getPlan() != null) {
                pointService.activateMembership(order.getUserId(), order.getPlan());
            }
            if (order.getPoints() != null && order.getPoints() > 0) {
                pointService.charge(order.getUserId(), order.getPoints(), "charge",
                        "充值 " + order.getPoints() + " 智学点");
            }

            log.info("[支付宝] 支付成功落库: orderId={} userId={} plan={} points={}",
                    orderId, order.getUserId(), order.getPlan(), order.getPoints());
            return "success";
        } catch (Exception e) {
            log.error("[支付宝] 回调处理失败", e);
            return "failure";   // 返回 failure 让支付宝重试
        }
    }

    /** 分 → 元（保留 2 位小数，支付宝 total_amount 要求） */
    private String centsToYuan(int cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
