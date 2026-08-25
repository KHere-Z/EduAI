package com.eduai.system.dto;

import org.springframework.core.io.Resource;

/**
 * 预览文件结果：原文件名 + Content-Type + 可流式读取的资源句柄。
 * <p>
 * 与 {@link DownloadFile} 区别：多一个 contentType，供 preview/file 端点按类型
 * 设置响应头（pdf → application/pdf，image → image/png）。
 *
 * @param fileName    原文件名
 * @param contentType 响应 Content-Type
 * @param resource    指向预览文件的磁盘资源（截断+水印，与原始文件隔离）
 */
public record PreviewFile(String fileName, String contentType, Resource resource) {
}
