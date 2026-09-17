package com.eduai.security.service.impl;

import com.eduai.common.BusinessException;
import com.eduai.security.entity.Membership;
import com.eduai.security.entity.PointTransaction;
import com.eduai.security.entity.User;
import com.eduai.security.repository.MembershipRepository;
import com.eduai.security.repository.PointTransactionRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.PointService;
import com.eduai.security.vo.MembershipVO;
import com.eduai.security.vo.PointHistoryItemVO;
import com.eduai.security.vo.PointHistoryPageVO;
import com.eduai.security.vo.PointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 智学点服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserRepository userRepository;
    private final PointTransactionRepository transactionRepository;
    private final MembershipRepository membershipRepository;

    // 会员方案配置（充值即送固定点数，无折扣）
    private static final Map<String, PlanConfig> PLANS = Map.of(
            "month",    new PlanConfig("月卡", 80),
            "quarter",  new PlanConfig("季卡", 220),
            "halfyear", new PlanConfig("半年卡", 420),
            "year",     new PlanConfig("年卡", 800)
    );

    /**
     * 注册赠送的体验会员方案代码。
     * <p>
     * ⚠️ 刻意**不放进上面的 PLANS**：PLANS 同时是 {@link #activateMembership} 的方案白名单，
     * 一旦放进去，任何能触达 activateMembership 的路径都能用 "trial" 白拿一个月。
     * 试用期本就不可购买，展示名改由 {@link #planConfigOf} 单独解析。
     */
    private static final String TRIAL_PLAN = "trial";

    /** 体验会员的展示名（{@code MembershipVO.plan} 对外返回的是这个，不是方案代码） */
    private static final String TRIAL_PLAN_NAME = "体验会员";

    /** 方案展示信息（含不可购买的 trial） */
    private static PlanConfig planConfigOf(String plan) {
        if (TRIAL_PLAN.equals(plan)) {
            return new PlanConfig(TRIAL_PLAN_NAME, 0);
        }
        return PLANS.getOrDefault(plan, new PlanConfig("未知", 0));
    }

    // AI 消耗点数
    public static final int COST_AI_CHAT = 3;
    public static final int COST_AI_WRONG_ANALYSIS = 3;
    public static final int COST_AI_EXAM_ANALYSIS = 5;
    public static final int COST_AI_QUESTION_GRADE = 5;
    public static final int COST_AI_ANIMATION = 6;

    @Override
    public PointVO getPoints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));
        return PointVO.builder().points(user.getPoints()).build();
    }

    @Override
    public PointHistoryPageVO getHistory(Long userId, int page, int pageSize) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        var pageResult = transactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page - 1, pageSize));

        List<PointHistoryItemVO> items = pageResult.getContent().stream()
                .map(t -> PointHistoryItemVO.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .description(t.getDescription())
                        .balanceAfter(t.getBalanceAfter())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();

        return PointHistoryPageVO.builder()
                .list(items)
                .total(pageResult.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    public void charge(Long userId, int amount, String type, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        int before = user.getPoints() != null ? user.getPoints() : 0;
        user.setPoints(before + amount);
        userRepository.save(user);

        transactionRepository.save(PointTransaction.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .description(description)
                .balanceAfter(user.getPoints())
                .build());

        log.info("智学点充值: userId={} amount={} type={} balance={}", userId, amount, type, user.getPoints());
    }

    @Override
    @Transactional
    public void consume(Long userId, int amount, String description) {
        // SELECT ... FOR UPDATE 行锁，防止并发扣点超扣（必须在事务内，见方法级 @Transactional）
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        int before = user.getPoints() != null ? user.getPoints() : 0;
        if (before < amount) {
            throw new BusinessException(400, "智学点不足，当前余额 " + before + "，需要 " + amount);
        }

        user.setPoints(before - amount);
        userRepository.save(user);

        transactionRepository.save(PointTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .type("consume")
                .description(description)
                .balanceAfter(user.getPoints())
                .build());

        log.info("智学点消费: userId={} amount={} desc={} balance={}", userId, amount, description, user.getPoints());
    }

    @Override
    public MembershipVO getMembership(Long userId) {
        Optional<Membership> opt = membershipRepository.findByUserIdAndStatus(userId, "active");
        if (opt.isEmpty()) {
            return MembershipVO.builder().active(false).discount(1.0).monthlyPoints(0).build();
        }

        Membership m = opt.get();
        PlanConfig config = planConfigOf(m.getPlan());

        boolean isActive = m.getExpiresAt() != null
                && m.getExpiresAt().isAfter(LocalDateTime.now())
                && "active".equals(m.getStatus());

        return MembershipVO.builder()
                .active(isActive)
                .plan(config.name)
                .startedAt(m.getStartedAt())
                .expiresAt(m.getExpiresAt())
                .discount(1.0)
                .monthlyPoints(config.giftPoints)
                .build();
    }

    @Override
    @Transactional
    public void activateMembership(Long userId, String plan) {
        if (!PLANS.containsKey(plan)) {
            throw new BusinessException(400, "无效的会员方案: " + plan);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        PlanConfig config = PLANS.get(plan);

        // 查找现有会员
        Membership m = membershipRepository.findByUserId(userId).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (m != null && "active".equals(m.getStatus()) && m.getExpiresAt() != null && m.getExpiresAt().isAfter(now)) {
            // 续费：延长到期时间
            LocalDateTime newExpires = m.getExpiresAt().plusMonths(planMonths(plan));
            m.setExpiresAt(newExpires);
            m.setPlan(plan);
            m.setUpdatedAt(now);
            membershipRepository.save(m);
        } else {
            // 新开通
            LocalDateTime expires = now.plusMonths(planMonths(plan));
            m = Membership.builder()
                    .userId(userId)
                    .plan(plan)
                    .startedAt(now)
                    .expiresAt(expires)
                    .status("active")
                    .build();
            membershipRepository.save(m);
        }

        // 赠送固定点数
        charge(userId, config.giftPoints, "gift", "开通" + config.name + "赠送 " + config.giftPoints + " 点");

        log.info("会员开通: userId={} plan={} giftPoints={}", userId, plan, config.giftPoints);
    }

    @Override
    @Transactional
    public void grantTrialMembership(Long userId, int days) {
        if (days <= 0) {
            throw new BusinessException(400, "体验天数必须大于 0");
        }
        LocalDateTime now = LocalDateTime.now();

        // membership.user_id 上有唯一约束，故必须「有则顺延、无则插入」，不能无脑 insert
        Membership m = membershipRepository.findByUserId(userId).orElse(null);

        if (m != null && "active".equals(m.getStatus())
                && m.getExpiresAt() != null && m.getExpiresAt().isAfter(now)) {
            // 已有有效会员（如先付费后补发）→ 只顺延到期时间，不覆盖原方案
            m.setExpiresAt(m.getExpiresAt().plusDays(days));
            m.setUpdatedAt(now);
            membershipRepository.save(m);
        } else {
            // 无会员，或原记录已过期 → 新建（过期记录的 status 可能仍是 active，此处一并覆盖）
            m = Membership.builder()
                    .userId(userId)
                    .plan(TRIAL_PLAN)
                    .startedAt(now)
                    .expiresAt(now.plusDays(days))
                    .status("active")
                    .build();
            membershipRepository.save(m);
        }

        // 刻意不 charge 点数：注册赠送的点数是一笔独立流水，由调用方另行发放
        log.info("体验会员发放: userId={} days={} expiresAt={}", userId, days, m.getExpiresAt());
    }

    /** 获取会员折扣（非会员返回 1.0） */
    public double getDiscount(Long userId) {
        return getMembership(userId).getDiscount();
    }

    /** 方案对应周期（月） */
    private static int planMonths(String plan) {
        return switch (plan) {
            case "quarter" -> 3;
            case "halfyear" -> 6;
            case "year" -> 12;
            default -> 1; // month
        };
    }

    private record PlanConfig(String name, int giftPoints) {}
}
