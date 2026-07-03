package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打卡连续天数 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakVO {

    /** 当前连续打卡天数 */
    private int streak;

    /** 总打卡天数 */
    private long totalDays;

    /** 今日是否已打卡 */
    private boolean checkedInToday;
}