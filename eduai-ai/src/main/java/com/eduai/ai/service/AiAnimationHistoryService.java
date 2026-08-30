package com.eduai.ai.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 动图历史持久化服务 — 用 EntityManager 原生 SQL 操作 ai_animation_history 表。
 * <p>
 * eduai-ai 模块不依赖 eduai-system（避免循环依赖），故不建实体/Repository，
 * 沿用 {@code AIConfigInitializer} 的 {@link EntityManager} 原生 SQL 模式。
 * 历史卡片按登录用户隔离（user_id 由 Sa-Token 登录态注入，非前端传参）。
 */
@Slf4j
@Service
public class AiAnimationHistoryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存一条动图历史，返回含 id / coverUrl / createdAt 的记录对象。
     */
    @Transactional
    public Map<String, Object> save(String title, String grade, String subject,
                                    List<String> knowledgeTags, String schema, String coverUrl) {
        Long userId = StpUtil.getLoginIdAsLong();
        String tagsJson = writeTags(knowledgeTags);

        entityManager.createNativeQuery(
                "INSERT INTO ai_animation_history (user_id, title, grade, subject, knowledge_tags, schema_json, cover_url, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, userId)
                .setParameter(2, title)
                .setParameter(3, grade)
                .setParameter(4, subject)
                .setParameter(5, tagsJson)
                .setParameter(6, schema)
                .setParameter(7, coverUrl)
                .setParameter(8, LocalDateTime.now())
                .executeUpdate();

        Number id = (Number) entityManager
                .createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        return detail(id.longValue());
    }

    /**
     * 历史列表（分页，不含 schema，体积小），按 subject 可选过滤，倒序。
     * 返回 { list, total } 供前端 el-pagination 使用。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> history(int page, int pageSize, String subject) {
        Long userId = StpUtil.getLoginIdAsLong();
        StringBuilder where = new StringBuilder(" WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (subject != null && !subject.isBlank()) {
            where.append(" AND subject = ?");
            params.add(subject.trim());
        }

        // 总数
        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM ai_animation_history" + where);
        for (int i = 0; i < params.size(); i++) {
            countQuery.setParameter(i + 1, params.get(i));
        }
        long total = ((Number) countQuery.getSingleResult()).longValue();

        // 分页列表
        Query listQuery = entityManager.createNativeQuery(
                "SELECT id, title, grade, subject, knowledge_tags, cover_url, created_at " +
                "FROM ai_animation_history" + where +
                " ORDER BY created_at DESC, id DESC");
        for (int i = 0; i < params.size(); i++) {
            listQuery.setParameter(i + 1, params.get(i));
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        listQuery.setFirstResult((safePage - 1) * safeSize);
        listQuery.setMaxResults(safeSize);
        List<Object[]> rows = listQuery.getResultList();

        List<Map<String, Object>> list = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            list.add(toListItem(r));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    /**
     * 历史详情（含 schema + coverUrl），按 id + user_id 定位。
     */
    public Map<String, Object> detail(long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<?> rows = entityManager.createNativeQuery(
                "SELECT id, title, grade, subject, knowledge_tags, cover_url, schema_json, created_at " +
                "FROM ai_animation_history WHERE id = ? AND user_id = ?")
                .setParameter(1, id)
                .setParameter(2, userId)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        Object[] r = (Object[]) rows.get(0);
        Map<String, Object> item = toListItem(r);
        item.put("schema", r[6] == null ? null : r[6].toString());
        return item;
    }

    /**
     * 删除一条历史，返回是否命中（0 = 记录不存在或非本人）。
     */
    @Transactional
    public boolean delete(long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        int affected = entityManager.createNativeQuery(
                "DELETE FROM ai_animation_history WHERE id = ? AND user_id = ?")
                .setParameter(1, id)
                .setParameter(2, userId)
                .executeUpdate();
        return affected > 0;
    }

    /**
     * 把一条结果行映射为通用对象。列序约定：0=id 1=title 2=grade 3=subject 4=knowledge_tags
     * 5=cover_url 末列=created_at。
     * 列表查询（7 列）与详情查询（8 列，schema_json 在末列前）共用此约定。
     */
    private Map<String, Object> toListItem(Object[] r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ((Number) r[0]).longValue());
        m.put("title", r[1]);
        m.put("grade", r[2]);
        m.put("subject", r[3]);
        m.put("knowledgeTags", parseTags(r[4] == null ? null : r[4].toString()));
        m.put("coverUrl", r[5]);
        m.put("createdAt", formatTime(r[r.length - 1]));
        return m;
    }

    /** knowledge_tags 存 JSON 数组字符串，返回时解析为数组；解析失败则回退为原始字符串（前端兼容两者）。 */
    private Object parseTags(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return json;
        }
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? new ArrayList<String>() : tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** created_at 可能以 java.sql.Timestamp / LocalDateTime / String 返回，统一格式化为字符串。 */
    private Object formatTime(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().format(FMT);
        }
        if (raw instanceof LocalDateTime ldt) {
            return ldt.format(FMT);
        }
        return raw.toString();
    }
}
