package com.eduai.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理员视角 - 排课查看 VO（可按老师过滤）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminScheduleVO {

    private List<ScheduleItem> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

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

        /** 教师信息 */
        private Long teacherId;
        private String teacherName;

        /** 学生信息 */
        private Long studentId;
        private String studentName;
    }
}