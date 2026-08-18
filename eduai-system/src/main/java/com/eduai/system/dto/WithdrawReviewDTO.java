package com.eduai.system.dto;

import lombok.Data;

/**
 * 管理员提现审核入参
 */
@Data
public class WithdrawReviewDTO {

    /** true=通过，false=驳回 */
    private Boolean approved;

    /** 审核备注 */
    private String note;
}
