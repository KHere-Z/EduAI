package com.eduai.system.service;

import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.vo.KnowledgePointPageVO;
import com.eduai.system.vo.KnowledgePointVO;

/**
 * 知识点 Service
 */
public interface KnowledgePointService {

    /**
     * 分页查询知识点（老师端，按学科+年级筛选）
     */
    KnowledgePointPageVO list(int page, int pageSize, String subject, String gradeLevel);

    /**
     * 按多年级批量查询（学生端）
     */
    KnowledgePointPageVO listByGrades(int page, int pageSize, String subject, String grades);

    /**
     * 新增知识点
     */
    KnowledgePointVO create(KnowledgePointDTO dto);

    /**
     * 修改知识点
     */
    KnowledgePointVO update(Long id, KnowledgePointDTO dto);

    /**
     * 删除知识点
     */
    void delete(Long id);
}