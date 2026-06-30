package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教师信息实体 — 与 users 表 1:1 关联
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联用户ID（1:1） */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 任教学科，逗号分隔，如 "math,physics" */
    @Column(name = "subject_ids", nullable = false, length = 500)
    private String subjectIds;

    /** 所属机构ID */
    @Column(name = "org_id")
    private Long orgId;

    /** 职称（高级教师、一级教师等） */
    @Column(length = 100)
    private String title;

    /** 个人简介 */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** 头像URL */
    @Column(length = 255)
    private String avatar;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}