package com.eduai.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

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
 *   <li>{@code metric:active:daily:{yyyy-MM-dd}} — 日活分桶（保留 31 天，供 DAU/MAU 用）</li>
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

    /** 按天分桶的活跃埋点前缀，完整 key 形如 {@code metric:active:daily:2026-09-19} */
    private static final String KEY_ACTIVE_DAILY_PREFIX = "metric:active:daily:";

    /** 时间窗（毫秒）— 5 分钟 */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    /**
     * 日活桶的保留时长。取 31 天而非 30：
     * <p>
     * 月活在 D 日要读 D-29..D 共 30 个桶，最老的那个（X = D-29）TTL 是「X 当天最后一次写入 + 保留时长」，
     * 而当天最后一次写入最早可能就在 X 00:00 —— 故取 30 天时该桶最早在 D+1 00:00 过期，
     * 与查询窗口的边界**严丝合缝、保证余量为 0**（实际余量 0~24h，取决于当天最后写入的时刻）。
     * 多留 1 天让余量恒 ≥24h，用一天的 Redis 内存换掉一个边界条件。
     */
    private static final Duration DAILY_RETENTION = Duration.ofDays(31);

    private final StringRedisTemplate redisTemplate;

    // ==================== 打点 ====================

    /** 在线心跳：刷新 userId 的最后活跃时间戳 */
    public void heartbeat(Long userId) {
        if (userId == null) return;
        redisTemplate.opsForZSet().add(KEY_ONLINE, String.valueOf(userId), System.currentTimeMillis());
    }

    /**
     * 按自然日分桶的活跃打点 —— DAU / MAU 的精确口径（「访问日活」，非「登录日活」）。
     * <p>
     * 与 {@link #heartbeat} 的区别：{@code metric:online} 只保留 5 分钟
     * （{@link #windowCount} 会主动删掉窗口外成员），只能回答「当前在线」；
     * 本方法按天建桶、桶内 member 去重（ZSET 天然去重）、桶整体靠 TTL 保留，
     * 因此可以回溯算任意一天的 DAU 和近 30 天的 MAU。
     * <p>
     * 读取方式（**尚未接线**，等埋点攒够 30 天再切到本口径）：
     * <ul>
     *   <li>DAU = {@code ZCARD metric:active:daily:{今天}}</li>
     *   <li>MAU = {@code ZUNIONSTORE dest 30 metric:active:daily:{D-29} ... {D}} + {@code ZCARD dest}</li>
     * </ul>
     * <p>
     * ⚠️ 本指标只在 Redis 里，不落库：Redis 重启 / 未开持久化即丢。这也正是日活当前
     * 先用 {@code users.last_login}（在 MySQL 里）兜底的原因，见 {@code AdminServiceImpl#getStats}。
     */
    public void markDailyActive(Long userId) {
        if (userId == null) return;
        String key = KEY_ACTIVE_DAILY_PREFIX + LocalDate.now();
        redisTemplate.opsForZSet().add(key, String.valueOf(userId), System.currentTimeMillis());
        // score 用不上（桶内只按 member 去重、过期交给 TTL），保留时间戳是为了排查时能看出当天最后活跃时刻
        // 每次写入都续期：TTL 变成「最后一次活跃起算 31 天」，活跃中的桶不会中途消失
        redisTemplate.expire(key, DAILY_RETENTION);
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
