package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员记录
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "membership")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID (users.id) */
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    /** 方案: month/quarter/halfyear/year */
    @Column(length = 20)
    private String plan;

    /** 会员开始时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 会员到期时间 */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 状态: active/expired/cancelled */
    @Column(length = 10)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "active";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
