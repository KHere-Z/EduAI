package com.eduai.system.service;

import com.eduai.system.dto.RescheduleDTO;
import com.eduai.system.vo.*;

/**
 * 学生端 Service
 */
public interface StudentPortalService {

    /** 学生报名科目列表（学科中心） */
    StudentEnrollmentVO getEnrollments();

    /** 学生课表 */
    StudentScheduleVO getSchedule(Integer year, Integer month);

    /** 提交调课申请 */
    RescheduleVO submitReschedule(RescheduleDTO dto);

    /** 学习打卡 */
    void checkin();

    /** 打卡连续天数 */
    StreakVO getStreak();
}