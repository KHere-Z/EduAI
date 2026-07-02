package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.system.dto.EnrollmentDTO;
import com.eduai.system.dto.SessionDTO;
import com.eduai.system.dto.StudentDTO;
import com.eduai.system.entity.Student;
import com.eduai.system.entity.StudentEnrollment;
import com.eduai.system.entity.StudentSession;
import com.eduai.system.entity.TeacherStudent;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.repository.StudentSessionRepository;
import com.eduai.system.repository.TeacherStudentRepository;
import com.eduai.system.service.StudentService;
import com.eduai.system.vo.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生管理 Service 实现（v2：通过 teacher_student 关系表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentSessionRepository studentSessionRepository;

    private Long getCurrentTeacherId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPageVO list(int page, int pageSize, String keyword, String subject, String grade) {
        Long teacherId = getCurrentTeacherId();

        Specification<TeacherStudent> spec = buildSpecification(teacherId, keyword, subject, grade);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<TeacherStudent> tsPage = teacherStudentRepository.findAll(spec, pageable);

        List<StudentVO> list = tsPage.getContent().stream()
                .map(this::toStudentVO)
                .collect(Collectors.toList());

        return StudentPageVO.builder()
                .list(list)
                .total(tsPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentVO getById(Long id) {
        Long teacherId = getCurrentTeacherId();
        TeacherStudent ts = teacherStudentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));
        return toStudentVO(ts);
    }

    @Override
    @Transactional
    public StudentVO create(StudentDTO dto) {
        Long teacherId = getCurrentTeacherId();

        // 1. 查找或创建 Student（按姓名+学校去重）
        Student student = studentRepository.findByNameAndSchool(dto.getName(), dto.getSchool())
                .orElseGet(() -> {
                    Student s = new Student();
                    s.setName(dto.getName());
                    s.setGender(dto.getGender());
                    s.setContact(dto.getContact());
                    s.setGrade(dto.getGrade());
                    s.setSchool(dto.getSchool());
                    return studentRepository.save(s);
                });

        // 2. 检查是否已存在相同的老师-学生关系
        teacherStudentRepository.findByTeacherIdAndStudentId(teacherId, student.getId())
                .ifPresent(ts -> {
                    throw new BusinessException("该学生已在您的列表中，请勿重复添加");
                });

        // 3. 创建老师-学生关系
        TeacherStudent ts = new TeacherStudent();
        ts.setTeacherId(teacherId);
        ts.setStudentId(student.getId());
        ts.setHoursLeft(dto.getHoursLeft() != null ? dto.getHoursLeft() : 0);
        ts.setRegDate(dto.getRegDate());
        ts.setEnrollments(new ArrayList<>());

        buildEnrollments(ts, dto.getEnrollments());

        ts = teacherStudentRepository.save(ts);
        log.info("create: 老师{}添加学生{}成功, ts.id={}", teacherId, student.getId(), ts.getId());
        return toStudentVO(ts);
    }

    @Override
    @Transactional
    public StudentVO update(Long id, StudentDTO dto) {
        Long teacherId = getCurrentTeacherId();
        TeacherStudent ts = teacherStudentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        // 更新 Student 基本信息
        Student student = ts.getStudent();
        if (student != null) {
            student.setName(dto.getName());
            student.setGender(dto.getGender());
            student.setContact(dto.getContact());
            student.setGrade(dto.getGrade());
            student.setSchool(dto.getSchool());
        }

        // 更新关系属性
        ts.setHoursLeft(dto.getHoursLeft() != null ? dto.getHoursLeft() : ts.getHoursLeft());
        ts.setRegDate(dto.getRegDate());

        // 先删后插 enrollments
        ts.getEnrollments().clear();
        buildEnrollments(ts, dto.getEnrollments());

        ts = teacherStudentRepository.save(ts);
        return toStudentVO(ts);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long teacherId = getCurrentTeacherId();
        TeacherStudent ts = teacherStudentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));
        teacherStudentRepository.delete(ts);
    }

    @Override
    @Transactional
    public void updateHours(Long id, int delta) {
        Long teacherId = getCurrentTeacherId();
        TeacherStudent ts = teacherStudentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        int newHours = Math.max(0, ts.getHoursLeft() + delta);
        ts.setHoursLeft(newHours);
        teacherStudentRepository.save(ts);
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarVO calendar(int year, int month, Long teacherId) {
        final Long effectiveTeacherId = teacherId != null ? teacherId : getCurrentTeacherId();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<StudentSession> sessions = studentSessionRepository
                .findSessionsByTeacherAndDateRange(effectiveTeacherId, start, end);

        Map<String, List<CalendarVO.CalendarEntry>> datesMap = new LinkedHashMap<>();

        if (!sessions.isEmpty()) {
            Set<Long> enrollmentIds = sessions.stream()
                    .map(StudentSession::getEnrollmentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 查询所有相关 teacher_student 记录（含学生和科目信息）
            List<TeacherStudent> tsList = teacherStudentRepository.findAll(
                    (root, query, cb) -> {
                        Join<Object, Object> enrollments = root.join("enrollments", JoinType.INNER);
                        return cb.and(
                                cb.equal(root.get("teacherId"), effectiveTeacherId),
                                enrollments.get("id").in(enrollmentIds)
                        );
                    }
            );

            // 构建 enrollmentId → 上下文映射
            Map<Long, CalendarEntryContext> ctxMap = new HashMap<>();
            for (TeacherStudent ts : tsList) {
                Student student = ts.getStudent();
                String studentName = student != null ? student.getName() : "未知";
                if (ts.getEnrollments() != null) {
                    for (StudentEnrollment e : ts.getEnrollments()) {
                        if (enrollmentIds.contains(e.getId())) {
                            ctxMap.put(e.getId(), new CalendarEntryContext(
                                    ts.getStudentId(), studentName, e.getSubject()
                            ));
                        }
                    }
                }
            }

            for (StudentSession session : sessions) {
                String dateKey = session.getClassDate().toString();
                CalendarEntryContext ctx = ctxMap.get(session.getEnrollmentId());
                CalendarVO.CalendarEntry entry = CalendarVO.CalendarEntry.builder()
                        .studentId(ctx != null ? ctx.studentId : null)
                        .studentName(ctx != null ? ctx.studentName : "未知")
                        .subject(ctx != null ? ctx.subject : "未知")
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .build();
                datesMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(entry);
            }
        }

        return CalendarVO.builder().dates(datesMap).build();
    }

    // ==================== 内部辅助方法 ====================

    private record CalendarEntryContext(Long studentId, String studentName, String subject) {}

    /**
     * 构建动态查询条件（基于 TeacherStudent 表）
     */
    private Specification<TeacherStudent> buildSpecification(Long teacherId, String keyword, String subject, String grade) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("teacherId"), teacherId));

            // 关键词搜索：通过 student 关联查姓名
            if (keyword != null && !keyword.isBlank()) {
                Join<Object, Object> studentJoin = root.join("student", JoinType.LEFT);
                predicates.add(cb.like(studentJoin.get("name"), "%" + keyword + "%"));
            }

            // 年级筛选：通过 student 关联
            if (grade != null && !grade.isBlank()) {
                Join<Object, Object> studentJoin = root.join("student", JoinType.LEFT);
                predicates.add(cb.equal(studentJoin.get("grade"), grade));
            }

            // 科目筛选：通过 enrollments 关联
            if (subject != null && !subject.isBlank()) {
                Join<Object, Object> enrollments = root.join("enrollments", JoinType.INNER);
                predicates.add(cb.equal(enrollments.get("subject"), subject));
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 根据 DTO 构建报名科目和排课实体
     */
    private void buildEnrollments(TeacherStudent ts, List<EnrollmentDTO> enrollmentDTOs) {
        if (enrollmentDTOs == null || enrollmentDTOs.isEmpty()) {
            return;
        }
        for (EnrollmentDTO eDto : enrollmentDTOs) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setSubject(eDto.getSubject());
            enrollment.setSessions(new ArrayList<>());

            if (eDto.getSessions() != null) {
                for (SessionDTO sDto : eDto.getSessions()) {
                    StudentSession session = new StudentSession();
                    session.setClassDate(sDto.getClassDate());
                    session.setStartTime(sDto.getStartTime());
                    session.setEndTime(sDto.getEndTime());
                    enrollment.addSession(session);
                }
            }
            ts.addEnrollment(enrollment);
        }
    }

    /**
     * TeacherStudent → StudentVO
     */
    private StudentVO toStudentVO(TeacherStudent ts) {
        Student student = ts.getStudent();

        List<EnrollmentVO> enrollmentVOs = new ArrayList<>();
        if (ts.getEnrollments() != null) {
            for (StudentEnrollment enrollment : ts.getEnrollments()) {
                List<SessionVO> sessionVOs = new ArrayList<>();
                if (enrollment.getSessions() != null) {
                    for (StudentSession session : enrollment.getSessions()) {
                        sessionVOs.add(SessionVO.builder()
                                .id(session.getId())
                                .classDate(session.getClassDate())
                                .startTime(session.getStartTime())
                                .endTime(session.getEndTime())
                                .build());
                    }
                }
                enrollmentVOs.add(EnrollmentVO.builder()
                        .id(enrollment.getId())
                        .subject(enrollment.getSubject())
                        .sessions(sessionVOs)
                        .build());
            }
        }

        return StudentVO.builder()
                .id(ts.getId())
                .name(student != null ? student.getName() : null)
                .gender(student != null ? student.getGender() : null)
                .contact(student != null ? student.getContact() : null)
                .hoursLeft(ts.getHoursLeft())
                .grade(student != null ? student.getGrade() : null)
                .school(student != null ? student.getSchool() : null)
                .regDate(ts.getRegDate())
                .enrollments(enrollmentVOs)
                .build();
    }
}