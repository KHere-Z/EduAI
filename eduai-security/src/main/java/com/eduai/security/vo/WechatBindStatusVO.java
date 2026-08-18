package com.eduai.security.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信绑定状态 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WechatBindStatusVO {

    /** 是否已绑定微信 */
    private Boolean bound;

    /** 绑定的微信昵称 */
    private String wechatNickname;

    /** 绑定时间 */
    private String boundAt;
}
