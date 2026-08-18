package com.eduai.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 老师提现申请入参
 */
@Data
public class WithdrawRequestDTO {

    /** 提现金额（单位：分，1元=100分） */
    @NotNull(message = "提现金额不能为空")
    @Min(value = 1, message = "提现金额必须大于0")
    private Long amount;

    @NotBlank(message = "开户行不能为空")
    private String bankName;

    @NotBlank(message = "银行卡号不能为空")
    private String bankCardNo;

    @NotBlank(message = "开户名不能为空")
    private String accountName;
}
