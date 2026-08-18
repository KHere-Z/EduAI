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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
