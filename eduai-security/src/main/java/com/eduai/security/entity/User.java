package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对外唯一ID（雪花算法，用于API暴露） */
    @Column(unique = true)
    private Long uid;

    /** 用户名（手机号登录时可为 NULL） */
    @Column(length = 50)
    private String username;

    /** 密码（BCrypt 哈希，手机号登录时可为 NULL） */
    @Column(length = 200)
    private String password;

    /** 真实姓名 */
    @Column(name = "real_name", length = 50)
    private String realName;

    /** 昵称 */
    @Column(length = 50)
    private String nickname;

    /** 头像URL */
    @Column(length = 500)
    private String avatar;

    /** 个人简介 */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** 学科数组 JSON（如 ["math","physics"]） */
    @Column(columnDefinition = "JSON")
    private String subjects;

    /** 教师专属：学员UID列表 JSON */
    @Column(name = "student_uids", columnDefinition = "JSON")
    private String studentUids;

    /** 学生专属：老师UID */
    @Column(name = "teacher_uid")
    private Long teacherUid;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /** 邮箱 */
    @Column(length = 100)
    private String email;

    /** 角色类型：1=平台管理员 3=教师 4=学生 */
    @Column(name = "role_type", nullable = false)
    private Integer roleType;

    /** 状态：1=正常 0=禁用 */
    @Column(nullable = false)
    private Integer status;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 智学点余额 */
    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    /** 最后登录时间 */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (status == null) this.status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
