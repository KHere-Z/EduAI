package com.eduai.system.vo;

import com.eduai.system.entity.TeacherEarning;
import com.eduai.system.entity.WithdrawRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 老师收益中心汇总 + 流水
 */
@Data
@Builder
public class TeacherRevenueVO {

    /** 累计分成总额（分） */
    private long totalEarnings;

    /** 可提现余额（分）= 累计分成 - 未驳回的提现申请 */
    private long availableBalance;

    /** 待审核提现总额（分） */
    private long pendingWithdraw;

    /** 收益流水（倒序） */
    private List<TeacherEarning> earnings;

    /** 提现记录（倒序） */
    private List<WithdrawRequest> withdraws;
}
