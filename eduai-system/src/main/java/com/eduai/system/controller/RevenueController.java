package com.eduai.system.controller;

import com.eduai.common.Result;
import com.eduai.system.dto.WithdrawRequestDTO;
import com.eduai.system.dto.WithdrawReviewDTO;
import com.eduai.system.entity.WithdrawRequest;
import com.eduai.system.service.TeacherRevenueService;
import com.eduai.system.vo.TeacherRevenueVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 老师资源下载分成 + 提现（老师端收益中心 / 管理员端提现审核）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RevenueController {

    private final TeacherRevenueService revenueService;

    /** 老师收益汇总 + 流水 */
    @GetMapping("/teacher/revenue")
    public Result<TeacherRevenueVO> getRevenue() {
        return Result.ok(revenueService.getRevenue());
    }

    /** 老师提交提现申请 */
    @PostMapping("/teacher/revenue/withdraw")
    public Result<WithdrawRequest> requestWithdraw(@Valid @RequestBody WithdrawRequestDTO dto) {
        log.info("POST /api/v1/teacher/revenue/withdraw amount={}", dto.getAmount());
        return Result.ok(revenueService.requestWithdraw(dto));
    }

    /** 管理员查看全部提现申请 */
    @GetMapping("/admin/revenue/withdraws")
    public Result<List<WithdrawRequest>> listAdminWithdraws() {
        return Result.ok(revenueService.listAdminWithdraws());
    }

    /** 管理员审核提现申请 */
    @PostMapping("/admin/revenue/withdraws/{id}/review")
    public Result<WithdrawRequest> reviewWithdraw(@PathVariable Long id,
                                                  @Valid @RequestBody WithdrawReviewDTO dto) {
        log.info("POST /api/v1/admin/revenue/withdraws/{}/review approved={}", id, dto.getApproved());
        return Result.ok(revenueService.reviewWithdraw(id, dto));
    }
}
