package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.KnowledgePointDTO;
import com.eduai.system.entity.KnowledgePoint;
import com.eduai.system.entity.Student;
import com.eduai.system.repository.KnowledgePointRepository;
import com.eduai.system.repository.StudentRepository;
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
import java.util.List;
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
    private final StudentRepository studentRepository;

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

    @Override
    @Transactional(readOnly = true)
    public KnowledgePointPageVO list(int page, int pageSize, String subject, String gradeLevel) {
        checkAuthenticated();

        // 学生未传 gradeLevel 时，自动从学生档案读取年级
        if (gradeLevel == null || gradeLevel.isBlank() || "all".equals(gradeLevel)) {
            Long userId = getCurrentUserId();
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getRoleType() == 4) {
                Student student = studentRepository.findByUserId(userId).orElse(null);
                if (student != null && student.getGrade() != null && !student.getGrade().isBlank()) {
                    gradeLevel = student.getGrade();
                }
            }
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder", "id"));

        // 前缀匹配：学生 grade="初一" 能命中 grade_level="初一·上学期"/"初一·下学期"
        final String finalGradeLevel = gradeLevel;
        Specification<KnowledgePoint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subject"), subject));
            if (finalGradeLevel != null && !finalGradeLevel.isBlank() && !"all".equals(finalGradeLevel)) {
                predicates.add(cb.like(root.get("gradeLevel"), finalGradeLevel + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KnowledgePoint> kpPage = knowledgePointRepository.findAll(spec, pageable);

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
    @Transactional(readOnly = true)
    public KnowledgePointPageVO listByGrades(int page, int pageSize, String subject, String grades) {
        checkAuthenticated();

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder", "id"));

        List<String> gradeList = (grades != null && !grades.isBlank())
                ? Arrays.stream(grades.split(",")).map(String::trim).filter(g -> !g.isEmpty()).collect(Collectors.toList())
                : List.of();

        // 前缀匹配：每个 grade 都做 LIKE grade+'%'
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
        Page<KnowledgePoint> kpPage = knowledgePointRepository.findAll(spec, pageable);

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

        // 去重检查
        if (knowledgePointRepository.existsBySubjectAndName(dto.getSubject(), dto.getName())) {
            throw new BusinessException("该学科下已存在同名知识点");
        }

        KnowledgePoint kp = KnowledgePoint.builder()
                .subject(dto.getSubject())
                .name(dto.getName())
                .gradeLevel(dto.getGradeLevel())
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
        checkTeacherOrAdmin();

        KnowledgePoint kp = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识点不存在"));

        // 名称去重（如果修改了名称）
        if (!kp.getName().equals(dto.getName())
                && knowledgePointRepository.existsBySubjectAndName(dto.getSubject(), dto.getName())) {
            throw new BusinessException("该学科下已存在同名知识点");
        }

        kp.setName(dto.getName());
        kp.setSubject(dto.getSubject());
        kp.setGradeLevel(dto.getGradeLevel());
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
        checkTeacherOrAdmin();

        KnowledgePoint kp = knowledgePointRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识点不存在"));

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
                .gradeLevel(kp.getGradeLevel())
                .parentId(kp.getParentId())
                .sortOrder(kp.getSortOrder())
                .createdAt(kp.getCreatedAt())
                .updatedAt(kp.getUpdatedAt())
                .build();
    }
}