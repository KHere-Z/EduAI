package com.eduai.system.service;

import com.eduai.system.dto.AdminSettingsDTO;
import com.eduai.system.dto.AdminStudentDTO;
import com.eduai.system.dto.AdminTeacherDTO;
import com.eduai.system.vo.*;

import java.util.Map;

/**
 * 管理员 Service
 */
public interface AdminService {

    // ==================== 学生管理 ====================

    /**
     * 全局学生列表（不过滤 teacher_id）
     */
    AdminStudentPageVO listStudents(int page, int pageSize, String keyword, String grade);

    /**
     * 新增学生（仅基本档案）
     */
    AdminStudentVO createStudent(AdminStudentDTO dto);

    /**
     * 更新学生
     */
    AdminStudentVO updateStudent(Long id, AdminStudentDTO dto);

    /**
     * 删除学生（级联删除关联数据）
     */
    void deleteStudent(Long id);

    // ==================== 老师管理 ====================

    /**
     * 老师列表（含学生数/课时汇总）
     */
    AdminTeacherPageVO listTeachers(int page, int pageSize, String keyword);

    /**
     * 老师详情 + 学生列表
     */
    AdminTeacherDetailVO getTeacherDetail(Long teacherId);

    /**
     * 新增老师（创建 User + Teacher）
     */
    AdminTeacherVO createTeacher(AdminTeacherDTO dto);

    /**
     * 更新老师信息
     */
    AdminTeacherVO updateTeacher(Long userId, AdminTeacherDTO dto);

    /**
     * 删除老师（删除 Teacher + User + 相关关系）
     */
    void deleteTeacher(Long userId);

    // ==================== 排课查看 ====================

    /**
     * 全部排课（可按 teacher_id/日期范围 过滤）
     */
    AdminScheduleVO listSchedules(Long teacherId, Integer year, Integer month, int page, int pageSize);

    // ==================== 概览统计 ====================

    /**
     * 概览统计（老师数/学生数/总课时等）
     */
    AdminStatsVO getStats();

    // ==================== 系统设置 ====================

    /**
     * 获取当前配置
     */
    Map<String, String> getSettings();

    /**
     * 更新配置
     */
    Map<String, String> updateSettings(AdminSettingsDTO dto);
}