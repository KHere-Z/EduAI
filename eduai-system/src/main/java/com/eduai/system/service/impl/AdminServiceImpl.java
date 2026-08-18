package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.ai.config.DeepSeekConfig;
import com.eduai.common.BusinessException;
import com.eduai.common.util.PasswordUtil;
import com.eduai.security.entity.Organization;
import com.eduai.security.entity.Teacher;
import com.eduai.security.entity.User;
import com.eduai.security.repository.OrganizationRepository;
import com.eduai.security.repository.TeacherRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.impl.AuthServiceImpl;
import com.eduai.system.dto.AdminSettingsDTO;
import com.eduai.system.dto.AdminStudentDTO;
import com.eduai.system.dto.AdminTeacherDTO;
import com.eduai.system.dto.AiModelDTO;
import com.eduai.system.entity.*;
import com.eduai.system.repository.*;
import com.eduai.system.service.AdminService;
import com.eduai.system.vo.*;
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
 * 管理员 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final OrganizationRepository organizationRepository;
    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final AiConfigRepository aiConfigRepository;
    private final AiModelRepository aiModelRepository;
    private final DeepSeekConfig deepSeekConfig;
    private final QuestionRepository questionRepository;

    // ==================== 权限检查 ====================

    /** 校验当前用户是否为管理员（roleType=1） */
    private void checkAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 1) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }

    /** 获取当前登录用户ID */
    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    // ==================== 学生管理 ====================

    @Override
    @Transactional(readOnly = true)
    public AdminStudentPageVO listStudents(int page, int pageSize, String keyword, String grade) {
        checkAdmin();

        Specification<Student> spec = buildStudentSpec(keyword, grade);
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Student> studentPage = studentRepository.findAll(spec, pageable);

        // 批量查询所有学生的老师关系
        Set<Long> studentIds = studentPage.getContent().stream()
                .map(Student::getId)
                .collect(Collectors.toSet());

        Map<Long, List<TeacherStudent>> tsMap = Collections.emptyMap();
        if (!studentIds.isEmpty()) {
            List<TeacherStudent> allTS = teacherStudentRepository.findAll(
                    (root, query, cb) -> root.get("studentId").in(studentIds)
            );
            tsMap = allTS.stream().collect(Collectors.groupingBy(TeacherStudent::getStudentId));
        }

        // 批量查询用户姓名
        Set<Long> teacherIds = tsMap.values().stream()
                .flatMap(List::stream)
                .map(TeacherStudent::getTeacherId)
                .collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = Collections.emptyMap();
        if (!teacherIds.isEmpty()) {
            teacherNameMap = userRepository.findAllById(teacherIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        }

        // 批量查询教师学科
        Map<Long, Teacher> teacherMap = Collections.emptyMap();
        if (!teacherIds.isEmpty()) {
            teacherMap = teacherRepository.findAll().stream()
                    .filter(t -> teacherIds.contains(t.getUserId()))
                    .collect(Collectors.toMap(Teacher::getUserId, t -> t));
        }

        final Map<Long, List<TeacherStudent>> finalTsMap = tsMap;
        final Map<Long, String> finalTeacherNameMap = teacherNameMap;
        final Map<Long, Teacher> finalTeacherMap = teacherMap;

        List<AdminStudentVO> list = studentPage.getContent().stream()
                .map(s -> toAdminStudentVO(s, finalTsMap.getOrDefault(s.getId(), List.of()),
                        finalTeacherNameMap, finalTeacherMap))
                .collect(Collectors.toList());

        return AdminStudentPageVO.builder()
                .list(list)
                .total(studentPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    @Transactional
    public AdminStudentVO createStudent(AdminStudentDTO dto) {
        checkAdmin();

        // 查找或创建 Student（按姓名+学校去重）
        Student student = studentRepository.findByNameAndSchool(dto.getName(), dto.getSchool())
                .orElseGet(() -> {
                    Student s = Student.builder()
                            .name(dto.getName())
                            .gender(dto.getGender())
                            .contact(dto.getContact())
                            .grade(dto.getGrade())
                            .school(dto.getSchool())
                            .build();
                    return studentRepository.save(s);
                });

        // 如果传了 username，同时创建学生用户账号（roleType=4）
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new BusinessException("用户名 " + dto.getUsername() + " 已存在");
            }
            User studentUser = User.builder()
                    .uid(AuthServiceImpl.generateUid())
                    .username(dto.getUsername())
                    .password(PasswordUtil.encode(dto.getPassword() != null ? dto.getPassword() : "123456"))
                    .realName(dto.getName())
                    .phone(dto.getContact())
                    .roleType(4)
                    .status(1)
                    .build();
            studentUser = userRepository.save(studentUser);
            student.setUserId(studentUser.getId());
            studentRepository.save(student);
            log.info("管理员{} 为学生{}创建用户账号: username={}", getCurrentUserId(), student.getId(), dto.getUsername());
        }

        // 遍历 teacherIds，为每个老师创建 teacher_student 记录
        List<TeacherStudent> tsList = new ArrayList<>();
        if (dto.getTeacherIds() != null && !dto.getTeacherIds().isEmpty()) {
            for (Long teacherId : dto.getTeacherIds()) {
                final Long tid = teacherId;
                // 检查是否已存在相同的老师-学生关系
                teacherStudentRepository.findByTeacherIdAndStudentId(tid, student.getId())
                        .ifPresentOrElse(
                                tsList::add,
                                () -> {
                                    TeacherStudent ts = new TeacherStudent();
                                    ts.setTeacherId(tid);
                                    ts.setStudentId(student.getId());
                                    ts.setHoursLeft(0);
                                    ts.setRegDate(java.time.LocalDate.now());
                                    ts.setEnrollments(new ArrayList<>());
                                    tsList.add(teacherStudentRepository.save(ts));
                                }
                        );
            }
        }

        log.info("管理员{} 创建学生: id={}, name={}, teacherIds={}",
                getCurrentUserId(), student.getId(), student.getName(), dto.getTeacherIds());

        // 查询老师姓名和学科用于 VO
        Set<Long> allTeacherIds = tsList.stream().map(TeacherStudent::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = userRepository.findAllById(allTeacherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        Map<Long, Teacher> teacherMap = teacherRepository.findAll().stream()
                .filter(t -> allTeacherIds.contains(t.getUserId()))
                .collect(Collectors.toMap(Teacher::getUserId, t -> t));

        return toAdminStudentVO(student, tsList, teacherNameMap, teacherMap);
    }

    @Override
    @Transactional
    public AdminStudentVO updateStudent(Long id, AdminStudentDTO dto) {
        checkAdmin();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        // 更新 Student 基本信息
        student.setName(dto.getName());
        student.setGender(dto.getGender());
        student.setContact(dto.getContact());
        student.setGrade(dto.getGrade());
        student.setSchool(dto.getSchool());
        studentRepository.save(student);

        // 处理 teacherIds：先删旧的 teacher_student WHERE student_id=?，再插入新的
        if (dto.getTeacherIds() != null) {
            List<TeacherStudent> oldTSList = teacherStudentRepository.findByStudentId(id);
            if (!oldTSList.isEmpty()) {
                teacherStudentRepository.deleteAll(oldTSList);
                // flush 确保 DELETE 先执行，避免 INSERT 时违反唯一约束
                teacherStudentRepository.flush();
            }

            for (Long teacherId : dto.getTeacherIds()) {
                TeacherStudent ts = new TeacherStudent();
                ts.setTeacherId(teacherId);
                ts.setStudentId(student.getId());
                ts.setHoursLeft(0);
                ts.setRegDate(java.time.LocalDate.now());
                ts.setEnrollments(new ArrayList<>());
                teacherStudentRepository.save(ts);
            }
        }

        // 查询更新后的关联老师信息
        List<TeacherStudent> tsList = teacherStudentRepository.findByStudentId(id);
        Set<Long> teacherIds = tsList.stream().map(TeacherStudent::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = userRepository.findAllById(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        Map<Long, Teacher> teacherMap = teacherRepository.findAll().stream()
                .filter(t -> teacherIds.contains(t.getUserId()))
                .collect(Collectors.toMap(Teacher::getUserId, t -> t));

        log.info("管理员{} 更新学生: id={}, teacherIds={}", getCurrentUserId(), id, dto.getTeacherIds());
        return toAdminStudentVO(student, tsList, teacherNameMap, teacherMap);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        checkAdmin();

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学生不存在"));

        // 级联删除：错题 → teacher_student
        questionRepository.deleteByStudentId(id);
        List<TeacherStudent> tsList = teacherStudentRepository.findByStudentId(id);
        if (!tsList.isEmpty()) {
            teacherStudentRepository.deleteAll(tsList);
        }

        studentRepository.delete(student);
        log.info("管理员{} 删除学生: id={}, name={}", getCurrentUserId(), id, student.getName());
    }

    // ==================== 老师管理 ====================

    @Override
    @Transactional(readOnly = true)
    public AdminTeacherPageVO listTeachers(int page, int pageSize, String keyword) {
        checkAdmin();

        // 查询所有教师用户（roleType=3）
        List<User> teacherUsers = userRepository.findByRoleType(3);

        // 关键词过滤
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            teacherUsers = teacherUsers.stream()
                    .filter(u -> (u.getRealName() != null && u.getRealName().contains(kw))
                            || u.getUsername().contains(kw)
                            || (u.getPhone() != null && u.getPhone().contains(kw)))
                    .collect(Collectors.toList());
        }

        long total = teacherUsers.size();

        // 分页
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, teacherUsers.size());
        if (fromIndex >= teacherUsers.size()) {
            return AdminTeacherPageVO.builder()
                    .list(List.of()).total(total).page(page).pageSize(pageSize).build();
        }
        List<User> pagedUsers = teacherUsers.subList(fromIndex, toIndex);

        // 批量查询 Teachers
        Set<Long> userIds = pagedUsers.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Teacher> teacherMap = teacherRepository.findAll().stream()
                .filter(t -> userIds.contains(t.getUserId()))
                .collect(Collectors.toMap(Teacher::getUserId, t -> t));

        // 批量查询机构名称
        Set<Long> orgIds = teacherMap.values().stream()
                .map(Teacher::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Map<Long, String> orgNameMap = orgIds.isEmpty()
                ? Collections.emptyMap()
                : organizationRepository.findAllById(orgIds).stream()
                        .collect(Collectors.toMap(
                                org -> org.getId(),
                                org -> org.getName(),
                                (a, b) -> a));

        List<AdminTeacherVO> list = pagedUsers.stream()
                .map(u -> {
                    Teacher t = teacherMap.get(u.getId());
                    long studentCount = teacherStudentRepository.countStudentsByTeacherId(u.getId());
                    long totalHours = teacherStudentRepository.sumHoursByTeacherId(u.getId());

                    List<String> subjects = t != null ? parseSubjectIds(t.getSubjectIds()) : List.of();
                    String orgName = (t != null && t.getOrgId() != null) ? orgNameMap.getOrDefault(t.getOrgId(), "") : "";

                    return AdminTeacherVO.builder()
                            .id(t != null ? t.getId() : null)
                            .userId(u.getId())
                            .username(u.getUsername())
                            .realName(u.getRealName())
                            .phone(u.getPhone())
                            .email(u.getEmail())
                            .subjects(subjects)
                            .title(t != null ? t.getTitle() : null)
                            .orgName(orgName)
                            .avatar(t != null ? t.getAvatar() : null)
                            .status(u.getStatus())
                            .studentCount(studentCount)
                            .totalHours(totalHours)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminTeacherPageVO.builder()
                .list(list).total(total).page(page).pageSize(pageSize).build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTeacherDetailVO getTeacherDetail(Long teacherId) {
        checkAdmin();

        // teacherId 这里实际是 userId（老师对应的 users.id）
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(404, "教师不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(400, "该用户不是教师");
        }

        Teacher teacher = teacherRepository.findByUserId(teacherId).orElse(null);
        long studentCount = teacherStudentRepository.countStudentsByTeacherId(teacherId);
        long totalHours = teacherStudentRepository.sumHoursByTeacherId(teacherId);

        List<String> subjects = teacher != null ? parseSubjectIds(teacher.getSubjectIds()) : List.of();
        String orgName = "";
        if (teacher != null && teacher.getOrgId() != null) {
            orgName = organizationRepository.findById(teacher.getOrgId())
                    .map(org -> org.getName())
                    .orElse("");
        }

        // 查询该老师的学生列表
        List<TeacherStudent> tsList = teacherStudentRepository.findByTeacherId(teacherId);
        List<AdminTeacherDetailVO.TeacherStudentItem> studentItems = tsList.stream()
                .map(ts -> {
                    Student s = ts.getStudent();
                    List<String> enrolledSubjects = List.of();
                    if (ts.getEnrollments() != null) {
                        enrolledSubjects = ts.getEnrollments().stream()
                                .map(StudentEnrollment::getSubject)
                                .distinct()
                                .collect(Collectors.toList());
                    }
                    return AdminTeacherDetailVO.TeacherStudentItem.builder()
                            .tsId(ts.getId())
                            .studentId(ts.getStudentId())
                            .studentName(s != null ? s.getName() : "未知")
                            .gender(s != null ? s.getGender() : null)
                            .grade(s != null ? s.getGrade() : null)
                            .school(s != null ? s.getSchool() : null)
                            .hoursLeft(ts.getHoursLeft())
                            .subjects(enrolledSubjects)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminTeacherDetailVO.builder()
                .id(teacher != null ? teacher.getId() : null)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .subjects(subjects)
                .title(teacher != null ? teacher.getTitle() : null)
                .orgName(orgName)
                .avatar(teacher != null ? teacher.getAvatar() : null)
                .bio(teacher != null ? teacher.getBio() : null)
                .status(user.getStatus())
                .studentCount(studentCount)
                .totalHours(totalHours)
                .students(studentItems)
                .build();
    }

    @Override
    @Transactional
    public AdminTeacherVO createTeacher(AdminTeacherDTO dto) {
        checkAdmin();

        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 创建 User
        User user = User.builder()
                .uid(AuthServiceImpl.generateUid())
                .username(dto.getUsername())
                .password(PasswordUtil.encode(dto.getPassword()))
                .realName(dto.getRealName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .roleType(3)
                .status(1)
                .build();
        user = userRepository.save(user);

        // 创建 Teacher
        Long resolvedOrgId = resolveOrgId(dto);
        String subjectIdsStr = (dto.getSubjectIds() != null && !dto.getSubjectIds().isEmpty())
                ? String.join(",", dto.getSubjectIds()) : "";
        Teacher teacher = Teacher.builder()
                .userId(user.getId())
                .subjectIds(subjectIdsStr)
                .orgId(resolvedOrgId)
                .title(dto.getTitle())
                .build();
        teacher = teacherRepository.save(teacher);

        log.info("管理员{} 创建老师: userId={}, realName={}, subjects={}",
                getCurrentUserId(), user.getId(), user.getRealName(), subjectIdsStr);

        return toAdminTeacherVO(user, teacher);
    }

    @Override
    @Transactional
    public AdminTeacherVO updateTeacher(Long userId, AdminTeacherDTO dto) {
        checkAdmin();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "教师不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(400, "该用户不是教师");
        }

        // 更新 User
        user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(PasswordUtil.encode(dto.getPassword()));
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && !dto.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(dto.getUsername());
        }
        userRepository.save(user);

        // 更新 Teacher
        Long resolvedOrgId = resolveOrgId(dto);
        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
        boolean hasOrgChange = resolvedOrgId != null;
        if (teacher == null && (dto.getSubjectIds() != null || dto.getTitle() != null || hasOrgChange)) {
            // 创建 Teacher 记录（如果之前不存在）
            String subjectIdsStr = (dto.getSubjectIds() != null && !dto.getSubjectIds().isEmpty())
                    ? String.join(",", dto.getSubjectIds()) : "";
            teacher = Teacher.builder()
                    .userId(user.getId())
                    .subjectIds(subjectIdsStr)
                    .orgId(resolvedOrgId)
                    .title(dto.getTitle())
                    .build();
            teacher = teacherRepository.save(teacher);
        } else if (teacher != null) {
            if (dto.getSubjectIds() != null) {
                teacher.setSubjectIds(String.join(",", dto.getSubjectIds()));
            }
            if (dto.getTitle() != null) teacher.setTitle(dto.getTitle());
            if (resolvedOrgId != null) teacher.setOrgId(resolvedOrgId);
            teacherRepository.save(teacher);
        }

        log.info("管理员{} 更新老师: userId={}", getCurrentUserId(), userId);
        return toAdminTeacherVO(user, teacher);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long userId) {
        checkAdmin();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "教师不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(400, "该用户不是教师");
        }

        // 删除该老师的所有 teacher_student 关系（级联 enrollment → session）
        List<TeacherStudent> tsList = teacherStudentRepository.findByTeacherId(userId);
        if (!tsList.isEmpty()) {
            teacherStudentRepository.deleteAll(tsList);
        }

        // 删除 Teacher 记录
        teacherRepository.findByUserId(userId).ifPresent(teacherRepository::delete);

        // 删除 User 记录
        userRepository.delete(user);

        log.info("管理员{} 删除老师: userId={}, realName={}", getCurrentUserId(), userId, user.getRealName());
    }

    // ==================== 排课查看 ====================

    @Override
    @Transactional(readOnly = true)
    public AdminScheduleVO listSchedules(Long teacherId, Integer year, Integer month, int page, int pageSize) {
        checkAdmin();

        log.info("[排课查询] teacherId={} year={} month={} page={} pageSize={}",
                teacherId, year, month, page, pageSize);

        List<StudentSession> sessions;
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            log.info("[排课查询] 日期范围: {} ~ {}", start, end);

            if (teacherId != null) {
                sessions = studentSessionRepository.findAllSessionsByTeacherAndDateRange(teacherId, start, end);
            } else {
                sessions = studentSessionRepository.findAllSessionsByDateRange(start, end);
            }
        } else {
            sessions = studentSessionRepository.findAllSessionsWithDetails();
            if (teacherId != null) {
                sessions = sessions.stream()
                        .filter(s -> {
                            // 通过关联查询 teacherId
                            StudentEnrollment e = s.getEnrollment();
                            if (e == null) return false;
                            TeacherStudent ts = e.getTeacherStudent();
                            return ts != null && ts.getTeacherId().equals(teacherId);
                        })
                        .collect(Collectors.toList());
            }
        }

        log.info("[排课查询] 结果: {} 条", sessions.size());

        // 批量查询关联信息
        Set<Long> enrollmentIds = sessions.stream()
                .map(StudentSession::getEnrollmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<Long, StudentEnrollment> enrollmentMap;
        final Map<Long, TeacherStudent> tsMap;
        if (!enrollmentIds.isEmpty()) {
            enrollmentMap = studentEnrollmentRepository.findAllById(enrollmentIds).stream()
                    .collect(Collectors.toMap(StudentEnrollment::getId, e -> e));

            Set<Long> tsIds = enrollmentMap.values().stream()
                    .map(StudentEnrollment::getTeacherStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            tsMap = !tsIds.isEmpty()
                    ? teacherStudentRepository.findAllById(tsIds).stream()
                            .collect(Collectors.toMap(TeacherStudent::getId, ts -> ts))
                    : Collections.emptyMap();
        } else {
            enrollmentMap = Collections.emptyMap();
            tsMap = Collections.emptyMap();
        }

        Set<Long> allTeacherIds = tsMap.values().stream()
                .map(TeacherStudent::getTeacherId)
                .collect(Collectors.toSet());
        Set<Long> allStudentIds = tsMap.values().stream()
                .map(TeacherStudent::getStudentId)
                .collect(Collectors.toSet());

        final Map<Long, String> teacherNameMap = allTeacherIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(allTeacherIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        final Map<Long, String> studentNameMap = allStudentIds.isEmpty()
                ? Collections.emptyMap()
                : studentRepository.findAllById(allStudentIds).stream()
                        .collect(Collectors.toMap(Student::getId, Student::getName));

        // 分页
        long total = sessions.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, sessions.size());
        if (fromIndex >= sessions.size()) {
            return AdminScheduleVO.builder().list(List.of()).build();
        }
        List<StudentSession> pagedSessions = sessions.subList(fromIndex, toIndex);

        List<AdminScheduleVO.ScheduleItem> items = pagedSessions.stream()
                .map(s -> {
                    StudentEnrollment e = enrollmentMap.get(s.getEnrollmentId());
                    TeacherStudent ts = e != null ? tsMap.get(e.getTeacherStudentId()) : null;
                    String teacherName = ts != null ? teacherNameMap.getOrDefault(ts.getTeacherId(), "未知") : "未知";
                    String studentName = ts != null ? studentNameMap.getOrDefault(ts.getStudentId(), "未知") : "未知";

                    return AdminScheduleVO.ScheduleItem.builder()
                            .sessionId(s.getId())
                            .classDate(s.getClassDate().toString())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .subject(e != null ? e.getSubject() : "未知")
                            .teacherId(ts != null ? ts.getTeacherId() : null)
                            .teacherName(teacherName)
                            .studentId(ts != null ? ts.getStudentId() : null)
                            .studentName(studentName)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminScheduleVO.builder()
                .list(items).total(total).page(page).pageSize(pageSize).build();
    }

    // ==================== 概览统计 ====================

    @Override
    @Transactional(readOnly = true)
    public AdminStatsVO getStats() {
        checkAdmin();

        long teacherCount = userRepository.countByRoleType(3);
        long studentCount = studentRepository.countTotal();
        long totalHours = teacherStudentRepository.sumTotalHours();
        long relationCount = teacherStudentRepository.countTotal();
        long sessionCount = studentSessionRepository.countTotal();
        long enrollmentCount = studentEnrollmentRepository.countTotal();

        return AdminStatsVO.builder()
                .teacherCount(teacherCount)
                .studentCount(studentCount)
                .totalHours(totalHours)
                .relationCount(relationCount)
                .sessionCount(sessionCount)
                .enrollmentCount(enrollmentCount)
                .build();
    }

    // ==================== 系统设置 ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSettings() {
        checkAdmin();
        // 预加载 ai_models → value→name 映射
        Map<String, String> modelNameMap = aiModelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        m -> m.getValue(),
                        m -> m.getName() != null ? m.getName() : m.getValue(),
                        (a, b) -> a));

        // 返回 camelCase 格式，与 AdminSettingsDTO 保持一致
        Map<String, Object> map = new LinkedHashMap<>();
        for (AiConfig cfg : aiConfigRepository.findAll()) {
            String camelKey = snakeToCamel(cfg.getModule()) + "Model";
            map.put(camelKey, cfg.getModel());
            // 同时返回模型显示名，方便前端回显
            String nameKey = snakeToCamel(cfg.getModule()) + "ModelName";
            map.put(nameKey, modelNameMap.getOrDefault(cfg.getModel(), ""));
        }
        return map;
    }

    /** wrong_analysis → wrongAnalysis */
    private String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder(snake.length());
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public Map<String, Object> updateSettings(AdminSettingsDTO dto) {
        checkAdmin();
        if (dto.getWrongAnalysisModel() != null) {
            upsertModule("wrong_analysis", dto.getWrongAnalysisModel());
        }
        if (dto.getExamAnalysisModel() != null) {
            upsertModule("exam_analysis", dto.getExamAnalysisModel());
        }
        aiConfigRepository.flush();
        deepSeekConfig.refresh();
        return getSettings();
    }

    private void upsertModule(String module, String model) {
        AiConfig cfg = aiConfigRepository.findByModule(module).orElseGet(() -> AiConfig.builder().module(module).build());
        if (model != null) cfg.setModel(model);
        aiConfigRepository.save(cfg);
    }

    // ==================== AI 模型管理 ====================

    @Override
    @Transactional(readOnly = true)
    public List<AiModelVO> listModels() {
        checkAdmin();
        return aiModelRepository.findAll().stream()
                .map(this::toAiModelVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AiModelVO createModel(AiModelDTO dto) {
        checkAdmin();

        if (aiModelRepository.existsByValue(dto.getValue())) {
            throw new BusinessException("模型标识 " + dto.getValue() + " 已存在");
        }

        AiModel model = AiModel.builder()
                .value(dto.getValue())
                .name(dto.getName())
                .description(dto.getDescription())
                .provider(dto.getProvider())
                .apiUrl(dto.getApiUrl())
                .apiKey(dto.getApiKey())
                .tag(dto.getTag())
                .build();

        model = aiModelRepository.save(model);
        log.info("管理员{} 新增AI模型: id={}, value={}, name={}", getCurrentUserId(), model.getId(), model.getValue(), model.getName());
        return toAiModelVO(model);
    }

    @Override
    @Transactional
    public AiModelVO updateModel(Long id, AiModelDTO dto) {
        checkAdmin();

        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模型不存在"));

        // 检查 value 唯一性（改了 value 的情况）
        if (!model.getValue().equals(dto.getValue())
                && aiModelRepository.existsByValue(dto.getValue())) {
            throw new BusinessException("模型标识 " + dto.getValue() + " 已存在");
        }

        model.setValue(dto.getValue());
        model.setName(dto.getName());
        model.setDescription(dto.getDescription());
        model.setProvider(dto.getProvider());
        model.setApiUrl(dto.getApiUrl());
        model.setApiKey(dto.getApiKey());
        model.setTag(dto.getTag());

        model = aiModelRepository.save(model);
        log.info("管理员{} 更新AI模型: id={}, value={}", getCurrentUserId(), id, dto.getValue());
        return toAiModelVO(model);
    }

    @Override
    @Transactional
    public void deleteModel(Long id) {
        checkAdmin();

        AiModel model = aiModelRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模型不存在"));

        // 检查是否有 ai_config 引用此模型
        List<AiConfig> refs = aiConfigRepository.findAll().stream()
                .filter(c -> model.getValue().equals(c.getModel()))
                .collect(Collectors.toList());
        if (!refs.isEmpty()) {
            String modules = refs.stream().map(AiConfig::getModule).collect(Collectors.joining(", "));
            throw new BusinessException("该模型正被以下功能使用，请先切换到其他模型: " + modules);
        }

        aiModelRepository.delete(model);
        log.info("管理员{} 删除AI模型: id={}, value={}, name={}", getCurrentUserId(), id, model.getValue(), model.getName());
    }

    // ==================== 内部辅助方法 ====================

    /** 构建学生动态查询条件 */
    private Specification<Student> buildStudentSpec(String keyword, String grade) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("school"), pattern)
                ));
            }
            if (grade != null && !grade.isBlank()) {
                predicates.add(cb.equal(root.get("grade"), grade));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Student → AdminStudentVO（含教师关系） */
    private AdminStudentVO toAdminStudentVO(Student student,
                                            List<TeacherStudent> tsList,
                                            Map<Long, String> teacherNameMap,
                                            Map<Long, Teacher> teacherMap) {
        List<AdminStudentVO.TeacherRelation> relations = tsList.stream()
                .map(ts -> {
                    Teacher t = teacherMap.get(ts.getTeacherId());
                    List<String> subjs = t != null ? parseSubjectIds(t.getSubjectIds()) : List.of();
                    // 过滤只显示该学生报的科目
                    if (ts.getEnrollments() != null && !ts.getEnrollments().isEmpty()) {
                        subjs = ts.getEnrollments().stream()
                                .map(StudentEnrollment::getSubject)
                                .distinct()
                                .collect(Collectors.toList());
                    }
                    return AdminStudentVO.TeacherRelation.builder()
                            .teacherId(ts.getTeacherId())
                            .teacherName(teacherNameMap.getOrDefault(ts.getTeacherId(), "未知"))
                            .subjects(subjs)
                            .hoursLeft(ts.getHoursLeft())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminStudentVO.builder()
                .id(student.getId())
                .name(student.getName())
                .gender(student.getGender())
                .contact(student.getContact())
                .grade(student.getGrade())
                .school(student.getSchool())
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .teacherRelations(relations)
                .build();
    }

    /** User + Teacher → AdminTeacherVO */
    private AdminTeacherVO toAdminTeacherVO(User user, Teacher teacher) {
        long studentCount = teacherStudentRepository.countStudentsByTeacherId(user.getId());
        long totalHours = teacherStudentRepository.sumHoursByTeacherId(user.getId());

        List<String> subjects = teacher != null ? parseSubjectIds(teacher.getSubjectIds()) : List.of();
        String orgName = "";
        if (teacher != null && teacher.getOrgId() != null) {
            orgName = organizationRepository.findById(teacher.getOrgId())
                    .map(org -> org.getName())
                    .orElse("");
        }

        return AdminTeacherVO.builder()
                .id(teacher != null ? teacher.getId() : null)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .subjects(subjects)
                .title(teacher != null ? teacher.getTitle() : null)
                .orgName(orgName)
                .avatar(teacher != null ? teacher.getAvatar() : null)
                .status(user.getStatus())
                .studentCount(studentCount)
                .totalHours(totalHours)
                .build();
    }

    /**
     * 解析机构：优先用 orgId；如果传了 orgName，按名称查找/创建 Organization
     */
    private Long resolveOrgId(AdminTeacherDTO dto) {
        if (dto.getOrgName() != null && !dto.getOrgName().isBlank()) {
            Organization org = organizationRepository.findByName(dto.getOrgName())
                    .orElseGet(() -> {
                        Organization newOrg = Organization.builder()
                                .name(dto.getOrgName())
                                .type(1)
                                .status(1)
                                .build();
                        return organizationRepository.save(newOrg);
                    });
            return org.getId();
        }
        return dto.getOrgId();
    }

    /** AiModel → AiModelVO */
    private AiModelVO toAiModelVO(AiModel m) {
        return AiModelVO.builder()
                .id(m.getId())
                .value(m.getValue())
                .name(m.getName())
                .description(m.getDescription())
                .provider(m.getProvider())
                .apiUrl(m.getApiUrl())
                .apiKey(m.getApiKey())
                .tag(m.getTag())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    /** "math,physics" → ["math", "physics"] */
    private List<String> parseSubjectIds(String subjectIds) {
        if (subjectIds == null || subjectIds.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(subjectIds.split(","));
    }
}