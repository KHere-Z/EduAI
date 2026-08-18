package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.FeedbackDTO;
import com.eduai.system.entity.Feedback;
import com.eduai.system.entity.Student;
import com.eduai.system.repository.FeedbackRepository;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.service.FeedbackService;
import com.eduai.system.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    // ==================== 权限 ====================

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private void checkTeacher() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(403, "仅教师可访问");
        }
    }

    private void checkStudent() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
        if (user.getRoleType() != 4) {
            throw new BusinessException(403, "仅学生可访问");
        }
    }

    // ==================== 老师端 ====================

    @Override
    @Transactional
    public FeedbackVO create(FeedbackDTO dto) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        Feedback fb = Feedback.builder()
                .teacherId(teacherId)
                .studentId(dto.getStudentId())
                .subject(dto.getSubject())
                .period(dto.getPeriod())
                .content(dto.getContent())
                .build();

        fb = feedbackRepository.save(fb);
        log.info("老师{} 发送反馈: feedbackId={}, studentId={}, subject={}",
                teacherId, fb.getId(), dto.getStudentId(), dto.getSubject());
        return toVO(fb);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackVO> listTeacherFeedbacks(String subject) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        List<Feedback> list = feedbackRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        // 按学科筛选
        if (subject != null && !subject.isBlank()) {
            list = list.stream()
                    .filter(f -> subject.equals(f.getSubject()))
                    .collect(Collectors.toList());
        }

        return toVOList(list);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        checkTeacher();
        Long teacherId = getCurrentUserId();

        Feedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "反馈不存在"));
        if (!fb.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "只能删除自己的反馈");
        }

        feedbackRepository.delete(fb);
        log.info("老师{} 删除反馈: id={}", teacherId, id);
    }

    // ==================== 学生端 ====================

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackVO> listStudentFeedbacks(String subject) {
        checkStudent();
        Long userId = getCurrentUserId();

        // 查找当前用户对应的学生档案
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "学生档案不存在"));
        Long studentId = student.getId();

        List<Feedback> list = feedbackRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        // 按学科筛选
        if (subject != null && !subject.isBlank()) {
            list = list.stream()
                    .filter(f -> subject.equals(f.getSubject()))
                    .collect(Collectors.toList());
        }

        return toVOList(list);
    }

    // ==================== 内部 ====================

    private List<FeedbackVO> toVOList(List<Feedback> list) {
        if (list.isEmpty()) return List.of();

        // 批量查老师姓名
        Set<Long> teacherIds = list.stream().map(Feedback::getTeacherId).collect(Collectors.toSet());
        Map<Long, String> teacherNameMap = userRepository.findAllById(teacherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        // 批量查学生姓名
        Set<Long> studentIds = list.stream().map(Feedback::getStudentId).collect(Collectors.toSet());
        Map<Long, String> studentNameMap = studentRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Student::getName));

        return list.stream()
                .map(f -> toVO(f, teacherNameMap, studentNameMap))
                .collect(Collectors.toList());
    }

    private FeedbackVO toVO(Feedback f) {
        return toVO(f, Map.of(), Map.of());
    }

    private FeedbackVO toVO(Feedback f, Map<Long, String> teacherNameMap, Map<Long, String> studentNameMap) {
        return FeedbackVO.builder()
                .id(f.getId())
                .teacherId(f.getTeacherId())
                .teacherName(teacherNameMap.getOrDefault(f.getTeacherId(), ""))
                .studentId(f.getStudentId())
                .studentName(studentNameMap.getOrDefault(f.getStudentId(), ""))
                .subject(f.getSubject())
                .period(f.getPeriod())
                .content(f.getContent())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
