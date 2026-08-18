package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /** 打卡日期列表（yyyy-MM-dd，升序），供前端日历标记 */
    private List<String> checkinDates;
}