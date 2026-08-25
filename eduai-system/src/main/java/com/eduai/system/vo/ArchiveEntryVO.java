package com.eduai.system.vo;

/**
 * 压缩包内部文件清单项（POST /resource/archive/inspect 返回）
 *
 * @param path        包内路径（如 课件/第1章.pdf）
 * @param name        文件名
 * @param size        字节数
 * @param previewable 是否可生成预览
 */
public record ArchiveEntryVO(String path, String name, long size, boolean previewable) {
}
