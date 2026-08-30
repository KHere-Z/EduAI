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

    /** 资源折扣（固定 1.0，已取消会员折扣） */
    private Double discount;

    /** 会员充值赠送点数（开通即送，非按月） */
    private Integer monthlyPoints;
}
