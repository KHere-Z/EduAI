package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.QuestionUpdateDTO;
import com.eduai.system.dto.QuestionUploadDTO;
import com.eduai.system.entity.Question;
import com.eduai.system.entity.Student;
import com.eduai.system.entity.StudentEnrollment;
import com.eduai.system.entity.TeacherStudent;
import com.eduai.system.repository.KnowledgePointRepository;
import com.eduai.system.repository.QuestionRepository;
import com.eduai.system.repository.StudentEnrollmentRepository;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.repository.TeacherStudentRepository;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import com.eduai.system.vo.StudentBriefVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    /** 校验当前用户是否为教师（roleType=3） */
    private void checkTeacher() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(403, "仅教师可访问");
        }
    }

    /** 校验当前用户是否为学生（roleType=4） */
    private Student checkStudent() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 4) {
            throw new BusinessException(403, "仅学生可访问");
        }
        // 查找学生档案
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生档案不存在，请联系管理员"));
    }

    /** 获取当前用户ID */
    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    // ==================== 老师端 ====================

    @Override
    @Transactional(readOnly = true)
    public QuestionPageVO listTeacherQuestions(int page, int pageSize, String subject,
                                               Long kpId, String type, Long studentId,
                                               String gradeLevel, String date) {
        checkTeacher();

        Specification<Question> spec = buildTeacherQuestionSpec(subject, kpId, type, studentId, gradeLevel, date);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Question> questionPage = questionRepository.findAll(spec, pageable);

        // 批量查询学生名称
        Set<Long> studentIds = questionPage.getContent().stream()
                .map(Question::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> studentNameMap = studentIds.isEmpty()
                ? Collections.emptyMap()
                : studentRepository.findAllById(studentIds).stream()
                        .collect(Collectors.toMap(Student::getId, Student::getName));

        // 批量查询老师名称
        Set<Long> teacherIds = questionPage.getContent().stream()
                .map(Question::getTeacherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = teacherIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        // 批量解析知识点 ID → 名称
        Map<Long, String> kpNameMap = resolveKpNameMap(questionPage.getContent());

        List<QuestionVO> list = questionPage.getContent().stream()
                .map(q -> toVO(q, studentNameMap, teacherNameMap, kpNameMap))
                .collect(Collectors.toList());

        return QuestionPageVO.builder()
                .list(list)
                .total(questionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionVO getQuestion(Long id) {
        checkTeacher();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        Map<Long, String> studentNameMap = Collections.emptyMap();
        if (q.getStudentId() != null) {
            studentNameMap = studentRepository.findById(q.getStudentId())
                    .map(s -> Map.of(s.getId(), s.getName()))
                    .orElse(Collections.emptyMap());
        }

        Map<Long, String> teacherNameMap = Collections.emptyMap();
        if (q.getTeacherId() != null) {
            teacherNameMap = userRepository.findById(q.getTeacherId())
                    .map(u -> Map.of(u.getId(), u.getRealName() != null ? u.getRealName() : u.getUsername()))
                    .orElse(Collections.emptyMap());
        }

        Map<Long, String> kpNameMap = resolveKpNameMap(List.of(q));
        return toVO(q, studentNameMap, teacherNameMap, kpNameMap);
    }

    @Override
    @Transactional
    public QuestionVO updateQuestion(Long id, QuestionUpdateDTO dto) {
        checkTeacher();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        // 只更新非null字段
        if (dto.getTitle() != null) q.setTitle(dto.getTitle());
        if (dto.getAnswer() != null) q.setAnswer(dto.getAnswer());
        if (dto.getKnowledgePointIds() != null) q.setKnowledgePointIds(dto.getKnowledgePointIds());
        if (dto.getDifficulty() != null) q.setDifficulty(dto.getDifficulty());
        if (dto.getGradeLevel() != null) q.setGradeLevel(dto.getGradeLevel());
        if (dto.getDiagramImageUrl() != null) q.setDiagramImageUrl(dto.getDiagramImageUrl());
        if (dto.getDiagramStatus() != null) q.setDiagramStatus(dto.getDiagramStatus());
        if (dto.getTeacherAnalysis() != null) q.setTeacherAnalysis(dto.getTeacherAnalysis());
        if (dto.getTeacherAnalysisImage() != null) q.setTeacherAnalysisImage(dto.getTeacherAnalysisImage());
        if (dto.getTeacherAnalysisImageType() != null) q.setTeacherAnalysisImageType(dto.getTeacherAnalysisImageType());
        if (dto.getMastery() != null) q.setMastery(dto.getMastery());
        if (dto.getAnalysis() != null) q.setAnalysis(dto.getAnalysis());
        if (dto.getSolution() != null) q.setSolution(dto.getSolution());
        if (dto.getErrorType() != null) q.setErrorType(dto.getErrorType());

        q = questionRepository.save(q);
        log.info("教师{} 更新题目: id={}", getCurrentUserId(), id);

        return getQuestion(id);
    }

    @Override
    @Transactional
    public QuestionVO uploadQuestion(QuestionUploadDTO dto) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        Question q = Question.builder()
                .subject(dto.getSubject())
                .type("NEW")
                .source("TEACHER")
                .teacherId(teacherId)
                .title(dto.getTitle())
                .answer(dto.getAnswer())
                .knowledgePointIds(dto.getKnowledgePointIds())
                .difficulty(dto.getDifficulty())
                .gradeLevel(dto.getGradeLevel())
                .originalImageUrl(dto.getOriginalImageUrl())
                .diagramImageUrl(dto.getDiagramImageUrl())
                .diagramStatus(dto.getDiagramStatus() != null ? dto.getDiagramStatus() : "NONE")
                .aiExtractedText(dto.getAiExtractedText())
                .teacherAnalysis(dto.getTeacherAnalysis())
                .teacherAnalysisImage(dto.getTeacherAnalysisImage())
                .teacherAnalysisImageType(dto.getTeacherAnalysisImageType())
                .mastery("UNMASTERED")
                .build();

        q = questionRepository.save(q);
        log.info("教师{} 上传新题: id={}, subject={}, title={}", teacherId, q.getId(), q.getSubject(),
                q.getTitle().length() > 30 ? q.getTitle().substring(0, 30) + "..." : q.getTitle());

        // 查询老师名称
        Map<Long, String> teacherNameMap = userRepository.findById(teacherId)
                .map(u -> Map.of(teacherId, u.getRealName() != null ? u.getRealName() : u.getUsername()))
                .orElse(Collections.emptyMap());

        Map<Long, String> kpNameMap = resolveKpNameMap(List.of(q));
        return toVO(q, Collections.emptyMap(), teacherNameMap, kpNameMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentBriefVO> listTeacherStudents(String subject) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        // 查当前老师的所有学生关系
        List<TeacherStudent> tsList = teacherStudentRepository.findByTeacherId(teacherId);
        if (tsList.isEmpty()) return Collections.emptyList();

        // 按学科筛选：直接从 enrollment 表查（避免懒加载问题）
        Set<Long> filteredStudentIds;
        if (subject != null && !subject.isBlank()) {
            Set<Long> tsIds = tsList.stream().map(TeacherStudent::getId).collect(Collectors.toSet());
            List<StudentEnrollment> enrollments = studentEnrollmentRepository
                    .findByTeacherStudentIdInAndSubject(tsIds, subject);
            // 通过 enrollment → teacherStudent → student 反查学生ID
            Set<Long> matchedTsIds = enrollments.stream()
                    .map(StudentEnrollment::getTeacherStudentId)
                    .collect(Collectors.toSet());
            filteredStudentIds = tsList.stream()
                    .filter(ts -> matchedTsIds.contains(ts.getId()))
                    .map(TeacherStudent::getStudentId)
                    .collect(Collectors.toSet());
        } else {
            filteredStudentIds = tsList.stream()
                    .map(TeacherStudent::getStudentId)
                    .collect(Collectors.toSet());
        }

        if (filteredStudentIds.isEmpty()) return Collections.emptyList();

        // 批量查学生姓名和年级
        Map<Long, Student> studentMap = studentRepository.findAllById(filteredStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        return filteredStudentIds.stream()
                .map(sid -> {
                    Student s = studentMap.get(sid);
                    return StudentBriefVO.builder()
                            .studentId(sid)
                            .studentName(s != null ? s.getName() : "未知")
                            .grade(s != null ? s.getGrade() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        checkTeacher();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        questionRepository.delete(q);
        log.info("教师{} 删除题目: id={}", getCurrentUserId(), id);
    }

    // ==================== 学生端 ====================

    @Override
    @Transactional(readOnly = true)
    public QuestionPageVO listStudentWrongQuestions(int page, int pageSize, String subject) {
        Student student = checkStudent();

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Question> questionPage;

        if (subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findByStudentIdAndSubjectAndType(
                    student.getId(), subject, "WRONG", pageable);
        } else {
            questionPage = questionRepository.findByStudentIdAndType(
                    student.getId(), "WRONG", pageable);
        }

        Map<Long, String> kpNameMap = resolveKpNameMap(questionPage.getContent());
        List<QuestionVO> list = questionPage.getContent().stream()
                .map(q -> toVO(q, Collections.emptyMap(), Collections.emptyMap(), kpNameMap))
                .collect(Collectors.toList());

        return QuestionPageVO.builder()
                .list(list)
                .total(questionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionPageVO listStudentNewQuestions(int page, int pageSize, String subject, String gradeLevel) {
        Student student = checkStudent();

        // 未传 gradeLevel 时，自动从学生档案读取年级
        if (gradeLevel == null || gradeLevel.isBlank()) {
            gradeLevel = student.getGrade();
        }

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        // 前缀匹配：学生 grade="初一" 能命中 grade_level="初一·上学期"/"初一·下学期"
        final String finalGradeLevel = gradeLevel;
        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("type"), "NEW"));
            predicates.add(cb.equal(root.get("subject"), subject));
            if (finalGradeLevel != null && !finalGradeLevel.isBlank()) {
                predicates.add(cb.like(root.get("gradeLevel"), finalGradeLevel + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Question> questionPage = questionRepository.findAll(spec, pageable);

        Map<Long, String> kpNameMap = resolveKpNameMap(questionPage.getContent());
        List<QuestionVO> list = questionPage.getContent().stream()
                .map(q -> toVO(q, Collections.emptyMap(), Collections.emptyMap(), kpNameMap))
                .collect(Collectors.toList());

        return QuestionPageVO.builder()
                .list(list)
                .total(questionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 构建老师端题目动态查询条件（7维筛选）
     */
    private Specification<Question> buildTeacherQuestionSpec(String subject, Long kpId, String type,
                                                              Long studentId, String gradeLevel, String date) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 学科筛选（必填）
            predicates.add(cb.equal(root.get("subject"), subject));

            // 知识点筛选（knowledge_point_ids 是逗号分隔的字符串，用 LIKE 查询）
            if (kpId != null) {
                predicates.add(cb.like(root.get("knowledgePointIds"), "%" + kpId + "%"));
            }

            // 类型筛选：WRONG / NEW
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 学生筛选
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }

            // 年级筛选（前缀匹配："初一" 命中 "初一·上学期"/"初一·下学期"）
            if (gradeLevel != null && !gradeLevel.isBlank() && !"all".equals(gradeLevel)) {
                predicates.add(cb.like(root.get("gradeLevel"), gradeLevel + "%"));
            }

            // 日期筛选（按 created_at 过滤）
            if (date != null && !date.isBlank()) {
                try {
                    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                    predicates.add(cb.equal(root.get("createdAt").as(LocalDate.class), localDate));
                } catch (Exception e) {
                    // 日期格式错误，忽略此条件
                    log.warn("日期格式错误: {}", date);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 批量解析知识点 ID → 名称映射
     * knowledgePointIds 如 "3,7,12" → 查库后返回 {3L:"一元一次方程", 7L:"全等三角形", ...}
     */
    private Map<Long, String> resolveKpNameMap(List<Question> questions) {
        Set<Long> allKpIds = questions.stream()
                .map(Question::getKnowledgePointIds)
                .filter(ids -> ids != null && !ids.isBlank())
                .flatMap(ids -> Arrays.stream(ids.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        if (allKpIds.isEmpty()) return Collections.emptyMap();
        return knowledgePointRepository.findAllById(allKpIds).stream()
                .collect(Collectors.toMap(
                        kp -> kp.getId(),
                        kp -> kp.getName(),
                        (a, b) -> a));
    }

    /** 解析单条题目的知识点名称（从 knowledgePointIds 如 "1,3,7" → "有理数运算,一元一次方程" */
    private String resolveKpNames(String knowledgePointIds, Map<Long, String> kpNameMap) {
        if (knowledgePointIds == null || knowledgePointIds.isBlank() || kpNameMap.isEmpty()) return null;
        return Arrays.stream(knowledgePointIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(id -> kpNameMap.getOrDefault(Long.valueOf(id), id))
                .collect(Collectors.joining(","));
    }

    /** Question → QuestionVO */
    private QuestionVO toVO(Question q, Map<Long, String> studentNameMap,
                            Map<Long, String> teacherNameMap, Map<Long, String> kpNameMap) {
        String studentName = q.getStudentId() != null
                ? studentNameMap.getOrDefault(q.getStudentId(), null)
                : null;
        String teacherName = q.getTeacherId() != null
                ? teacherNameMap.getOrDefault(q.getTeacherId(), null)
                : null;

        return QuestionVO.builder()
                .id(q.getId())
                .subject(q.getSubject())
                .type(q.getType())
                .source(q.getSource())
                .studentId(q.getStudentId())
                .studentName(studentName)
                .teacherId(q.getTeacherId())
                .teacherName(teacherName)
                .originalImageUrl(q.getOriginalImageUrl())
                .diagramImageUrl(q.getDiagramImageUrl())
                .diagramStatus(q.getDiagramStatus())
                .aiExtractedText(q.getAiExtractedText())
                .title(q.getTitle())
                .knowledgePointIds(q.getKnowledgePointIds())
                .knowledgePointNames(resolveKpNames(q.getKnowledgePointIds(), kpNameMap))
                .answer(q.getAnswer())
                .analysis(q.getAnalysis())
                .solution(q.getSolution())
                .similarJson(q.getSimilarJson())
                .teacherAnalysis(q.getTeacherAnalysis())
                .teacherAnalysisImage(q.getTeacherAnalysisImage())
                .teacherAnalysisImageType(q.getTeacherAnalysisImageType())
                .difficulty(q.getDifficulty())
                .mastery(q.getMastery())
                .errorType(q.getErrorType())
                .gradeLevel(q.getGradeLevel())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}