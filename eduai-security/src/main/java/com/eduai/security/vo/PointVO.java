package com.eduai.security.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 点数信息 VO
 */
@Data
@Builder
public class PointVO {

    /** 当前余额 */
    private Integer points;
}
