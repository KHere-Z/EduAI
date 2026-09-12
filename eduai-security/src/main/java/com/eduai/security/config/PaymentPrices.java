package com.eduai.security.config;

import java.util.Map;

/**
 * 支付价格常量（会员方案 / 智学点）
 * <p>
 * Mock、微信、支付宝三个渠道实现共用，避免价格表在多处重复维护导致漂移。
 */
public final class PaymentPrices {

    private PaymentPrices() {
    }

    /** 会员方案价格（分） */
    public static final Map<String, Integer> PLANS = Map.of(
            "month", 2900,      // 29元
            "quarter", 7900,    // 79元
            "halfyear", 13900,  // 139元
            "year", 19900       // 199元
    );

    /** 1 智学点 = 10 分（即 1元 = 10点） */
    public static final int CENTS_PER_POINT = 10;

    public static boolean isPlan(String plan) {
        return plan != null && PLANS.containsKey(plan);
    }

    /**
     * 计算订单总额（分）：会员价 + 点数金额
     *
     * @param plan       会员方案，可为 null
     * @param buyPoints  购买点数，可为 null
     * @return 总额（分）
     */
    public static int totalCents(String plan, Integer buyPoints) {
        int total = 0;
        if (plan != null) {
            total += PLANS.getOrDefault(plan, 0);
        }
        if (buyPoints != null) {
            total += buyPoints * CENTS_PER_POINT;
        }
        return total;
    }
}
