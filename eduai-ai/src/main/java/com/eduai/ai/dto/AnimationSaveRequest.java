package com.eduai.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 动图历史保存请求 DTO。
 * <p>
 * 前端保存动图卡片时上传标题/年级/学科/知识点标签 + 完整几何 schema JSON 字符串。
 * knowledgeTags 前端传数组，后端序列化为 JSON 字符串存入 TEXT 列。
 */
@Data
public class AnimationSaveRequest {

    /** 题目概述（历史卡片标题） */
    private String title;

    /** 年级，如「初二」 */
    private String grade;

    /** 学科，如「数学」 */
    private String subject;

    /** 知识点标签数组（来自预置词典） */
    private List<String> knowledgeTags;

    /** 完整几何 schema JSON 字符串 */
    private String schema;

    /** 封面图（原题 base64 data URL，可选，后端落盘成图片文件后返回 coverUrl） */
    private String coverImage;
}
