package com.eduai.system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生实体（全局唯一，管理员维护基本档案）
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 姓名 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 性别：男/女 */
    @Column(length = 10)
    private String gender;

    /** 联系方式 */
    @Column(length = 20)
    private String contact;

    /** 年级 */
    @Column(length = 20)
    private String grade;

    /** 所在学校 */
    @Column(length = 100)
    private String school;

    /**
     * 关联用户ID（学生登录账号 → users.id）。
     * <p>
     * 自助注册时<b>必定写入</b>（AuthServiceImpl.createStudentProfile）；管理员当年手工
     * 建的存量档案可能为 NULL。索引 {@code uk_user_id} 为唯一索引，由
     * {@code docs/sql/migrate-students-user-id.sql} 负责创建（刻意不加
     * {@code @Column(unique = true)}：dev 的 ddl-auto=update 在脏数据上建唯一约束会启动失败）。
     */
    @Column(name = "user_id")
    private Long userId;

    /** 学科列表 JSON（如 ["数学","英语"]），以用户个人中心修改为准 */
    @Column(columnDefinition = "JSON")
    private String subjects;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.gender == null) {
            this.gender = "男";
        } else if (this.gender.isBlank()) {
            this.gender = "男";
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}