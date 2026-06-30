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

    /** 用户名 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 密码（明文） */
    @Column(nullable = false, length = 200)
    private String password;

    /** 真实姓名 */
    @Column(length = 50)
    private String realName;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /** 邮箱 */
    @Column(length = 100)
    private String email;

    /** 角色类型：1=平台管理员 3=教师 4=学生 */
    @Column(nullable = false)
    private Integer roleType;

    /** 状态：1=正常 0=禁用 */
    @Column(nullable = false)
    private Integer status;

    /** 创建时间 */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}