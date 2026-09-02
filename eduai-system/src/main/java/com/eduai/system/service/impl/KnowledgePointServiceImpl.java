package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.entity.UserRelation;
import com.eduai.security.repository.UserRelationRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.impl.AuthServiceImpl;
import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.entity.KnowledgePoint;
import com.eduai.system.repository.KnowledgePointRepository;
import com.eduai.system.service.KnowledgePointService;
import com.eduai.system.vo.KnowledgePointPageVO;
import com.eduai.system.vo.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识点 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointRepository knowledgePointRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository relationRepository;

    /** 校验当前用户是否为教师（roleType=3） */
    private void checkTeacher() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(403, "仅教师可访问");
        }
    }

    /** 校验当前用户是否为教师或管理员（roleType=3 或 1） */
    private void checkTeacherOrAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3 && user.getRoleType() != 1) {
            throw new BusinessException(403, "仅教师或管理员可访问");
        }
    }

    /** 获取当前用户ID */
    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 校验已登录（任意角色） */
    private void checkAuthenticated() {
        StpUtil.checkLogin();
    }

    /** 当前登录用户上下文 */
    private record CurrentUser(Long userId, Long uid, int roleType) {}

    /** 获取当前登录用户（含 uid，老用户自动补 uid） */
    private CurrentUser currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        Long uid = user.getUid();
        if (uid == null) {
            uid = AuthServiceImpl.generateUid();
            user.setUid(uid);
            userRepository.save(user);
        }
        int roleType = user.getRoleType() != null ? user.getRoleType() : 0;
        return new CurrentUser(userId, uid, roleType);
    }

    /** 当前用户可见的老师 uid 集合（不含历史共享数据） */
    private Set<Long> visibleTeacherUids(CurrentUser ctx) {
        if (ctx.roleType == 3) {
            // 老师：自己 + 所有已接受同事
            Set<Long> uids = new HashSet<>();
            uids.add(ctx.uid);
            uids.addAll(colleagueUids(ctx.uid));
            return uids;
        }
        if (ctx.roleType == 4) {
            // 学生：已接受师生关系的老师
            return teacherUidsOfStudent(ctx);
        }
        return Collections.emptySet();
    }

    /** 已接受同事的 uid 集合 */
    private Set<Long> colleagueUids(Long myUid) {
        Set<Long> uids = new HashSet<>();
        for (UserRelation r : relationRepository.findByFromUid(myUid)) {
            if ("accepted".equals(r.getStatus()) && "colleague".equals(r.getType())) {
                uids.add(r.getToUid());
            }
        }
        for (UserRelation r : relationRepository.findByToUid(myUid)) {
            if ("accepted".equals(r.getStatus()) && "colleague".equals(r.getType())) {
                uids.add(r.getFromUid());
            }
        }
        return uids;
    }

    /** 学生已接受师生关系的老师 uid 集合（含 User.teacherUid 兜底） */
    private Set<Long> teacherUidsOfStudent(CurrentUser ctx) {
        Set<Long> uids = new HashSet<>();
        List<UserRelation> rels = new ArrayList<>();
        rels.addAll(relationRepository.findByFromUid(ctx.uid));
        rels.addAll(relationRepository.findByToUid(ctx.uid));
        for (UserRelation r : rels) {
            if (!"accepted".equals(r.getStatus())) continue;
            String type = r.getType() == null ? "teacher_student" : r.getType();
            if (!"teacher_student".equals(type)) continue;
            Long otherUid = r.getFromUid().equals(ctx.uid) ? r.getToUid() : r.getFromUid();
            User other = userRepository.findByUid(otherUid).orElse(null);
            if (other != null && other.getRoleType() != null && other.getRoleType() == 3) {
                uids.add(otherUid);
            }
        }
        // 兜底：注册时填写的老师 UID
        User me = userRepository.findByUid(ctx.uid).orElse(null);
        if (me != null && me.getTeacherUid() != null) {
            uids.add(me.getTeacherUid());
        }
        return uids;
    }

    /** 知识点可见性 Specification */
    private Specification<KnowledgePoint> visibilitySpec(CurrentUser ctx) {
        return (root, query, cb) -> {
            if (ctx.roleType == 1) {
                return cb.conjunction(); // 管理员看全部
            }
            boolean includeLegacy = ctx.roleType == 3; // 老师保留历史共享（teacher_uid IS NULL）可见
            Set<Long> visible = visibleTeacherUids(ctx);
            List<Predicate> ps = new ArrayList<>();
            if (includeLegacy) {
                ps.add(cb.isNull(root.get("teacherUid")));
            }
            if (!visible.isEmpty()) {
                ps.add(root.get("teacherUid").in(visible));
            }
            if (ps.isEmpty()) {
                return cb.disjunction(); // 无任何可见 → 返回空
            }
            return cb.or(ps.toArray(new Predicate[0]));
        };
    }

    /** 校验当前用户对知识点有写权限（owner 本人或管理员） */
    private void checkKpOwner(KnowledgePoint kp) {
        CurrentUser ctx = currentUser();
        if (ctx.roleType == 1) return;
        if (ctx.roleType == 3 && kp.getTeacherUid() != null && kp.getTeacherUid().equals(ctx.uid)) return;
        throw new BusinessException(403, "无权操作他人知识点");
    }

    @Override
    @Transactional
    public KnowledgePointPageVO list(int page, int pageSize, String subject, String gradeLevel) {
        checkAuthenticated();
        CurrentUser ctx = currentUser();

        // 学生不显式传 gradeLevel 时，不做年级过滤（按绑定老师看全部知识点，跨年级辅导常见）；
        // 需要按年级查询时由前端走 listByGrades（grades 参数）显式传入。
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder", "id"));

        // 基础过滤：学科 + 年级前缀（学生 grade="初一" 命中 grade_level="初一·上学期"/"初一·下学期"）
        final String finalGradeLevel = gradeLevel;
        Specification<KnowledgePoint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subject"), subject));
            if (finalGradeLevel != null && !finalGradeLevel.isBlank() && !"all".equals(finalGradeLevel)) {
                predicates.add(cb.like(root.get("gradeLevel"), finalGradeLevel + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 叠加可见性过滤
        Page<KnowledgePoint> kpPage = knowledgePointRepository.findAll(spec.and(visibilitySpec(ctx)), pageable);

        List<KnowledgePointVO> list = kpPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return KnowledgePointPageVO.builder()
                .list(list)
                .total(kpPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    public KnowledgePointPageVO listByGrades(int page, int pageSize, String subject, String grades) {
        checkAuthenticated();
        CurrentUser ctx = currentUser();

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder", "id"));

        List<String> gradeList = (grades != null && !grades.isBlank())
                ? Arrays.stream(grades.split(",")).map(String::trim).filter(g -> !g.isEmpty()).collect(Collectors.toList())
                : List.of();

        // 基础过滤：学科 + 多年级前缀匹配
        Specification<KnowledgePoint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subject"), subject));
            if (!gradeList.isEmpty()) {
                Predicate[] gradePreds = gradeList.stream()
                        .map(g -> cb.like(root.get("gradeLevel"), g + "%"))
                        .toArray(Predicate[]::new);
                predicates.add(cb.or(gradePreds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 叠加可见性过滤
        Page<KnowledgePoint> kpPage = knowledgePointRepository.findAll(spec.and(visibilitySpec(ctx)), pageable);

        List<KnowledgePointVO> list = kpPage.getContent().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return KnowledgePointPageVO.builder()
                .list(list)
                .total(kpPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    public KnowledgePointVO create(KnowledgePointDTO dto) {
        checkTeacherOrAdmin();
        CurrentUser ctx = currentUser();

        // 去重检查（owner 维度，允许不同老师各自建同名知识点）
        if (knowledgePointRepository.existsBySubjectAndNameAndTeacherUid(dto.getSubject(), dto.getName(), ctx.uid)) {
            throw new BusinessException("该学科下已存在同名知识点");
        }

        KnowledgePoint kp = KnowledgePoint.builder()
                .subject(dto.getSubject())
                .name(dto.getName())
                .teacherUid(ctx.uid)
                .gradeLevel(dto.getGradeLevel())
                .description(dto.getDescription())
                .parentId(dto.getParentId())
                .sortOrder(dto.getSortOrder())
                .build();

        kp = knowledgePointRepository.save(kp);
        log.info("教师{} 新增知识点: id={}, name={}, subject={}", getCurrentUserId(), kp.getId(), kp.getName(), kp.getSubject());

        return toVO(kp);
    }

    @Override
    @Transactional
    public KnowledgePointVO update(Long id, KnowledgePointDTO dto) {
        KnowledgePoint kp = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识点不存在"));

        // 同事只读：仅 owner 本人（或管理员）可改
        checkKpOwner(kp);

        // 名称去重（owner 维度，如果修改了名称）
        if (!kp.getName().equals(dto.getName())
                && knowledgePointRepository.existsBySubjectAndNameAndTeacherUid(dto.getSubject(), dto.getName(), kp.getTeacherUid())) {
            throw new BusinessException("该学科下已存在同名知识点");
        }

        kp.setName(dto.getName());
        kp.setSubject(dto.getSubject());
        kp.setGradeLevel(dto.getGradeLevel());
        kp.setDescription(dto.getDescription());
        kp.setParentId(dto.getParentId());
        if (dto.getSortOrder() != null) {
            kp.setSortOrder(dto.getSortOrder());
        }

        kp = knowledgePointRepository.save(kp);
        log.info("教师{} 更新知识点: id={}, name={}", getCurrentUserId(), kp.getId(), kp.getName());

        return toVO(kp);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KnowledgePoint kp = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识点不存在"));

        checkKpOwner(kp);

        // 如果有子知识点，清除其parent_id
        List<KnowledgePoint> children = knowledgePointRepository.findBySubjectOrderBySortOrderAsc(kp.getSubject());
        for (KnowledgePoint child : children) {
            if (kp.getId().equals(child.getParentId())) {
                child.setParentId(null);
                knowledgePointRepository.save(child);
            }
        }

        knowledgePointRepository.delete(kp);
        log.info("教师{} 删除知识点: id={}, name={}", getCurrentUserId(), id, kp.getName());
    }

    /** Entity → VO */
    private KnowledgePointVO toVO(KnowledgePoint kp) {
        return KnowledgePointVO.builder()
                .id(kp.getId())
                .subject(kp.getSubject())
                .name(kp.getName())
                .teacherId(AuthServiceImpl.formatUid(kp.getTeacherUid()))
                .gradeLevel(kp.getGradeLevel())
                .description(kp.getDescription())
                .parentId(kp.getParentId())
                .sortOrder(kp.getSortOrder())
                .createdAt(kp.getCreatedAt())
                .updatedAt(kp.getUpdatedAt())
                .build();
    }
}
