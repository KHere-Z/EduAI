package com.eduai.system.dto;

import org.springframework.core.io.Resource;

/**
 * 文件下载结果：原文件名 + 可流式读取的资源句柄。
 * <p>
 * 替代旧的 {@code Object[]{fileName, byte[]}}（{@code Files.readAllBytes} 会把整个文件
 * 一次性载入堆内存，高并发付费资源下载时易触发 OOM）。{@link Resource} 由
 * {@code ResponseEntity<Resource>} 流式写出，Spring 按块读写，内存占用与文件大小无关。
 *
 * @param fileName 原始文件名（用于 Content-Disposition）
 * @param resource 指向磁盘文件、延迟读取的资源
 */
public record DownloadFile(String fileName, Resource resource) {
}
