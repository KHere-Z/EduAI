package com.eduai.security.service;

import com.eduai.security.vo.MessagePageVO;

/**
 * 站内消息服务
 */
public interface MessageService {

    /** 发送一条消息 */
    void send(Long userId, String type, String title, String content);

    /** 分页查询消息（时间倒序） */
    MessagePageVO list(Long userId, int page, int pageSize);

    /** 未读数量 */
    long unreadCount(Long userId);

    /** 标记单条已读 */
    void markRead(Long userId, Long messageId);

    /** 删除单条消息（仅本人） */
    void delete(Long userId, Long messageId);

    /** 全部标记已读 */
    void markAllRead(Long userId);
}
