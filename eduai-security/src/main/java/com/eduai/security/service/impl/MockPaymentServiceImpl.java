package com.eduai.security.service.impl;

import com.eduai.common.Result;
import com.eduai.security.service.PaymentService;
import com.eduai.security.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock 支付实现（仅开发环境注册）
 * <p>
 * 生产接入真实 SDK 时，新增 {@code AlipayPaymentServiceImpl} / {@code WechatPaymentServiceImpl}
 * 并标注 {@code @Profile("prod")}，实现 {@link PaymentService}。
 */
@Slf4j
@Service
@Profile("!prod")
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements PaymentService {

    private final PointService pointService;

    /** Mock 订单存储 */
    private static final ConcurrentHashMap<String, PaymentOrder> ORDERS = new ConcurrentHashMap<>();

    /** 价格映射（分） */
    private static final Map<String, Integer> PRICES = Map.of(
            "month",    2900,   // 29元
            "quarter",  7900,   // 79元
            "halfyear", 13900,  // 139元
            "year",     19900   // 199元
    );

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

        // 价格计算：会员价 + 点数金额（1元=10点，1点=10分）
        int totalCents = 0;
        if (plan != null) totalCents += PRICES.get(plan);
        if (buyPoints != null) totalCents += buyPoints * 10;  // 1点=10分

        String orderId = UUID.randomUUID().toString().substring(0, 8);
        ORDERS.put(orderId, new PaymentOrder(orderId, userId, plan,
                buyPoints != null ? buyPoints : 0, totalCents, "pending"));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("orderId", orderId);
        resp.put("price", totalCents);
        if (plan != null) resp.put("plan", plan);
        if (buyPoints != null) resp.put("points", buyPoints);
        resp.put("qrCode", "mock-qrcode-" + orderId);
        resp.put("status", "pending");

        log.info("[Mock支付] 创建订单: orderId={} userId={} plan={} points={} price={}分",
                orderId, userId, plan, buyPoints, totalCents);

        return Result.ok(resp);
    }

    @Override
    public Result<Map<String, Object>> queryStatus(Long userId, String orderId) {
        PaymentOrder order = ORDERS.get(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.userId.equals(userId)) {
            return Result.error("无权查看他人订单");
        }
        return Result.ok(Map.of(
                "orderId", order.orderId,
                "status", order.status,
                "price", order.price
        ));
    }

    @Override
    public Result<Void> mockPay(Long userId, String orderId) {
        PaymentOrder order = ORDERS.get(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.userId.equals(userId)) {
            return Result.error("无权操作他人订单");
        }
        if (!"pending".equals(order.status)) {
            return Result.error("订单已处理");
        }

        // 开通会员（如有选择方案）
        if (order.plan != null) {
            pointService.activateMembership(order.userId, order.plan);
        }

        // 充值点数（如有购买点数）
        if (order.points > 0) {
            pointService.charge(order.userId, order.points, "charge",
                    "充值 " + order.points + " 智学点");
        }

        ORDERS.put(orderId, new PaymentOrder(order.orderId, order.userId,
                order.plan, order.points, order.price, "paid"));

        log.info("[Mock支付] 支付成功: orderId={} userId={} plan={} points={} price={}分",
                orderId, order.userId, order.plan, order.points, order.price);
        return Result.ok();
    }

    @Override
    public String handleNotify(String channel, String body) {
        log.info("[支付回调-{}] body={}", channel, body);
        return "success";
    }

    /** Mock 订单 */
    private record PaymentOrder(String orderId, Long userId, String plan, int points, int price, String status) {}
}
