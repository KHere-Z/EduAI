package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户微信绑定实体
 * <p>
 * 一对一绑定约束：一个 unionid 只能绑定一个用户，由数据库 UNIQUE 索引 + 应用层双重保证
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_wechat")
public class UserWechat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户UID → users.uid */
    @Column(name = "user_uid", nullable = false)
    private Long userUid;

    /** 微信 openid */
    @Column(nullable = false, length = 100)
    private String openid;

    /** 微信 unionid（唯一索引） */
    @Column(length = 100)
    private String unionid;

    /** 微信昵称 */
    @Column(length = 100)
    private String nickname;

    /** 微信头像URL */
    @Column(length = 500)
    private String avatar;

    /** 绑定时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
