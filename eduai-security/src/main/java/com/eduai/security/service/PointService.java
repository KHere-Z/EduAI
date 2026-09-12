package com.eduai.security.service;

import com.eduai.security.vo.MembershipVO;
import com.eduai.security.vo.PointHistoryPageVO;
import com.eduai.security.vo.PointVO;

import java.util.List;

/**
 * 智学点服务
 */
public interface PointService {

    /** 查询当前用户点数余额 */
    PointVO getPoints(Long userId);

    /** 点数变动历史（分页） */
    PointHistoryPageVO getHistory(Long userId, int page, int pageSize);

    /**
     * 批量（软）删除当前用户自己的点数变动记录，返回实际删除行数。
     * <p>
     * <b>软删</b>：行保留在库中供后台对账，仅对用户不可见（{@code deleted = true}）。
     * <p>
     * <b>不改动余额</b>：删除一条消费记录不会退回智学点，删除一条充值记录也不会扣减，
     * 否则删记录就等于凭空造点。
     */
    int deleteHistory(Long userId, List<Long> ids);

    /** 充值（正数增加） */
    void charge(Long userId, int amount, String type, String description);

    /** 消费（扣点，余额不足抛异常） */
    void consume(Long userId, int amount, String description);

    /** 查询会员状态 */
    MembershipVO getMembership(Long userId);

    /** 开通/续费会员 */
    void activateMembership(Long userId, String plan);
}
