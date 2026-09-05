package com.eduai.system.service;

import com.eduai.system.dto.DownloadFile;
import com.eduai.system.dto.PreviewFile;
import com.eduai.system.dto.ResourceChapterDTO;
import com.eduai.system.dto.ResourceSectionDTO;
import com.eduai.system.dto.ResourceTextbookDTO;
import com.eduai.system.entity.ResourceChapter;
import com.eduai.system.entity.ResourceSection;
import com.eduai.system.entity.ResourceTextbook;
import com.eduai.system.vo.ArchiveEntryVO;
import com.eduai.system.vo.ResourceFilePageVO;
import com.eduai.system.vo.ResourceFileVO;
import com.eduai.system.vo.ResourceReviewVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学习资源 Service（教材 → 章节 → 小节 → 资源文件）
 */
public interface ResourceService {

    // ==================== 教材 ====================
    List<ResourceTextbook> listTextbooks(String subject);

    ResourceTextbook createTextbook(ResourceTextbookDTO dto);

    ResourceTextbook updateTextbook(Long id, Map<String, Object> body);

    void reorderTextbooks(List<Long> orderedIds);

    void deleteTextbook(Long id);

    // ==================== 章节 ====================
    List<ResourceChapter> listChapters(Long textbookId);

    ResourceChapter createChapter(Long textbookId, ResourceChapterDTO dto);

    ResourceChapter updateChapter(Long id, Map<String, Object> body);

    void reorderChapters(List<Long> orderedIds);

    void deleteChapter(Long id);

    // ==================== 小节 ====================
    List<ResourceSection> listSections(Long chapterId);

    ResourceSection createSection(Long chapterId, ResourceSectionDTO dto);

    ResourceSection updateSection(Long id, Map<String, Object> body);

    void reorderSections(List<Long> orderedIds);

    void deleteSection(Long id);

    // ==================== 资源文件 ====================
    /** 查询某节点资源（含子级聚合），nodeType ∈ textbook/chapter/section；type/year 可选过滤 */
    List<ResourceFileVO> listResources(String nodeType, Long nodeId, String subject, String type, String year);

    /** 查询某节点资源（含子级聚合，分页）：total 为过滤后聚合全量条数，list 为切片 */
    ResourceFilePageVO listResourcesPage(String nodeType, Long nodeId, String subject, String type, String year,
                                         int page, int pageSize);

    /** 上传资源（多文件），previewPaths 为 JSON 字符串数组，与 files[] 按索引对齐 */
    List<ResourceFileVO> uploadResources(String nodeType, Long nodeId, String subject, String tag,
                                         String year, Integer price, Boolean shared,
                                         String previewPaths, List<MultipartFile> files);

    void deleteResource(Long id);

    /** 下载资源（流式，返回文件名 + 磁盘资源句柄，避免整文件载入内存） */
    DownloadFile downloadResource(Long id);

    /** 检查压缩包内部文件清单（仅 zip，上传者可用） */
    List<ArchiveEntryVO> inspectArchive(MultipartFile file);

    /** 预览元信息：{type, url}，无可预览内容时 {type:"none"} */
    Map<String, String> previewResource(Long id);

    /** 预览文件流（截断+水印，与原始文件隔离） */
    PreviewFile previewResourceFile(Long id);

    // ==================== 资源审核 ====================

    /** 待审核资源列表（管理员） */
    List<ResourceReviewVO> listPendingResources();

    /** 审核资源：通过（可改价）/ 驳回（必填理由） */
    void reviewResource(Long id, boolean approved, Integer price, String reason);
}
