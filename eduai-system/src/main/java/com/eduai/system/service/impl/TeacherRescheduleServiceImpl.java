package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.entity.RescheduleRequest;
import com.eduai.system.entity.Student;
import com.eduai.system.entity.StudentSession;
import com.eduai.system.repository.RescheduleRequestRepository;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.repository.StudentSessionRepository;
import com.eduai.system.service.TeacherRescheduleService;
import com.eduai.system.vo.RescheduleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 老师端 - 调课管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherRescheduleServiceImpl implements TeacherRescheduleService {

    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    private Long getCurrentTeacherId() {
        return StpUtil.getLoginIdAsLong();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleVO> listReschedules() {
        Long teacherId = getCurrentTeacherId();
        List<RescheduleRequest> requests = rescheduleRequestRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        Set<Long> studentIds = requests.stream().map(RescheduleRequest::getStudentId).collect(Collectors.toSet());
        Map<Long, String> studentNameMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Student::getName));
        Map<Long, String> teacherNameMap = userRepository.findAllById(Set.of(teacherId)).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        return requests.stream()
                .map(r -> RescheduleVO.builder()
                        .id(r.getId()).sessionId(r.getSessionId())
                        .studentId(r.getStudentId()).studentName(studentNameMap.getOrDefault(r.getStudentId(), "未知"))
                        .teacherId(r.getTeacherId()).teacherName(teacherNameMap.getOrDefault(r.getTeacherId(), ""))
                        .subject(r.getSubject())
                        .originalDate(r.getOriginalDate()).originalStart(r.getOriginalStart()).originalEnd(r.getOriginalEnd())
                        .requestedDate(r.getRequestedDate()).requestedStart(r.getRequestedStart()).requestedEnd(r.getRequestedEnd())
                        .reason(r.getReason()).status(r.getStatus()).createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RescheduleVO approve(Long id) {
        Long teacherId = getCurrentTeacherId();
        RescheduleRequest req = rescheduleRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "调课申请不存在"));
        if (!req.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (!"pending".equals(req.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        // 更新原 session 的日期和时间
        studentSessionRepository.findById(req.getSessionId()).ifPresent(session -> {
            session.setClassDate(req.getRequestedDate());
            session.setStartTime(req.getRequestedStart());
            session.setEndTime(req.getRequestedEnd());
            studentSessionRepository.save(session);
        });

        req.setStatus("approved");
        rescheduleRequestRepository.save(req);

        log.info("老师{} 批准调课: rescheduleId={}, sessionId={}", teacherId, id, req.getSessionId());
        return toVO(req);
    }

    @Override
    @Transactional
    public RescheduleVO defer(Long id) {
        Long teacherId = getCurrentTeacherId();
        RescheduleRequest req = rescheduleRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "调课申请不存在"));
        if (!req.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权操作");
        }

        req.setStatus("deferred");
        rescheduleRequestRepository.save(req);
        log.info("老师{} 待议调课: rescheduleId={}", teacherId, id);
        return toVO(req);
    }

    @Override
    @Transactional
    public void close(Long id) {
        Long teacherId = getCurrentTeacherId();
        RescheduleRequest req = rescheduleRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "调课申请不存在"));
        if (!req.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权操作");
        }

        req.setStatus("closed");
        rescheduleRequestRepository.save(req);
        log.info("老师{} 关闭调课: rescheduleId={}", teacherId, id);
    }

    private RescheduleVO toVO(RescheduleRequest r) {
        String studentName = studentRepository.findById(r.getStudentId()).map(Student::getName).orElse("未知");
        String teacherName = userRepository.findById(r.getTeacherId())
                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername()).orElse("");
        return RescheduleVO.builder()
                .id(r.getId()).sessionId(r.getSessionId())
                .studentId(r.getStudentId()).studentName(studentName)
                .teacherId(r.getTeacherId()).teacherName(teacherName)
                .subject(r.getSubject())
                .originalDate(r.getOriginalDate()).originalStart(r.getOriginalStart()).originalEnd(r.getOriginalEnd())
                .requestedDate(r.getRequestedDate()).requestedStart(r.getRequestedStart()).requestedEnd(r.getRequestedEnd())
                .reason(r.getReason()).status(r.getStatus()).createdAt(r.getCreatedAt())
                .build();
    }
}