package com.eduai.security.service;

import com.eduai.security.vo.MembershipVO;
import com.eduai.security.vo.PointHistoryPageVO;
import com.eduai.security.vo.PointVO;

/**
 * 智学点服务
 */
public interface PointService {

    /** 查询当前用户点数余额 */
    PointVO getPoints(Long userId);

    /** 点数变动历史（分页） */
    PointHistoryPageVO getHistory(Long userId, int page, int pageSize);

    /** 充值（正数增加） */
    void charge(Long userId, int amount, String type, String description);

    /** 消费（扣点，余额不足抛异常） */
    void consume(Long userId, int amount, String description);

    /** 查询会员状态 */
    MembershipVO getMembership(Long userId);

    /** 开通/续费会员 */
    void activateMembership(Long userId, String plan);

    /**
     * 发放体验会员（注册赠送用），{@code days} 天后到期。
     * <p>
     * 与 {@link #activateMembership} 的区别：按**天**计算（后者只支持按月）、
     * **不赠送点数**（注册送的点数由调用方另行 charge）、方案代码为不可购买的 {@code trial}。
     * 已有有效会员时顺延到期时间，不覆盖原方案。
     */
    void grantTrialMembership(Long userId, int days);
}
