package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 - 概览统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsVO {

    /** 教师总数 */
    private Long teacherCount;

    /** 学生总数 */
    private Long studentCount;

    /** 总排课课时 */
    private Long totalHours;

    /** 老师-学生关系总数 */
    private Long relationCount;

    /** 排课记录总数 */
    private Long sessionCount;

    /** 报名科目总数 */
    private Long enrollmentCount;

    /**
     * 日活：今日登录过的去重用户数。
     * <p>
     * ⚠️ 口径是「登录日活」（{@code users.last_login}），不是「访问日活」。
     * 详见 {@code AdminServiceImpl#getStats} 的说明。
     */
    private Long dailyActiveUsers;

    /** 月活：近 30 天登录过的去重用户数（同一口径） */
    private Long monthlyActiveUsers;
}