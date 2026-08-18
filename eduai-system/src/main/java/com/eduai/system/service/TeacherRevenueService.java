package com.eduai.system.service;

import com.eduai.system.dto.WithdrawRequestDTO;
import com.eduai.system.dto.WithdrawReviewDTO;
import com.eduai.system.entity.WithdrawRequest;
import com.eduai.system.vo.TeacherRevenueVO;

import java.util.List;

/**
 * 老师资源下载分成 + 提现
 */
public interface TeacherRevenueService {

    /** 老师收益汇总 + 流水（按当前登录用户） */
    TeacherRevenueVO getRevenue();

    /** 老师提交提现申请 */
    WithdrawRequest requestWithdraw(WithdrawRequestDTO dto);

    /** 管理员查看全部提现申请 */
    List<WithdrawRequest> listAdminWithdraws();

    /** 管理员审核提现申请 */
    WithdrawRequest reviewWithdraw(Long id, WithdrawReviewDTO dto);
}
