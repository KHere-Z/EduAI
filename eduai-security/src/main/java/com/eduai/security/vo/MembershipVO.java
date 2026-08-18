package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员信息 VO
 */
@Data
@Builder
public class MembershipVO {

    /** 是否有有效会员 */
    private boolean active;

    /** 会员方案 */
    private String plan;

    /** 会员开始时间 */
    private LocalDateTime startedAt;

    /** 会员到期时间 */
    private LocalDateTime expiresAt;

    /** 资源折扣（0.5~1.0） */
    private Double discount;

    /** 每月赠送点数 */
    private Integer monthlyPoints;
}
