package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.WithdrawRequestDTO;
import com.eduai.system.dto.WithdrawReviewDTO;
import com.eduai.system.entity.WithdrawRequest;
import com.eduai.system.repository.TeacherEarningRepository;
import com.eduai.system.repository.WithdrawRequestRepository;
import com.eduai.system.service.TeacherRevenueService;
import com.eduai.system.vo.TeacherRevenueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 老师资源下载分成 + 提现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherRevenueServiceImpl implements TeacherRevenueService {

    private static final long MIN_WITHDRAW = 100L; // 最低提现 1 元 = 100 分

    private final UserRepository userRepository;
    private final TeacherEarningRepository teacherEarningRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;

    /** 校验当前用户为教师（管理员也可） */
    private Long checkTeacher() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3 && user.getRoleType() != 1) {
            throw new BusinessException(403, "仅教师或管理员可访问");
        }
        return userId;
    }

    /** 校验当前用户为管理员 */
    private void checkAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 1) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherRevenueVO getRevenue() {
        Long teacherId = checkTeacher();
        long totalEarnings = teacherEarningRepository.sumAmountByTeacherId(teacherId);
        long nonRejected = withdrawRequestRepository.sumNonRejectedAmountByTeacherId(teacherId);
        List<WithdrawRequest> withdraws = withdrawRequestRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        long pending = withdraws.stream()
                .filter(w -> "pending".equals(w.getStatus()))
                .mapToLong(WithdrawRequest::getAmount)
                .sum();
        return TeacherRevenueVO.builder()
                .totalEarnings(totalEarnings)
                .availableBalance(totalEarnings - nonRejected)
                .pendingWithdraw(pending)
                .earnings(teacherEarningRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId))
                .withdraws(withdraws)
                .build();
    }

    @Override
    @Transactional
    public WithdrawRequest requestWithdraw(WithdrawRequestDTO dto) {
        Long teacherId = checkTeacher();
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new BusinessException(400, "金额不合法");
        }
        if (dto.getAmount() < MIN_WITHDRAW) {
            throw new BusinessException(400, "最低提现 1 元（100 智学点）");
        }
        if (isBlank(dto.getBankName()) || isBlank(dto.getBankCardNo()) || isBlank(dto.getAccountName())) {
            throw new BusinessException(400, "请填写完整银行卡信息");
        }

        long totalEarnings = teacherEarningRepository.sumAmountByTeacherId(teacherId);
        long nonRejected = withdrawRequestRepository.sumNonRejectedAmountByTeacherId(teacherId);
        long available = totalEarnings - nonRejected;
        if (dto.getAmount() > available) {
            throw new BusinessException(400, "可提现余额不足");
        }

        WithdrawRequest request = WithdrawRequest.builder()
                .teacherId(teacherId)
                .amount(dto.getAmount())
                .bankName(dto.getBankName())
                .bankCardNo(dto.getBankCardNo())
                .accountName(dto.getAccountName())
                .status("pending")
                .build();
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        log.info("老师 {} 申请提现 {} 分", teacherId, saved.getAmount());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequest> listAdminWithdraws() {
        checkAdmin();
        return withdrawRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public WithdrawRequest reviewWithdraw(Long id, WithdrawReviewDTO dto) {
        checkAdmin();
        WithdrawRequest request = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "提现申请不存在"));
        if (!"pending".equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已审核，不能重复操作");
        }
        request.setStatus(Boolean.TRUE.equals(dto.getApproved()) ? "approved" : "rejected");
        request.setReviewNote(dto.getNote());
        request.setReviewedAt(LocalDateTime.now());
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        log.info("管理员审核提现申请 id={} status={}", id, saved.getStatus());
        return saved;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
