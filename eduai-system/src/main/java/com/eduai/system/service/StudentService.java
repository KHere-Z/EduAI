package com.eduai.system.service;

import com.eduai.system.dto.StudentDTO;
import com.eduai.system.vo.CalendarVO;
import com.eduai.system.vo.StudentPageVO;
import com.eduai.system.vo.StudentVO;

/**
 * 学生管理 Service
 */
public interface StudentService {

    /**
     * 分页查询老师的学生列表（通过 teacher_student 关联）
     */
    StudentPageVO list(int page, int pageSize, String keyword, String subject, String grade);

    /**
     * 查询单个老师-学生关系详情
     */
    StudentVO getById(Long id);

    /**
     * 添加学生（查找或创建 Student，再创建 TeacherStudent 关系）
     */
    StudentVO create(StudentDTO dto);

    /**
     * 更新老师-学生关系（重新设置 enrollments）
     */
    StudentVO update(Long id, StudentDTO dto);

    /**
     * 删除老师-学生关系（级联删除 enrollments 和 sessions）
     */
    void delete(Long id);

    /**
     * 调整剩余课时
     */
    void updateHours(Long id, int delta);

    /**
     * 日历排课查询
     */
    CalendarVO calendar(int year, int month, Long teacherId);
}