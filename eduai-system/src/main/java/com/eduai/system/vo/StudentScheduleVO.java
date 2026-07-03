package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学生端 - 课表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentScheduleVO {

    private List<ScheduleItem> schedules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleItem {
        private Long sessionId;
        private String classDate;
        private String startTime;
        private String endTime;
        private String subject;
        private String teacherName;
        private Long teacherId;
    }
}