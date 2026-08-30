package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内消息（资源审核结果通知等）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 接收者 users.id */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 消息类型，如 RESOURCE_REVIEW */
    @Column(length = 50)
    private String type;

    /** 标题 */
    @Column(length = 200)
    private String title;

    /** 内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 是否已读（列名避免 MySQL 保留字 read） */
    @Column(name = "read_flag", nullable = false)
    @Builder.Default
    private Boolean readFlag = false;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.readFlag == null) this.readFlag = false;
    }
}
