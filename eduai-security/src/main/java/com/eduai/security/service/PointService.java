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
}
