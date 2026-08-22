package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.ai.dto.ChatRequest;
import com.eduai.ai.service.AIChatService;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.PointService;
import com.eduai.security.service.impl.PointServiceImpl;
import com.eduai.system.dto.QuestionUpdateDTO;
import com.eduai.system.dto.QuestionUploadDTO;
import com.eduai.system.entity.Question;
import com.eduai.system.entity.QuestionGradeRecord;
import com.eduai.system.entity.QuestionKnowledgePoint;
import com.eduai.system.entity.Student;
import com.eduai.system.entity.StudentEnrollment;
import com.eduai.system.entity.StudentQuestionProgress;
import com.eduai.system.entity.TeacherStudent;
import com.eduai.system.repository.KnowledgePointRepository;
import com.eduai.system.repository.QuestionGradeRecordRepository;
import com.eduai.system.repository.QuestionKnowledgePointRepository;
import com.eduai.system.repository.QuestionRepository;
import com.eduai.system.repository.StudentEnrollmentRepository;
import com.eduai.system.repository.StudentQuestionProgressRepository;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.repository.TeacherStudentRepository;
import com.eduai.system.service.ImageStorageService;
import com.eduai.system.service.QuestionBankService;
import com.eduai.system.vo.GradeResultVO;
import com.eduai.system.vo.QuestionPageVO;
import com.eduai.system.vo.QuestionVO;
import com.eduai.system.vo.StudentBriefVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final QuestionKnowledgePointRepository questionKnowledgePointRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentQuestionProgressRepository studentQuestionProgressRepository;
    private final ImageStorageService imageStorageService;
    private final PointService pointService;
    private final AIChatService aiChatService;
    private final QuestionGradeRecordRepository questionGradeRecordRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                                               String gradeLevel, String date,
                                               String questionType) {
        checkTeacher();

        // 知识点筛选：提前从中间表取题目ID，避免 CSV 列 LIKE 全表扫描
        List<Long> kpQuestionIds = kpId != null
                ? questionKnowledgePointRepository.findQuestionIdsByKnowledgePointId(kpId)
                : null;
        Specification<Question> spec = buildTeacherQuestionSpec(subject, kpQuestionIds, type, studentId, gradeLevel, date, questionType);
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
        Long teacherId = getCurrentUserId();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));
        // 私域题仅上传老师可见；共享题所有老师可见
        if (!Boolean.TRUE.equals(q.getShared()) && !teacherId.equals(q.getTeacherId())) {
            throw new BusinessException(403, "无权访问他人私域题目");
        }

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
        Long teacherId = getCurrentUserId();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));
        // 新题(teacherId 非空)仅上传老师可改；错题(teacherId 空)为共享批改
        if (q.getTeacherId() != null && !q.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权修改他人题目");
        }

        // 只更新非null字段
        if (dto.getTitle() != null) q.setTitle(dto.getTitle());
        if (dto.getAnswer() != null) q.setAnswer(dto.getAnswer());
        if (dto.getKnowledgePointIds() != null) {
            q.setKnowledgePointIds(dto.getKnowledgePointIds());
            replaceKnowledgePointLinks(id, dto.getKnowledgePointIds());
        }
        if (dto.getDifficulty() != null) q.setDifficulty(dto.getDifficulty());
        if (dto.getGradeLevel() != null) q.setGradeLevel(dto.getGradeLevel());
        if (dto.getDiagramImageUrl() != null) q.setDiagramImageUrl(imageStorageService.persistIfBase64(dto.getDiagramImageUrl(), "diagram"));
        if (dto.getDiagramStatus() != null) q.setDiagramStatus(dto.getDiagramStatus());
        if (dto.getTeacherAnalysis() != null) q.setTeacherAnalysis(dto.getTeacherAnalysis());
        if (dto.getTeacherAnalysisImage() != null) q.setTeacherAnalysisImage(imageStorageService.persistIfBase64(dto.getTeacherAnalysisImage(), "analysis"));
        if (dto.getTeacherAnalysisImageType() != null) q.setTeacherAnalysisImageType(dto.getTeacherAnalysisImageType());
        if (dto.getMastery() != null) q.setMastery(dto.getMastery());
        if (dto.getCompleted() != null) q.setCompleted(dto.getCompleted());
        if (dto.getAnalysis() != null) q.setAnalysis(dto.getAnalysis());
        if (dto.getSolution() != null) q.setSolution(dto.getSolution());
        if (dto.getErrorType() != null) q.setErrorType(dto.getErrorType());
        if (dto.getQuestionType() != null) q.setQuestionType(dto.getQuestionType());
        if (dto.getShared() != null) q.setShared(dto.getShared());

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
                .originalImageUrl(imageStorageService.persistIfBase64(dto.getOriginalImageUrl(), "original"))
                .diagramImageUrl(imageStorageService.persistIfBase64(dto.getDiagramImageUrl(), "diagram"))
                .diagramStatus(dto.getDiagramStatus() != null ? dto.getDiagramStatus() : "NONE")
                .aiExtractedText(dto.getAiExtractedText())
                .teacherAnalysis(dto.getTeacherAnalysis())
                .teacherAnalysisImage(imageStorageService.persistIfBase64(dto.getTeacherAnalysisImage(), "analysis"))
                .teacherAnalysisImageType(dto.getTeacherAnalysisImageType())
                .questionType(dto.getQuestionType())
                .shared(dto.getShared() != null ? dto.getShared() : true)
                .mastery("UNMASTERED")
                .build();

        q = questionRepository.save(q);
        replaceKnowledgePointLinks(q.getId(), dto.getKnowledgePointIds());
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
    @Transactional(readOnly = true)
    public QuestionPageVO listTeacherStudentWrongQuestions(int page, int pageSize, String subject, Long studentId) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        // 1. 学生存在性校验（404）
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        // 2. 绑定关系校验（403）：teacherId ↔ studentId 必须存在绑定，否则无权查看
        boolean bound = teacherStudentRepository.findByTeacherIdAndStudentId(teacherId, studentId).isPresent();
        if (!bound) {
            throw new BusinessException(403, "未绑定该学生，无权查看");
        }

        // 3. 查该学生的错题（复用学生端查询逻辑，studentId 即 students.id）
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Question> questionPage;
        if (subject != null && !subject.isBlank()) {
            questionPage = questionRepository.findByStudentIdAndSubjectAndType(studentId, subject, "WRONG", pageable);
        } else {
            questionPage = questionRepository.findByStudentIdAndType(studentId, "WRONG", pageable);
        }

        // 学生姓名回填到 VO（老师端展示该学生的错题列表需要）
        Map<Long, String> studentNameMap = Map.of(student.getId(), student.getName());
        Map<Long, String> kpNameMap = resolveKpNameMap(questionPage.getContent());
        List<QuestionVO> list = questionPage.getContent().stream()
                .map(q -> toVO(q, studentNameMap, Collections.emptyMap(), kpNameMap))
                .collect(Collectors.toList());

        log.info("老师{} 查看学生{}错题: subject={}, 共{}条", teacherId, studentId, subject, questionPage.getTotalElements());
        return QuestionPageVO.builder()
                .list(list)
                .total(questionPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));
        // 新题(teacherId 非空)仅上传老师可删；错题(teacherId 空)为共享批改
        if (q.getTeacherId() != null && !q.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权删除他人题目");
        }

        questionRepository.delete(q);
        log.info("教师{} 删除题目: id={}", teacherId, id);
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
    public QuestionPageVO listStudentNewQuestions(int page, int pageSize, String subject,
                                                   String gradeLevel, String questionType) {
        Student student = checkStudent();

        // 未传 gradeLevel 时，自动从学生档案读取年级
        if (gradeLevel == null || gradeLevel.isBlank()) {
            gradeLevel = student.getGrade();
        }

        // 解析逗号分隔的年级列表
        List<String> gradeList = new ArrayList<>();
        if (gradeLevel != null && !gradeLevel.isBlank()) {
            for (String g : gradeLevel.split(",")) {
                String trimmed = g.trim();
                if (!trimmed.isEmpty()) {
                    gradeList.add(trimmed);
                }
            }
        }

        // 获取学生的老师ID列表（用于私域题目可见性）
        List<Long> teacherUserIds = new ArrayList<>();
        try {
            teacherUserIds = teacherStudentRepository.findByStudentId(student.getId())
                    .stream().map(com.eduai.system.entity.TeacherStudent::getTeacherId).toList();
        } catch (Exception ignored) {}

        List<Long> finalTeacherUserIds = teacherUserIds;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("type"), "NEW"));
            predicates.add(cb.equal(root.get("subject"), subject));

            if (!gradeList.isEmpty()) {
                Predicate gradeIn = root.get("gradeLevel").in(gradeList);
                Predicate gradeNull = cb.isNull(root.get("gradeLevel"));
                Predicate gradeEmpty = cb.equal(root.get("gradeLevel"), "");
                predicates.add(cb.or(gradeIn, gradeNull, gradeEmpty));
            }

            // 题型筛选
            if (questionType != null && !questionType.isBlank()) {
                predicates.add(cb.equal(root.get("questionType"), questionType));
            }

            // 私域隔离：公域 OR 关联老师的私域题
            Predicate sharedTrue = cb.equal(root.get("shared"), true);
            if (!finalTeacherUserIds.isEmpty()) {
                Predicate fromMyTeacher = root.get("teacherId").in(finalTeacherUserIds);
                predicates.add(cb.or(sharedTrue, fromMyTeacher));
            } else {
                predicates.add(sharedTrue);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Question> questionPage = questionRepository.findAll(spec, pageable);
        log.info("学生新题查询: subject={}, gradeList={}, 结果={}条", subject, gradeList, questionPage.getTotalElements());

        Map<Long, String> kpNameMap = resolveKpNameMap(questionPage.getContent());
        List<QuestionVO> list = questionPage.getContent().stream()
                .map(q -> toVO(q, Collections.emptyMap(), Collections.emptyMap(), kpNameMap))
                .collect(Collectors.toList());
        // 共享新题的掌握度/完成状态按学生隔离，覆盖当前学生进度
        overlayProgress(list, student.getId());

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
    private Specification<Question> buildTeacherQuestionSpec(String subject, List<Long> kpQuestionIds, String type,
                                                              Long studentId, String gradeLevel, String date,
                                                              String questionType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 学科筛选（必填）
            predicates.add(cb.equal(root.get("subject"), subject));

            // 知识点筛选：走 question_knowledge_point 中间表（索引精确匹配，消除前缀歧义）
            if (kpQuestionIds != null) {
                if (kpQuestionIds.isEmpty()) {
                    predicates.add(cb.disjunction()); // 该知识点无题 → 返回空
                } else {
                    predicates.add(root.get("id").in(kpQuestionIds));
                }
            }

            // 类型筛选：WRONG / NEW
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 学生筛选
            if (studentId != null) {
                predicates.add(cb.equal(root.get("studentId"), studentId));
            }

            // 年级筛选
            if (gradeLevel != null && !gradeLevel.isBlank() && !"all".equals(gradeLevel)) {
                predicates.add(cb.like(root.get("gradeLevel"), gradeLevel + "%"));
            }

            // 日期筛选
            if (date != null && !date.isBlank()) {
                try {
                    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                    predicates.add(cb.equal(root.get("createdAt").as(LocalDate.class), localDate));
                } catch (Exception e) {
                    log.warn("日期格式错误: {}", date);
                }
            }

            // 题型筛选
            if (questionType != null && !questionType.isBlank()) {
                predicates.add(cb.equal(root.get("questionType"), questionType));
            }

            // 权限隔离：老师只能看公域题目 + 自己的私域题目
            Long currentTeacherId = getCurrentUserId();
            Predicate sharedOr = cb.or(
                    cb.equal(root.get("shared"), true),
                    cb.equal(root.get("teacherId"), currentTeacherId)
            );
            predicates.add(sharedOr);

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
                .completed(q.getCompleted())
                .questionType(q.getQuestionType())
                .shared(q.getShared())
                .errorType(q.getErrorType())
                .gradeLevel(q.getGradeLevel())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    // ==================== 学生错题录入 ====================

    @Override
    @Transactional
    public QuestionVO addWrongQuestion(String subject, Map<String, Object> body) {
        Student student = checkStudent();

        Question q = new Question();
        q.setSubject(subject);
        q.setType("WRONG");
        q.setSource("STUDENT");
        q.setStudentId(student.getId());
        q.setTitle((String) body.get("title"));
        q.setKnowledgePointIds(null); // 待老师标识
        q.setDifficulty(getString(body, "difficulty", "MEDIUM"));
        q.setGradeLevel(getString(body, "gradeLevel", ""));
        q.setAnalysis((String) body.get("analysis"));
        q.setSolution(getString(body, "solution", ""));
        q.setErrorType(getString(body, "errorType", ""));
        q.setMastery("UNMASTERED");
        q.setAnswer(getString(body, "answer", ""));
        q.setOriginalImageUrl(imageStorageService.persistIfBase64(getString(body, "originalImageUrl", ""), "original"));
        q.setDiagramImageUrl(imageStorageService.persistIfBase64(getString(body, "diagramImageUrl", ""), "diagram"));

        questionRepository.save(q);
        log.info("学生 {} 录入错题: id={}, subject={}, title={}", student.getId(), q.getId(), subject,
                q.getTitle() != null ? q.getTitle().substring(0, Math.min(30, q.getTitle().length())) : "");

        return toVO(q, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    // ==================== 学生掌握度更新 ====================

    @Override
    @Transactional
    public void updateMastery(Long questionId, Map<String, Object> body) {
        Student student = checkStudent();

        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        // 越权校验：错题仅本人可改；共享新题(NEW) studentId 为空，进度按学生隔离
        if (q.getStudentId() != null && !q.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权操作他人题目");
        }

        String mastery = parseMastery(body.get("mastery"));
        Boolean completed = parseCompleted(body.get("completed"));

        if (q.getStudentId() != null) {
            // 学生私有错题：进度直接落在题行上（一行一学生，无并发覆盖）
            if (mastery != null) q.setMastery(mastery);
            if (completed != null) q.setCompleted(completed);
            Object answerVal = body.get("answer");
            if (answerVal instanceof String s && !s.isBlank()) {
                q.setAnswer(s);
            }
            questionRepository.save(q);
            log.info("学生更新错题掌握度: questionId={}, mastery={}, completed={}",
                    questionId, q.getMastery(), q.getCompleted());
        } else {
            // 共享新题：进度写入 student_question_progress，避免多学生互相覆盖
            StudentQuestionProgress p = studentQuestionProgressRepository
                    .findByStudentIdAndQuestionId(student.getId(), questionId)
                    .orElseGet(() -> StudentQuestionProgress.builder()
                            .studentId(student.getId())
                            .questionId(questionId)
                            .mastery("UNMASTERED")
                            .completed(false)
                            .build());
            if (mastery != null) p.setMastery(mastery);
            if (completed != null) p.setCompleted(completed);
            studentQuestionProgressRepository.save(p);
            // answer 是共享题的正确答案，学生不得修改，忽略
            log.info("学生更新共享题进度: questionId={}, studentId={}, mastery={}, completed={}",
                    questionId, student.getId(), p.getMastery(), p.getCompleted());
        }
    }

    /** 解析 mastery（仅接受 UNMASTERED/FAMILIAR/MASTERED），缺失或非法返回 null */
    private String parseMastery(Object val) {
        if (val == null) return null;
        String s = val instanceof String str ? str : String.valueOf(val);
        return switch (s) {
            case "UNMASTERED", "FAMILIAR", "MASTERED" -> s;
            default -> null;
        };
    }

    /** 解析 completed（boolean / 0/1 / "true"/"1"），缺失返回 null */
    private Boolean parseCompleted(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        if (val instanceof String s) return "true".equalsIgnoreCase(s) || "1".equals(s);
        return null;
    }

    /** 共享新题：把当前学生的掌握度/完成状态从 student_question_progress 覆盖到 VO */
    private void overlayProgress(List<QuestionVO> list, Long studentId) {
        if (list.isEmpty()) return;
        List<Long> qids = list.stream()
                .map(QuestionVO::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, StudentQuestionProgress> m = studentQuestionProgressRepository
                .findByStudentIdAndQuestionIdIn(studentId, qids).stream()
                .collect(Collectors.toMap(StudentQuestionProgress::getQuestionId, p -> p, (a, b) -> a));
        for (QuestionVO vo : list) {
            StudentQuestionProgress p = m.get(vo.getId());
            if (p != null) {
                if (p.getMastery() != null) vo.setMastery(p.getMastery());
                vo.setCompleted(p.getCompleted());
            }
        }
    }

    // ==================== 同类题目推荐 ====================

    @Override
    @Transactional(readOnly = true)
    public List<QuestionVO> listSimilarQuestions(String subject, Long kpId, int count, Long excludeId) {
        checkStudent();

        // 走 question_knowledge_point 中间表索引精确匹配，替代 CSV 列 LIKE
        List<Long> kpQuestionIds = questionKnowledgePointRepository.findQuestionIdsByKnowledgePointId(kpId);
        if (kpQuestionIds.isEmpty()) return Collections.emptyList();

        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subject"), subject));
            predicates.add(root.get("id").in(kpQuestionIds));
            if (excludeId != null) {
                predicates.add(cb.notEqual(root.get("id"), excludeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Question> all = questionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        log.info("同知识点题目查询: subject={}, kpId={}, 匹配{}道", subject, kpId, all.size());

        // 随机打乱，取前 count 道
        Collections.shuffle(all);
        List<Question> picked = all.stream().limit(count).collect(Collectors.toList());

        Map<Long, String> kpNameMap = resolveKpNameMap(picked);
        return picked.stream()
                .map(q -> toVO(q, Collections.emptyMap(), Collections.emptyMap(), kpNameMap))
                .collect(Collectors.toList());
    }

    private String getString(Map<String, Object> body, String key, String defaultValue) {
        Object val = body.get(key);
        return val instanceof String s && !s.isBlank() ? s : defaultValue;
    }

    /** 重建题目-知识点关联：先清后写（与 knowledge_point_ids CSV 列保持同步） */
    private void replaceKnowledgePointLinks(Long questionId, String kpIdsCsv) {
        questionKnowledgePointRepository.deleteByQuestionId(questionId);
        // 强制 flush：派生删除(deleteByQuestionId)与后续 saveAll 在同一事务时，
        // Hibernate 默认 flush 顺序是 insert 先于 delete，若新旧关联存在重叠 kpId
        // 会撞 question_knowledge_point.uk_question_kp 唯一约束，必须先让 DELETE 落库
        questionKnowledgePointRepository.flush();
        if (kpIdsCsv == null || kpIdsCsv.isBlank()) return;
        Set<Long> ids = Arrays.stream(kpIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        List<QuestionKnowledgePoint> links = ids.stream()
                .map(kpId -> QuestionKnowledgePoint.builder()
                        .questionId(questionId)
                        .knowledgePointId(kpId)
                        .build())
                .collect(Collectors.toList());
        if (!links.isEmpty()) {
            questionKnowledgePointRepository.saveAll(links);
        }
    }

    // ==================== AI 批改 ====================

    @Override
    @Transactional
    public GradeResultVO gradeQuestion(Long questionId, MultipartFile file, String answerText) {
        Student student = checkStudent();          // students.id（记录/题目归属）
        Long userId = getCurrentUserId();          // users.id（智学点扣减）

        // 幂等：一题一学生只批改一次，命中缓存直接返回，不扣点、不调 AI
        Optional<QuestionGradeRecord> cached = questionGradeRecordRepository
                .findByQuestionIdAndStudentId(questionId, student.getId());
        if (cached.isPresent()) {
            QuestionGradeRecord r = cached.get();
            log.info("AI批改命中缓存: questionId={}, studentId={}", questionId, student.getId());
            return GradeResultVO.builder()
                    .correct(r.getCorrect())
                    .result(r.getResult())
                    .cached(true)
                    .build();
        }

        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(404, "题目不存在"));

        // 越权校验：错题(studentId 非空)仅本人可批改；共享新题(studentId 空)所有学生可批改
        if (q.getStudentId() != null && !q.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权批改他人题目");
        }

        // 答题图片落盘（multipart → COS / 本地）
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new BusinessException(400, "答题图片读取失败");
            }
            imageUrl = imageStorageService.persistBytes(bytes, extractFileExt(file.getOriginalFilename()), "grade");
        }

        // 先扣点（FOR UPDATE 原子，同事务），AI 失败则随事务回滚
        pointService.consume(userId, PointServiceImpl.COST_AI_QUESTION_GRADE, "AI批改");

        // 同步调 AI（等待返回后决定 COMMIT/ROLLBACK）
        String aiResult = aiChatService.gradeQuestion(buildGradeRequest(q, answerText, imageUrl));

        // 解析 {correct, result}
        GradeParse pr = parseGradeResult(aiResult);

        // 落记录（唯一键兜底幂等）
        questionGradeRecordRepository.save(QuestionGradeRecord.builder()
                .questionId(questionId)
                .studentId(student.getId())
                .answerImageUrl(imageUrl)
                .answerText(answerText)
                .correct(pr.correct())
                .result(pr.result())
                .build());

        log.info("AI批改完成: questionId={}, studentId={}, correct={}", questionId, student.getId(), pr.correct());
        return GradeResultVO.builder()
                .correct(pr.correct())
                .result(pr.result())
                .cached(false)
                .build();
    }

    /** 构建批改 ChatRequest：题目 + 标准答案 + 学生作答文字 + 答题图片 */
    private ChatRequest buildGradeRequest(Question q, String answerText, String imageUrl) {
        StringBuilder sb = new StringBuilder();
        if (q.getTitle() != null && !q.getTitle().isBlank()) {
            sb.append("题目：").append(q.getTitle());
        }
        if (q.getAnswer() != null && !q.getAnswer().isBlank()) {
            sb.append("\n标准答案：").append(q.getAnswer());
        }
        if (answerText != null && !answerText.isBlank()) {
            sb.append("\n学生作答：").append(answerText);
        }
        sb.append("\n请结合学生作答文字及答题图片判断对错，并给出批改说明。");

        ChatRequest request = new ChatRequest();
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole("user");
        msg.setContent(sb.toString());
        request.setMessages(List.of(msg));
        if (imageUrl != null && !imageUrl.isBlank()) {
            request.setImageUrl(imageUrl);
        }
        return request;
    }

    /** 从 AI 返回文本中解析 {correct, result}，失败时启发式兜底 */
    private GradeParse parseGradeResult(String aiResult) {
        String json = aiResult != null ? aiResult.trim() : "";
        try {
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("非 JSON 输出");
            }
            JsonNode node = OBJECT_MAPPER.readTree(json.substring(start, end + 1));
            Boolean correct = node.has("correct") && !node.get("correct").isNull()
                    ? node.get("correct").asBoolean() : null;
            String result = node.has("result") && !node.get("result").isNull()
                    ? node.get("result").asText() : null;
            if (correct == null) {
                correct = json.contains("正确") && !json.contains("不正确") && !json.contains("错误");
            }
            if (result == null || result.isBlank()) {
                result = aiResult;
            }
            return new GradeParse(correct, result);
        } catch (Exception e) {
            log.warn("批改结果解析失败，原文兜底: {}", e.getMessage());
            boolean correct = json.contains("正确") && !json.contains("错误");
            return new GradeParse(correct, aiResult != null ? aiResult : "批改失败");
        }
    }

    /** 从文件名提取扩展名（非法时兜底 png） */
    private String extractFileExt(String filename) {
        if (filename == null || !filename.contains(".")) return "png";
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,5}") ? ext : "png";
    }

    private record GradeParse(boolean correct, String result) {}
}