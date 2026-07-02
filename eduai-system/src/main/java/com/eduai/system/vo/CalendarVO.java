package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 日历排课 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarVO {

    /** 日期 -> 当日排课列表 */
    private Map<String, List<CalendarEntry>> dates;

    /**
     * 单条排课条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarEntry {
        private Long studentId;
        private String studentName;
        private String subject;
        private String startTime;
        private String endTime;
    }
}