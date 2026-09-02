package com.eduai.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 实时埋点统计 Service — Redis 方案（零建表、零侵入）
 * <p>
 * 时间窗指标（在线人数 / 三个 AI 功能活跃数）用 ZSET 存储：
 * score = 毫秒时间戳，member = userId，天然按 userId 去重、score 覆盖式更新。
 * 累计指标（资源下载量）用 String INCR 单调递增。
 * <p>
 * Key 约定：
 * <ul>
 *   <li>{@code metric:online} — 在线心跳</li>
 *   <li>{@code metric:active:{metric}} — 各功能活跃（examAnalysis / aiAnimation / wrongAnalysis）</li>
 *   <li>{@code metric:download:total} — 下载累计</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private static final String KEY_ONLINE = "metric:online";
    private static final String KEY_ACTIVE_PREFIX = "metric:active:";
    private static final String KEY_DOWNLOAD_TOTAL = "metric:download:total";

    /** 时间窗（毫秒）— 5 分钟 */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final StringRedisTemplate redisTemplate;

    // ==================== 打点 ====================

    /** 在线心跳：刷新 userId 的最后活跃时间戳 */
    public void heartbeat(Long userId) {
        if (userId == null) return;
        redisTemplate.opsForZSet().add(KEY_ONLINE, String.valueOf(userId), System.currentTimeMillis());
    }

    /** 功能活跃打点：metric ∈ examAnalysis / aiAnimation / wrongAnalysis */
    public void markActive(String metric, Long userId) {
        if (userId == null || metric == null || metric.isBlank()) return;
        redisTemplate.opsForZSet().add(KEY_ACTIVE_PREFIX + metric, String.valueOf(userId), System.currentTimeMillis());
    }

    /** 下载累计 +1（单调递增） */
    public void incrDownload() {
        redisTemplate.opsForValue().increment(KEY_DOWNLOAD_TOTAL);
    }

    // ==================== 统计 ====================

    /** 当前在线人数（最近 5 分钟去重 userId） */
    public long onlineCount() {
        return windowCount(KEY_ONLINE);
    }

    /** 指定功能活跃数（最近 5 分钟去重 userId） */
    public long activeCount(String metric) {
        return windowCount(KEY_ACTIVE_PREFIX + metric);
    }

    /** 资源累计下载量（无记录返 0） */
    public long downloadTotal() {
        String v = redisTemplate.opsForValue().get(KEY_DOWNLOAD_TOTAL);
        return v == null ? 0 : Long.parseLong(v);
    }

    /** 计算时间窗内去重成员数，并顺手清理窗口外的过期成员 */
    private long windowCount(String key) {
        long now = System.currentTimeMillis();
        long min = now - WINDOW_MS;
        // 清理 score <= min 的过期成员（时间戳恒为正，用 0 作下界即可）
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, min);
        Long count = redisTemplate.opsForZSet().count(key, min, now);
        return count == null ? 0 : count;
    }
}
