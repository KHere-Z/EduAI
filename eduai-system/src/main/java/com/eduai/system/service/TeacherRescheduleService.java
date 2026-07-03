package com.eduai.system.service;

import com.eduai.system.vo.RescheduleVO;

import java.util.List;

/**
 * 老师端 - 调课管理 Service
 */
public interface TeacherRescheduleService {

    /** 查看学生的调课申请 */
    List<RescheduleVO> listReschedules();

    /** 批准调课（更新 session 日期时间） */
    RescheduleVO approve(Long id);

    /** 标记为待议 */
    RescheduleVO defer(Long id);

    /** 关闭/删除申请 */
    void close(Long id);
}