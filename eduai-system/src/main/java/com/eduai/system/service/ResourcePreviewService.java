package com.eduai.system.service;

import com.eduai.system.dto.PreviewFile;
import com.eduai.system.entity.ResourceFile;
import com.eduai.system.vo.ArchiveEntryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资源「30% 试看预览」服务。
 * <p>
 * 预览文件为独立生成（截断 + 水印），存 uploads/previews/，与原始文件 uploads/resource/
 * 路径不重叠，杜绝「改 URL 拿原文件」。预览能力由文件扩展名 + 是否 zip 实时判定。
 */
public interface ResourcePreviewService {

    /** 检查压缩包内部文件清单（仅 zip，返回内部文件列表） */
    List<ArchiveEntryVO> inspectArchive(MultipartFile file);

    /** 判定预览类型：pdf | image | none（office 转 pdf，故 office 返回 pdf） */
    String resolvePreviewType(ResourceFile resource);

    /** 获取预览文件流（首次生成落盘缓存，重复请求直接返回缓存） */
    PreviewFile getPreviewFile(ResourceFile resource);
}
