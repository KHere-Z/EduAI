package com.eduai.system.service;

import com.eduai.system.dto.ClientLogEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 前端日志内存环形缓冲（ring buffer）。
 * <p>
 * 只保留最近 {@link #MAX_ENTRIES} 条，超出自动淘汰最旧（FIFO）。
 * 纯 JVM 内存，不落盘、不进库、不接 Redis —— 天然满足「不占磁盘空间」，
 * 代价是进程重启即清空、只能查最近一段（近期排障用途，不做长期留存）。
 */
@Component
public class ClientLogBuffer {

    private static final int MAX_ENTRIES = 2000;

    private final Deque<ClientLogEntry> deque = new ConcurrentLinkedDeque<>();
    private final AtomicInteger count = new AtomicInteger(0);

    /** 入队一条日志，容量满时淘汰最旧条目。 */
    public void append(ClientLogEntry entry) {
        if (entry == null) {
            return;
        }
        deque.addLast(entry);
        if (count.incrementAndGet() > MAX_ENTRIES) {
            deque.pollFirst();
            count.decrementAndGet();
        }
    }

    /**
     * 按 level/uid/role 过滤后倒序（最新在前）分页查询。
     * 返回 { total, list }。
     */
    public Map<String, Object> query(String level, String uid, Integer role, int page, int size) {
        List<ClientLogEntry> filtered = deque.stream()
                .filter(e -> level == null || level.isBlank() || level.equals(e.getLevel()))
                .filter(e -> uid == null || uid.isBlank() || uid.equals(e.getUid()))
                .filter(e -> role == null || role.equals(e.getRole()))
                .sorted((a, b) -> Long.compare(timeOf(b), timeOf(a)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.max(page - 1, 0) * size;
        List<ClientLogEntry> list = from >= total
                ? new ArrayList<>()
                : filtered.subList(from, Math.min(from + size, total));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", list);
        return result;
    }

    private long timeOf(ClientLogEntry e) {
        return e.getTime() == null ? 0L : e.getTime();
    }
}
