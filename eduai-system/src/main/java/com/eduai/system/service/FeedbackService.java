package com.eduai.system.service;

import com.eduai.system.dto.FeedbackDTO;
import com.eduai.system.vo.FeedbackVO;

import java.util.List;

/**
 * 学习反馈 Service
 */
public interface FeedbackService {

    /** 老师写反馈 */
    FeedbackVO create(FeedbackDTO dto);

    /** 老师查看已发送反馈列表（按学科筛选） */
    List<FeedbackVO> listTeacherFeedbacks(String subject);

    /** 老师删除反馈 */
    void delete(Long id);

    /** 学生收到的反馈（按学科筛选） */
    List<FeedbackVO> listStudentFeedbacks(String subject);
}
