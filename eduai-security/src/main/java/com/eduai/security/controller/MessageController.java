package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.service.MessageService;
import com.eduai.security.vo.MessagePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内消息接口（老师端消息中心）
 */
@RestController
@RequestMapping("/api/v1/user/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 消息列表（分页，时间倒序） */
    @GetMapping
    public Result<MessagePageVO> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(messageService.list(StpUtil.getLoginIdAsLong(), page, pageSize));
    }

    /** 未读计数（侧边栏角标） */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", messageService.unreadCount(StpUtil.getLoginIdAsLong())));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 删除单条消息（仅本人） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }
}
