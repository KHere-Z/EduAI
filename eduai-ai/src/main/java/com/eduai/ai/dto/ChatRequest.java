package com.eduai.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 聊天请求 DTO
 * <p>
 * messages 包含完整对话历史（前端维护），后端直接转发给 DeepSeek。
 * systemPrompt 前端可按场景传入不同提示词。
 */
@Data
public class ChatRequest {

    /** 对话消息列表 */
    private List<Message> messages;

    /** 系统提示词（选填，会作为 system role 注入） */
    private String systemPrompt;

    /** 图片 URL（单图，兼容旧前端） */
    private String imageUrl;

    /** 多图片 URL 列表（试卷分析上传多张） */
    private List<String> imageUrls;

    /** 覆盖模型（不传则用全局配置，如 doubao-seed-2-1-pro-260628） */
    private String model;

    /** 覆盖 API URL（不传则用全局配置） */
    private String apiUrl;

    /** 覆盖 API Key（不传则用全局配置） */
    private String apiKey;

    /**
     * 单条消息
     */
    @Data
    public static class Message {
        /** 角色：user / assistant / system */
        private String role;
        /** 消息内容 */
        private String content;
    }
}