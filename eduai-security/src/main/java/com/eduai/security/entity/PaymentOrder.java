package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付订单实体（微信 Native 支付）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商户订单号（out_trade_no，唯一） */
    @Column(name = "order_id", nullable = false, unique = true, length = 32)
    private String orderId;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 会员方案（month/quarter/halfyear/year，可空） */
    @Column(length = 20)
    private String plan;

    /** 充值智学点（可空） */
    private Integer points;

    /** 金额（分） */
    @Column(name = "total_cents", nullable = false)
    private Integer totalCents;

    /** 状态：pending/paid/closed */
    @Column(nullable = false, length = 20)
    private String status;

    /** 微信支付交易号（回调成功后回填） */
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "pending";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
