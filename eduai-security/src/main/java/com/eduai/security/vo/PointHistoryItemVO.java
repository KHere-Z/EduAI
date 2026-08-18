package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智学点变动流水项 VO
 */
@Data
@Builder
public class PointHistoryItemVO {

    private Long id;
    /** 变动量（正=收入, 负=支出） */
    private Integer amount;
    /** 类型: charge/consume/refund/gift */
    private String type;
    /** 变动说明 */
    private String description;
    /** 变动后余额 */
    private Integer balanceAfter;
    private LocalDateTime createdAt;
}
