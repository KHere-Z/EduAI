package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.Teacher;
import com.eduai.security.entity.User;
import com.eduai.security.repository.TeacherRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.RescheduleDTO;
import com.eduai.system.entity.*;
import com.eduai.system.repository.*;
import com.eduai.system.service.StudentPortalService;
import com.eduai.system.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentPortalServiceImpl implements StudentPortalService {

    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final StudentCheckinRepository studentCheckinRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    /** 从 token 中获取当前学生 */
    private Student getCurrentStudent() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 4) {
            throw new BusinessException(403, "仅学生可访问");
        }
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "未找到学生档案，请联系管理员绑定账号"));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentEnrollmentVO getEnrollments() {
        Student student = getCurrentStudent();

        List<TeacherStudent> tsList = teacherStudentRepository.findByStudentId(student.getId());

        Set<Long> teacherIds = tsList.stream().map(TeacherStudent::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = userRepository.findAllById(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        List<StudentEnrollmentVO.EnrolledCourse> courses = tsList.stream()
                .flatMap(ts -> {
                    if (ts.getEnrollments() == null || ts.getEnrollments().isEmpty()) return java.util.stream.Stream.empty();
                    String teacherName = teacherNameMap.getOrDefault(ts.getTeacherId(), "未知");
                    return ts.getEnrollments().stream().map(e -> {
                        List<SessionVO> upcoming = List.of();
                        if (e.getSessions() != null) {
                            LocalDate today = LocalDate.now();
                            upcoming = e.getSessions().stream()
                                    .filter(s -> !s.getClassDate().isBefore(today))
                                    .sorted(Comparator.comparing(StudentSession::getClassDate)
                                            .thenComparing(StudentSession::getStartTime))
                                    .limit(5)
                                    .map(s -> SessionVO.builder()
                                            .id(s.getId())
                                            .classDate(s.getClassDate())
                                            .startTime(s.getStartTime())
                                            .endTime(s.getEndTime())
                                            .build())
                                    .collect(Collectors.toList());
                        }
                        return StudentEnrollmentVO.EnrolledCourse.builder()
                                .tsId(ts.getId())
                                .subject(e.getSubject())
                                .teacherName(teacherName)
                                .teacherId(ts.getTeacherId())
                                .hoursLeft(ts.getHoursLeft())
                                .upcomingSessions(upcoming)
                                .build();
                    });
                })
                .collect(Collectors.toList());

        return StudentEnrollmentVO.builder().courses(courses).build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentScheduleVO getSchedule(Integer year, Integer month) {
        Student student = getCurrentStudent();
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<StudentSession> sessions = studentSessionRepository
                .findSessionsByStudentAndDateRange(student.getId(), start, end);

        Set<Long> enrollmentIds = sessions.stream()
                .map(StudentSession::getEnrollmentId).filter(Objects::nonNull).collect(Collectors.toSet());

        // enrollmentId → {subject, teacherId, teacherName}
        Map<Long, String[]> ctxMap = new HashMap<>();
        if (!enrollmentIds.isEmpty()) {
            Set<Long> allTeacherIds = new HashSet<>();
            // Query enrollments to get subject + teacherStudentId
            studentEnrollmentRepository.findAllById(enrollmentIds).forEach(e -> {
                allTeacherIds.add(e.getTeacherStudentId());
                ctxMap.put(e.getId(), new String[]{e.getSubject(), String.valueOf(e.getTeacherStudentId())});
            });
            // Resolve teacherStudentId → teacherId + teacherName
            if (!allTeacherIds.isEmpty()) {
                teacherStudentRepository.findAllById(allTeacherIds).forEach(ts -> {
                    allTeacherIds.add(ts.getTeacherId());
                    // Update entries: replace teacherStudentId with teacherId
                    for (Map.Entry<Long, String[]> entry : ctxMap.entrySet()) {
                        if (entry.getValue()[1].equals(String.valueOf(ts.getId()))) {
                            entry.getValue()[1] = String.valueOf(ts.getTeacherId());
                        }
                    }
                });
            }
            Map<Long, String> nameMap = userRepository.findAllById(allTeacherIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            for (Map.Entry<Long, String[]> entry : ctxMap.entrySet()) {
                try {
                    Long tid = Long.valueOf(entry.getValue()[1]);
                    entry.getValue()[1] = nameMap.getOrDefault(tid, "未知");
                } catch (Exception ignored) {}
            }
        }

        List<StudentScheduleVO.ScheduleItem> items = sessions.stream()
                .map(s -> {
                    String[] ctx = ctxMap.getOrDefault(s.getEnrollmentId(), new String[]{"未知", "未知"});
                    Long tid = null;
                    try { tid = Long.valueOf(ctx[1]); } catch (Exception ignored) {}
                    return StudentScheduleVO.ScheduleItem.builder()
                            .sessionId(s.getId())
                            .classDate(s.getClassDate().toString())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .subject(ctx[0])
                            .teacherName(ctx[1])
                            .teacherId(tid)
                            .build();
                })
                .sorted(Comparator.comparing(StudentScheduleVO.ScheduleItem::getClassDate)
                        .thenComparing(StudentScheduleVO.ScheduleItem::getStartTime))
                .collect(Collectors.toList());

        return StudentScheduleVO.builder().schedules(items).build();
    }

    @Override
    @Transactional
    public RescheduleVO submitReschedule(RescheduleDTO dto) {
        Student student = getCurrentStudent();

        // 查找原 session
        StudentSession session = studentSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new BusinessException(404, "排课记录不存在"));

        // 查找关联的 enrollment → teacher_student
        StudentEnrollment enrollment = session.getEnrollment();
        if (enrollment == null) throw new BusinessException(400, "排课记录异常");
        TeacherStudent ts = enrollment.getTeacherStudent();
        if (ts == null || !ts.getStudentId().equals(student.getId())) {
            throw new BusinessException(403, "无权调此课程");
        }

        String teacherName = userRepository.findById(ts.getTeacherId())
                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
                .orElse("未知");

        RescheduleRequest req = RescheduleRequest.builder()
                .sessionId(dto.getSessionId())
                .studentId(student.getId())
                .teacherId(ts.getTeacherId())
                .originalDate(session.getClassDate())
                .originalStart(session.getStartTime())
                .originalEnd(session.getEndTime())
                .requestedDate(dto.getRequestedDate())
                .requestedStart(dto.getRequestedStart())
                .requestedEnd(dto.getRequestedEnd())
                .reason(dto.getReason())
                .subject(enrollment.getSubject())
                .status("pending")
                .build();
        req = rescheduleRequestRepository.save(req);

        log.info("学生{} 提交调课申请: sessionId={}, {}→{}", student.getId(), dto.getSessionId(),
                session.getClassDate(), dto.getRequestedDate());

        return RescheduleVO.builder()
                .id(req.getId()).sessionId(req.getSessionId())
                .studentId(student.getId()).studentName(student.getName())
                .teacherId(req.getTeacherId()).teacherName(teacherName)
                .subject(req.getSubject())
                .originalDate(req.getOriginalDate()).originalStart(req.getOriginalStart()).originalEnd(req.getOriginalEnd())
                .requestedDate(req.getRequestedDate()).requestedStart(req.getRequestedStart()).requestedEnd(req.getRequestedEnd())
                .reason(req.getReason()).status(req.getStatus()).createdAt(req.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void checkin() {
        Student student = getCurrentStudent();
        LocalDate today = LocalDate.now();

        if (studentCheckinRepository.findByStudentIdAndCheckinDate(student.getId(), today).isPresent()) {
            throw new BusinessException("今日已打卡");
        }

        StudentCheckin checkin = StudentCheckin.builder()
                .studentId(student.getId())
                .checkinDate(today)
                .build();
        studentCheckinRepository.save(checkin);
        log.info("学生{} 打卡: date={}", student.getId(), today);
    }

    @Override
    @Transactional(readOnly = true)
    public StreakVO getStreak() {
        Student student = getCurrentStudent();
        LocalDate today = LocalDate.now();

        List<StudentCheckin> checkins = studentCheckinRepository
                .findByStudentIdOrderByCheckinDateDesc(student.getId());

        boolean checkedInToday = !checkins.isEmpty() && checkins.get(0).getCheckinDate().equals(today);

        // 计算连续天数
        int streak = 0;
        LocalDate expected = today;
        for (StudentCheckin c : checkins) {
            if (c.getCheckinDate().equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (c.getCheckinDate().equals(expected.plusDays(1))) {
                // skip gaps (if student checked in today but missed yesterday, streak starts from today)
                break;
            } else {
                break;
            }
        }

        return StreakVO.builder()
                .streak(streak)
                .totalDays(checkins.size())
                .checkedInToday(checkedInToday)
                .build();
    }
}