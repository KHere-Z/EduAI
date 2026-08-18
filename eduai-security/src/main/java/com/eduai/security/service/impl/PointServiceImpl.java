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

    // 会员方案配置
    private static final Map<String, PlanConfig> PLANS = Map.of(
            "month",   new PlanConfig("月卡", 500, 0.9),
            "quarter", new PlanConfig("季卡", 750, 0.7),
            "year",    new PlanConfig("年卡", 1000, 0.5)
    );

    // AI 消耗点数
    public static final int COST_AI_CHAT = 3;
    public static final int COST_AI_WRONG_ANALYSIS = 5;
    public static final int COST_AI_EXAM_ANALYSIS = 10;

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
        User user = userRepository.findById(userId)
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
        PlanConfig config = PLANS.getOrDefault(m.getPlan(), new PlanConfig("未知", 0, 1.0));

        boolean isActive = m.getExpiresAt() != null
                && m.getExpiresAt().isAfter(LocalDateTime.now())
                && "active".equals(m.getStatus());

        return MembershipVO.builder()
                .active(isActive)
                .plan(config.name)
                .startedAt(m.getStartedAt())
                .expiresAt(m.getExpiresAt())
                .discount(config.discount)
                .monthlyPoints(config.monthlyPoints)
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
            LocalDateTime newExpires = m.getExpiresAt().plusMonths("month".equals(plan) ? 1 : "quarter".equals(plan) ? 3 : 12);
            m.setExpiresAt(newExpires);
            m.setPlan(plan);
            m.setUpdatedAt(now);
            membershipRepository.save(m);
        } else {
            // 新开通
            LocalDateTime expires = now.plusMonths("month".equals(plan) ? 1 : "quarter".equals(plan) ? 3 : 12);
            m = Membership.builder()
                    .userId(userId)
                    .plan(plan)
                    .startedAt(now)
                    .expiresAt(expires)
                    .status("active")
                    .build();
            membershipRepository.save(m);
        }

        // 赠送月度点数
        charge(userId, config.monthlyPoints, "gift", "开通" + config.name + "赠送 " + config.monthlyPoints + " 点");

        log.info("会员开通: userId={} plan={} monthlyPoints={}", userId, plan, config.monthlyPoints);
    }

    /** 获取会员折扣（非会员返回 1.0） */
    public double getDiscount(Long userId) {
        return getMembership(userId).getDiscount();
    }

    private record PlanConfig(String name, int monthlyPoints, double discount) {}
}
