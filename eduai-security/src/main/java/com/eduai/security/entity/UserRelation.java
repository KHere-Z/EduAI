package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户关系实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_relations")
public class UserRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发起方 UID */
    @Column(name = "from_uid", nullable = false)
    private Long fromUid;

    /** 接收方 UID */
    @Column(name = "to_uid", nullable = false)
    private Long toUid;

    /** pending / accepted / rejected */
    @Column(length = 20, nullable = false)
    private String status;

    /** 关系类型：teacher_student（师生）/ colleague（同事） */
    @Column(length = 20, nullable = false)
    private String type;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (status == null) this.status = "pending";
        if (type == null) this.type = "teacher_student";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
