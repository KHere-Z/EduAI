package com.eduai.security.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 机构实体
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 机构名称 */
    @Column(nullable = false, length = 200)
    private String name;

    /** 机构类型：1=学校 2=培训机构 */
    private Integer type;

    /** 地址 */
    @Column(length = 500)
    private String address;

    /** 联系方式 */
    @Column(length = 100)
    private String contact;

    /** 状态：1=正常 0=禁用 */
    private Integer status;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}