package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智学点变动记录
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID (users.id) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 变动量（正=充值, 负=消费） */
    @Column(nullable = false)
    private Integer amount;

    /** 类型: charge/consume/refund/gift */
    @Column(length = 20, nullable = false)
    private String type;

    /** 变动说明 */
    @Column(length = 200)
    private String description;

    /** 变动后余额 */
    @Column(name = "balance_after")
    private Integer balanceAfter;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 软删标记：true=用户已从流水中删除，仅对用户不可见。
     * <p>
     * 这张表是支付对账依据，物理删除会让后台再也对不上渠道流水，故用软删——
     * 行永远保留，只有查询过滤。余额不受删除影响。
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.deleted == null) {
            this.deleted = false;
        }
    }
}
