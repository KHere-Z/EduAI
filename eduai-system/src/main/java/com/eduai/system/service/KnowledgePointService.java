package com.eduai.system.service;

import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.vo.KnowledgePointPageVO;
import com.eduai.system.vo.KnowledgePointVO;
import com.eduai.system.vo.KpResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识点 Service
 */
public interface KnowledgePointService {

    /** 分页查询知识点（老师端，按学科+年级筛选） */
    KnowledgePointPageVO list(int page, int pageSize, String subject, String gradeLevel);

    /** 按多年级批量查询（学生端） */
    KnowledgePointPageVO listByGrades(int page, int pageSize, String subject, String grades);

    /** 新增知识点 */
    KnowledgePointVO create(KnowledgePointDTO dto);

    /** 修改知识点 */
    KnowledgePointVO update(Long id, KnowledgePointDTO dto);

    /** 删除知识点 */
    void delete(Long id);

    // ==================== 知识点资源 ====================

    /** 上传资源文件 */
    KpResourceVO uploadResource(Long kpId, MultipartFile file, String tag);

    /** 资源列表 */
    List<KpResourceVO> listResources(Long kpId);

    /** 下载资源（流式，返回文件名 + 磁盘资源句柄） */
    DownloadFile downloadResource(Long id);

    /** 删除资源 */
    void deleteResource(Long id);
}