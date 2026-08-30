package com.eduai.security.service.impl;

import com.eduai.common.BusinessException;
import com.eduai.security.entity.Message;
import com.eduai.security.repository.MessageRepository;
import com.eduai.security.service.MessageService;
import com.eduai.security.vo.MessagePageVO;
import com.eduai.security.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void send(Long userId, String type, String title, String content) {
        messageRepository.save(Message.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .build());
        log.info("站内消息发送: userId={} type={} title={}", userId, type, title);
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageVO list(Long userId, int page, int pageSize) {
        Page<Message> p = messageRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(pageSize, 1), 100)));
        List<MessageVO> list = p.getContent().stream().map(this::toVO).toList();
        return MessagePageVO.builder()
                .total(p.getTotalElements())
                .list(list)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long messageId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(404, "消息不存在"));
        if (!m.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该消息");
        }
        if (!Boolean.TRUE.equals(m.getReadFlag())) {
            m.setReadFlag(true);
            messageRepository.save(m);
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long messageId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(404, "消息不存在"));
        if (!m.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该消息");
        }
        messageRepository.delete(m);
        log.info("站内消息删除: userId={} messageId={}", userId, messageId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        messageRepository.markAllRead(userId);
    }

    private MessageVO toVO(Message m) {
        return MessageVO.builder()
                .id(m.getId())
                .type(m.getType())
                .title(m.getTitle())
                .content(m.getContent())
                .read(m.getReadFlag())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
