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

    /**
     * ⛔ 已关闭：老师端不再支持直接添加学生。
     * <p>
     * 原实现是 {@code findByNameAndSchool(name, school)} 按「姓名+学校」去重，但学生
     * 自助注册时 {@code students.school} 为 NULL，而 SQL 里 {@code school = NULL} 恒假
     * （{@code NULL = NULL} 为 unknown）→ 永远匹配不上 → 为已注册学生<b>新建一条重复
     * 空档案</b> → teacher_student 指向新档案 → 老师在自己的试卷页<b>一条试卷都看不到</b>
     * （学生此前存下的试卷、错题全挂在旧档案上）。
     * <p>
     * 本表单没有 UID 字段（前端 payload 只带 name/gender/contact/grade/school 等），
     * 后端 {@link StudentDTO} 也没有，所以这条路<b>没有任何能精确指到账号的键</b>，
     * 只能靠姓名猜：猜不中 → 建重复档案；猜错 → 老师看到<b>别的学生</b>的试卷。
     * 两条都不可接受。
     * <p>
     * <b>唯一入口改为</b>：学生在「个人中心」输入老师的 UID 发起关联请求，老师同意后由
     * {@code RelationServiceImpl.syncToTeacherStudent} 建 teacher_student —— 它本来就按
     * {@code user_id} 精确查，天然命中注册时建的那条档案。
     * <p>
     * <b>为什么抛业务异常而不是删掉本方法</b>：老师浏览器可能缓存着旧前端、按钮还在。
     * 抛业务异常给出的是可执行指引；删掉方法会变成 404 网络错误，而若放任不管，
     * 旧缓存会<b>静默</b>建出重复空档案，又回到「老师收不到试卷」的状态。
     * <p>
     * {@code update} / {@code getById} / 排课 / 课时调整<b>全部保留</b> —— 老师仍要在
     * 学生管理页排课、调课时、看错题。
     */
    @Override
    @Transactional
    public StudentVO create(StudentDTO dto) {
        throw new BusinessException(
                "老师端不再支持直接添加学生。请让学生在「个人中心」输入您的 UID 发起关联请求，您同意后学生即出现在列表中");
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
            // 判空写入：注册时已把手机号写进 students.contact（AuthServiceImpl.createStudentProfile），
            // 无条件覆盖会让「没填联系方式」的一次编辑把手机号永久抹成 NULL —— 不可逆。
            // 代价：老师无法再通过清空表单来清除 contact。
            if (dto.getContact() != null) student.setContact(dto.getContact());
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