package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 老师提现申请
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "withdraw_requests")
public class WithdrawRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 老师用户ID (users.id) */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 提现金额（单位：分） */
    @Column(nullable = false)
    private Long amount;

    /** 开户行 */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /** 银行卡号 */
    @Column(name = "bank_card_no", length = 50)
    private String bankCardNo;

    /** 开户名 */
    @Column(name = "account_name", length = 50)
    private String accountName;

    /** 状态：pending=待审核 approved=已通过(待打款) paid=已打款 rejected=已驳回 */
    @Column(length = 20, nullable = false)
    private String status;

    /** 审核备注 */
    @Column(name = "review_note", length = 200)
    private String reviewNote;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "pending";
    }
}
