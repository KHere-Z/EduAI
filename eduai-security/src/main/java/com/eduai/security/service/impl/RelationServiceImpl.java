package com.eduai.security.service.impl;

import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.entity.UserRelation;
import com.eduai.security.enums.RoleEnum;
import com.eduai.security.repository.UserRelationRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.RelationService;
import com.eduai.security.vo.RelationVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

/**
 * 用户关系服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationServiceImpl implements RelationService {

    private final UserRelationRepository relationRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public List<RelationVO> getMyRelations(Long myUid, String type) {
        User me = userRepository.findByUid(myUid).orElse(null);
        if (me == null) {
            log.warn("[RELATIONS] 用户不存在: myUid={}", myUid);
            return Collections.emptyList();
        }

        // 去重键：ordered "uidA-uidB"
        Set<String> seen = new LinkedHashSet<>();
        List<RelationVO> result = new ArrayList<>();

        // 1. user_relations 表（新系统）
        List<UserRelation> rels = new ArrayList<>();
        rels.addAll(relationRepository.findByFromUid(myUid));
        rels.addAll(relationRepository.findByToUid(myUid));
        for (UserRelation r : rels) {
            if ("accepted".equals(r.getStatus()) && matchesType(r, type)) {
                String key = orderedPairKey(r.getFromUid(), r.getToUid());
                if (seen.add(key)) {
                    result.add(toVO(r, myUid));
                }
            }
        }

        // 2. teacher_student 表（旧系统，管理员端数据）——仅在无类型过滤或过滤 teacher_student 时合并
        if (type == null || type.isBlank() || "teacher_student".equals(type)) {
            mergeFromTeacherStudent(me, myUid, seen, result);
        }

        return result;
    }

    /** 从 teacher_student 表读取旧关系并合并 */
    private void mergeFromTeacherStudent(User me, Long myUid,
                                          Set<String> seen, List<RelationVO> result) {
        try {
            if (me.getRoleType() != null && me.getRoleType() == 3) {
                // 我是老师：查我教的全部学生
                log.info("[RELATIONS] 查询 teacher_student: teacher_id={}", me.getId());
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT ts.id, s.user_id as student_user_id, s.name as student_name, " +
                        "u.uid as student_uid, u.nickname as student_nickname, u.subjects as student_subjects, " +
                        "ts.created_at " +
                        "FROM teacher_student ts " +
                        "JOIN students s ON ts.student_id = s.id " +
                        "JOIN users u ON s.user_id = u.id " +
                        "WHERE ts.teacher_id = ?", me.getId());
                log.info("[RELATIONS] teacher_student 查询结果: teacher_id={}, 行数={}, 详情={}",
                        me.getId(), rows.size(),
                        rows.stream().map(r -> "uid=" + r.get("student_uid")).toList());
                for (Map<String, Object> row : rows) {
                    Long otherUid = toLong(row.get("student_uid"));
                    if (otherUid == null || otherUid.equals(myUid)) continue;
                    String key = orderedPairKey(myUid, otherUid);
                    if (seen.add(key)) {
                        result.add(buildLegacyVO(row, otherUid, "student", myUid));
                    }
                }
            } else if (me.getRoleType() != null && me.getRoleType() == 4) {
                // 我是学生：查教我的全部老师
                Long studentRecordId = jdbcTemplate.queryForObject(
                        "SELECT id FROM students WHERE user_id = ?", Long.class, me.getId());
                if (studentRecordId != null) {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT ts.id, u.uid as teacher_uid, u.real_name as teacher_name, " +
                            "u.nickname as teacher_nickname, u.subjects as teacher_subjects, ts.created_at " +
                            "FROM teacher_student ts " +
                            "JOIN users u ON ts.teacher_id = u.id " +
                            "WHERE ts.student_id = ?", studentRecordId);
                    for (Map<String, Object> row : rows) {
                        Long otherUid = toLong(row.get("teacher_uid"));
                        if (otherUid == null || otherUid.equals(myUid)) continue;
                        String key = orderedPairKey(myUid, otherUid);
                        if (seen.add(key)) {
                            result.add(buildLegacyVO(row, otherUid, "teacher", myUid));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询teacher_student失败(可能表不存在): {}", e.getMessage());
        }
    }

    /** 构建旧关系 RelationVO */
    private RelationVO buildLegacyVO(Map<String, Object> row, Long otherUid,
                                      String role, Long myUid) {
        String name = role.equals("student")
                ? (String) row.get("student_name")
                : (String) row.get("teacher_name");
        String nickname = role.equals("student")
                ? (String) row.get("student_nickname")
                : (String) row.get("teacher_nickname");
        String subjectsJson = role.equals("student")
                ? (String) row.get("student_subjects")
                : (String) row.get("teacher_subjects");
        List<String> subjects = parseJsonArray(subjectsJson);
        java.time.LocalDateTime createdAt = toLocalDateTime(row.get("created_at"));

        return RelationVO.builder()
                .id(toLong(row.get("id")))
                .uid(AuthServiceImpl.formatUid(otherUid))
                .name(name)
                .nickname(nickname)
                .subjects(subjects)
                .role(role)
                .type("teacher_student")
                .status("accepted")
                .direction("received") // 旧数据方向未知，统一标 received
                .createdAt(createdAt)
                .build();
    }

    private static String orderedPairKey(Long a, Long b) {
        return a < b ? a + "-" + b : b + "-" + a;
    }

    private static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private static java.time.LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof java.time.LocalDateTime dt) return dt;
        if (val instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    @Override
    public List<RelationVO> getIncomingRelations(Long myUid, String type) {
        return relationRepository.findByToUidAndStatus(myUid, "pending")
                .stream()
                .filter(r -> matchesType(r, type))
                .map(r -> toVO(r, myUid))
                .toList();
    }

    @Override
    @Transactional
    public RelationVO sendRequest(Long fromUid, Long toUid, String type) {
        if (fromUid.equals(toUid)) {
            throw new BusinessException(400, "不能向自己发送请求");
        }
        String relType = normalizeType(type);

        // 检查目标用户是否存在
        User target = userRepository.findByUid(toUid)
                .orElseThrow(() -> new BusinessException(400, "目标用户不存在"));

        // 检查发起方是否存在
        User sender = userRepository.findByUid(fromUid)
                .orElseThrow(() -> new BusinessException(400, "用户不存在"));

        // 按关系类型校验双方角色
        validateRolesForType(sender, target, relType);

        // 检查是否已有 pending 或 accepted 关系（双向）
        Optional<UserRelation> existing = relationRepository.findByFromUidAndToUid(fromUid, toUid);
        if (existing.isPresent()) {
            String status = existing.get().getStatus();
            if ("accepted".equals(status)) {
                throw new BusinessException(400, "已是关联用户");
            }
            if ("pending".equals(status)) {
                throw new BusinessException(400, "已发送过请求，等待对方确认");
            }
        }
        // 反向检查（对方是否已向我发过请求）
        Optional<UserRelation> reverse = relationRepository.findByFromUidAndToUid(toUid, fromUid);
        if (reverse.isPresent()) {
            String status = reverse.get().getStatus();
            if ("accepted".equals(status)) {
                throw new BusinessException(400, "已是关联用户");
            }
            if ("pending".equals(status)) {
                throw new BusinessException(400, "对方已向你发送请求，请先处理");
            }
        }

        UserRelation relation = UserRelation.builder()
                .fromUid(fromUid)
                .toUid(toUid)
                .type(relType)
                .status("pending")
                .build();
        relationRepository.save(relation);

        log.info("关联请求已发送: from={} to={} type={}", fromUid, toUid, relType);
        return toVO(relation, fromUid);
    }

    @Override
    @Transactional
    public void approve(Long relationId, Long myUid) {
        UserRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException(400, "请求不存在或已过期"));

        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(400, "请求已被处理");
        }
        if (!relation.getToUid().equals(myUid)) {
            throw new BusinessException(403, "无权操作此请求");
        }

        relation.setStatus("accepted");
        relationRepository.save(relation);

        // 同步到 teacher_student 表（管理员端兼容；同事关系不涉及排课/错题，无需同步）
        if (!"colleague".equals(relation.getType())) {
            syncToTeacherStudent(relation);
        }

        log.info("关联请求已同意: id={} type={}", relationId, relation.getType());
    }

    @Override
    @Transactional
    public void reject(Long relationId, Long myUid) {
        UserRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException(400, "请求不存在或已过期"));

        if (!"pending".equals(relation.getStatus())) {
            throw new BusinessException(400, "请求已被处理");
        }
        if (!relation.getToUid().equals(myUid)) {
            throw new BusinessException(403, "无权操作此请求");
        }

        relation.setStatus("rejected");
        relationRepository.save(relation);
        log.info("关联请求已拒绝: id={}", relationId);
    }

    @Override
    @Transactional
    public void removeRelation(Long relationId, Long myUid) {
        // 先查 user_relations（新系统）
        UserRelation relation = relationRepository.findById(relationId).orElse(null);
        if (relation != null) {
            if (!"accepted".equals(relation.getStatus())) {
                throw new BusinessException(400, "只能移除已接受的关联");
            }
            if (!relation.getFromUid().equals(myUid) && !relation.getToUid().equals(myUid)) {
                throw new BusinessException(403, "无权操作");
            }
            unsyncFromTeacherStudent(relation);
            relationRepository.delete(relation);
            log.info("关联已移除(user_relations): id={}", relationId);
            return;
        }

        // 再查 teacher_student（旧系统/管理员创建的关系）
        removeFromTeacherStudent(relationId, myUid);
    }

    /** 从 teacher_student 表直接删除关系（旧系统记录） */
    private void removeFromTeacherStudent(Long tsId, Long myUid) {
        User me = userRepository.findByUid(myUid).orElse(null);
        if (me == null) throw new BusinessException(400, "用户不存在");

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(
                    "SELECT ts.teacher_id, s.user_id as student_user_id " +
                    "FROM teacher_student ts JOIN students s ON ts.student_id = s.id " +
                    "WHERE ts.id = ?", tsId);
        } catch (Exception e) {
            throw new BusinessException(400, "关联不存在");
        }

        Long teacherUserId = toLong(row.get("teacher_id"));
        Long studentUserId = toLong(row.get("student_user_id"));

        // 权限检查：操作者必须是教师或学生本人
        if (!me.getId().equals(teacherUserId) && !me.getId().equals(studentUserId)) {
            throw new BusinessException(403, "无权操作");
        }

        jdbcTemplate.update("DELETE FROM teacher_student WHERE id = ?", tsId);
        log.info("关联已移除(teacher_student): id={}", tsId);
    }

    /** 移除关联时同步删除 teacher_student */
    private void unsyncFromTeacherStudent(UserRelation rel) {
        User from = userRepository.findByUid(rel.getFromUid()).orElse(null);
        User to = userRepository.findByUid(rel.getToUid()).orElse(null);
        if (from == null || to == null) return;

        int fromRole = from.getRoleType() != null ? from.getRoleType() : 0;
        int toRole = to.getRoleType() != null ? to.getRoleType() : 0;
        if ((fromRole != 3 || toRole != 4) && (fromRole != 4 || toRole != 3)) return;

        Long teacherUserId = fromRole == 3 ? from.getId() : to.getId();
        Long studentUserId = fromRole == 4 ? from.getId() : to.getId();

        try {
            Long studentRecordId = jdbcTemplate.queryForObject(
                    "SELECT id FROM students WHERE user_id = ?", Long.class, studentUserId);
            if (studentRecordId != null) {
                jdbcTemplate.update(
                        "DELETE FROM teacher_student WHERE teacher_id = ? AND student_id = ?",
                        teacherUserId, studentRecordId);
                log.info("已移除 teacher_student: teacherId={} studentId={}", teacherUserId, studentRecordId);
            }
        } catch (Exception ignored) {
            // students 表中不存在则无需清理
        }
    }

    // ==================== teacher_student 同步 ====================

    /**
     * 同意请求后，如果是教师↔学生关系，同步写入 teacher_student 表。
     * <p>
     * 若学生自注册未在 students 表中，自动创建 students 记录。
     * 这保证了全系统（排课/错题/试卷/管理员端）对新旧关系入口兼容。
     */
    private void syncToTeacherStudent(UserRelation rel) {
        User from = userRepository.findByUid(rel.getFromUid()).orElse(null);
        User to = userRepository.findByUid(rel.getToUid()).orElse(null);
        if (from == null || to == null) return;

        int fromRole = from.getRoleType() != null ? from.getRoleType() : 0;
        int toRole = to.getRoleType() != null ? to.getRoleType() : 0;

        // 仅处理教师(3)↔学生(4)
        Long teacherUserId, studentUserId;
        User studentUser;
        if (fromRole == 3 && toRole == 4) {
            teacherUserId = from.getId();
            studentUserId = to.getId();
            studentUser = to;
        } else if (fromRole == 4 && toRole == 3) {
            teacherUserId = to.getId();
            studentUserId = from.getId();
            studentUser = from;
        } else {
            return;
        }

        // 确保 students 和 teachers 表有对应记录（自注册的可能没有）
        ensureTeacherRecord(teacherUserId);
        Long studentRecordId = ensureStudentRecord(studentUser);

        // 检查 teacher_student 是否已存在
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teacher_student WHERE teacher_id = ? AND student_id = ?",
                Integer.class, teacherUserId, studentRecordId);
        if (count != null && count > 0) {
            log.info("teacher_student 已存在(teacher={} student={}), 跳过", teacherUserId, studentRecordId);
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO teacher_student (teacher_id, student_id, hours_left, created_at) VALUES (?, ?, 0, NOW())",
                teacherUserId, studentRecordId);
        log.info("已同步 teacher_student: teacherId={} studentId={}", teacherUserId, studentRecordId);
    }

    /** 确保 teachers 表存在记录（自注册教师可能没有） */
    private void ensureTeacherRecord(Long teacherUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teachers WHERE user_id = ?", Integer.class, teacherUserId);
        if (count != null && count > 0) return;

        jdbcTemplate.update(
                "INSERT INTO teachers (user_id, subject_ids, created_at) VALUES (?, '', NOW())",
                teacherUserId);
        log.info("自动创建teachers记录: userId={}", teacherUserId);
    }

    /** 确保 students 表存在记录，不存在则自动创建，返回 student.id */
    private Long ensureStudentRecord(User studentUser) {
        // 先查是否已有
        Long recordId;
        try {
            recordId = jdbcTemplate.queryForObject(
                    "SELECT id FROM students WHERE user_id = ?", Long.class, studentUser.getId());
        } catch (Exception e) {
            recordId = null;
        }
        if (recordId != null) {
            // 已存在则同步学科（以 User.subjects 为准）
            if (studentUser.getSubjects() != null) {
                jdbcTemplate.update(
                        "UPDATE students SET subjects = ? WHERE id = ?",
                        studentUser.getSubjects(), recordId);
            }
            return recordId;
        }

        // 不存在 → 自动创建
        String name = studentUser.getRealName();
        if (name == null || name.isBlank()) name = studentUser.getNickname();
        if (name == null || name.isBlank()) name = "新同学";

        jdbcTemplate.update(
                "INSERT INTO students (name, user_id, subjects, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                name, studentUser.getId(), studentUser.getSubjects());
        recordId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("自动创建students记录: id={} userId={} name={}", recordId, studentUser.getId(), name);
        return recordId;
    }

    // ==================== 内部辅助 ====================

    private RelationVO toVO(UserRelation rel, Long myUid) {
        Long otherUid = rel.getFromUid().equals(myUid) ? rel.getToUid() : rel.getFromUid();
        String direction = rel.getFromUid().equals(myUid) ? "sent" : "received";

        User other = userRepository.findByUid(otherUid).orElse(null);

        String name = other != null ? other.getRealName() : null;
        String nickname = other != null ? other.getNickname() : null;
        List<String> subjects = other != null ? parseJsonArray(other.getSubjects()) : Collections.emptyList();
        String role = other != null ? roleLabel(other.getRoleType()) : null;

        return RelationVO.builder()
                .id(rel.getId())
                .uid(AuthServiceImpl.formatUid(otherUid))
                .name(name)
                .nickname(nickname)
                .subjects(subjects)
                .role(role)
                .type(rel.getType() == null ? "teacher_student" : rel.getType())
                .status(rel.getStatus())
                .direction(direction)
                .createdAt(rel.getCreatedAt())
                .build();
    }

    private String roleLabel(Integer roleType) {
        if (roleType == null) return null;
        try {
            return RoleEnum.fromDbValue(roleType).getCode();
        } catch (Exception e) {
            return String.valueOf(roleType);
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 归一化关系类型：null/blank → teacher_student，非法值抛错 */
    private String normalizeType(String type) {
        if (type == null || type.isBlank()) return "teacher_student";
        if (!"teacher_student".equals(type) && !"colleague".equals(type)) {
            throw new BusinessException(400, "无效的关系类型: " + type);
        }
        return type;
    }

    /** 关系是否匹配类型过滤（null 视为 teacher_student，兼容旧数据） */
    private boolean matchesType(UserRelation r, String filter) {
        if (filter == null || filter.isBlank()) return true;
        String rType = r.getType() == null || r.getType().isBlank() ? "teacher_student" : r.getType();
        return filter.equals(rType);
    }

    /** 按关系类型校验双方角色 */
    private void validateRolesForType(User sender, User target, String relType) {
        int sr = sender.getRoleType() != null ? sender.getRoleType() : 0;
        int tr = target.getRoleType() != null ? target.getRoleType() : 0;

        if ("colleague".equals(relType)) {
            // 同事：双方都必须是老师(3)
            if (sr != 3 || tr != 3) {
                throw new BusinessException(400, "同事关系仅限教师之间建立");
            }
            return;
        }
        // 师生：仅允许 teacher↔student
        if ((sr == 3 && tr == 4) || (sr == 4 && tr == 3)) {
            return;
        }
        throw new BusinessException(400, "同角色之间不能建立关联");
    }
}
