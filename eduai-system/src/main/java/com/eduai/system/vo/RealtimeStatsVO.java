package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员 - 实时埋点统计 VO
 * <p>
 * 对应 GET /api/v1/admin/stats/realtime，前端 DashboardView 30s 轮询展示。
 * 前 4 个为最近 5 分钟去重活跃数，最后一个为累计下载量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeStatsVO {

    /** 当前在线人数（最近 5 分钟有请求的去重用户） */
    private Long onlineCount;

    /** 试卷分析使用中 */
    private Long examAnalysisActive;

    /** AI 动图使用中 */
    private Long aiAnimationActive;

    /** 错题分析使用中 */
    private Long wrongAnalysisActive;

    /** 资源累计下载量 */
    private Long resourceDownloadTotal;
}
