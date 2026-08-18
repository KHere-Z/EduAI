package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.HomeworkDTO;
import com.eduai.system.entity.*;
import com.eduai.system.repository.*;
import com.eduai.system.service.HomeworkService;
import com.eduai.system.vo.HomeworkVO;
import com.eduai.system.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;

    // ==================== 鉴权 ====================

    private void checkTeacher() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3) throw new BusinessException(403, "仅教师可访问");
    }

    private Long getTeacherId() {
        return StpUtil.getLoginIdAsLong();
    }

    private Student getCurrentStudent() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 4) throw new BusinessException(403, "仅学生可访问");
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "未找到学生档案"));
    }

    private String getTeacherName(Long teacherId) {
        return userRepository.findById(teacherId)
                .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
                .orElse("未知");
    }

    private String getStudentName(Long studentId) {
        return studentRepository.findById(studentId)
                .map(Student::getName).orElse("未知");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllSubmissions(String subject) {
        checkTeacher();
        Long teacherId = getTeacherId();

        List<Homework> homeworks = (subject != null && !subject.isBlank())
                ? homeworkRepository.findByTeacherIdAndSubjectOrderByCreatedAtDesc(teacherId, subject)
                : homeworkRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Homework hw : homeworks) {
            List<HomeworkSubmission> subs = submissionRepository.findByHomeworkId(hw.getId());
            for (HomeworkSubmission s : subs) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("homeworkId", hw.getId());
                row.put("title", hw.getTitle());
                row.put("studentId", s.getStudentId());
                row.put("studentName", getStudentName(s.getStudentId()));
                row.put("status", s.getStatus());
                row.put("submittedImageUrl", s.getSubmittedImageUrl());
                row.put("correctedImageUrl", s.getCorrectedImageUrl());
                row.put("createdAt", hw.getCreatedAt());
                result.add(row);
            }
        }
        return result;
    }

    // ==================== 老师端 ====================

    @Override
    @Transactional
    public HomeworkVO createHomework(HomeworkDTO dto) {
        checkTeacher();
        Long teacherId = getTeacherId();

        Homework hw = Homework.builder()
                .teacherId(teacherId)
                .subject(dto.getSubject())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build();
        hw = homeworkRepository.save(hw);
        log.info("老师{} 布置作业: id={}, title={}", teacherId, hw.getId(), dto.getTitle());
        return toVO(hw);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkVO> listTeacherHomework(String subject) {
        checkTeacher();
        Long teacherId = getTeacherId();

        List<Homework> list = (subject != null && !subject.isBlank())
                ? homeworkRepository.findByTeacherIdAndSubjectOrderByCreatedAtDesc(teacherId, subject)
                : homeworkRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        String teacherName = getTeacherName(teacherId);
        return list.stream().map(h -> toVO(h, teacherName)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteHomework(Long id) {
        checkTeacher();
        Homework hw = homeworkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "作业不存在"));
        if (!hw.getTeacherId().equals(getTeacherId())) throw new BusinessException(403, "无权删除");
        submissionRepository.deleteByHomeworkId(id);
        homeworkRepository.delete(hw);
        log.info("老师{} 删除作业: id={}", getTeacherId(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionVO> listSubmissions(Long homeworkId) {
        checkTeacher();
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new BusinessException(404, "作业不存在"));
        if (!hw.getTeacherId().equals(getTeacherId())) throw new BusinessException(403, "无权查看该作业");

        List<HomeworkSubmission> subs = submissionRepository.findByHomeworkId(homeworkId);
        return subs.stream().map(s -> SubmissionVO.builder()
                .id(s.getId()).homeworkId(s.getHomeworkId()).studentId(s.getStudentId())
                .studentName(getStudentName(s.getStudentId()))
                .submittedImageUrl(s.getSubmittedImageUrl())
                .correctedImageUrl(s.getCorrectedImageUrl())
                .status(s.getStatus())
                .submittedAt(s.getSubmittedAt()).correctedAt(s.getCorrectedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HomeworkVO uploadAnswer(Long homeworkId, Map<String, String> body) {
        checkTeacher();
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new BusinessException(404, "作业不存在"));
        if (!hw.getTeacherId().equals(getTeacherId())) throw new BusinessException(403, "无权修改该作业");
        hw.setAnswerFileUrl(body.get("answerFileUrl"));
        hw.setAnswerFileName(body.get("answerFileName"));
        homeworkRepository.save(hw);
        return toVO(hw);
    }

    @Override
    @Transactional
    public SubmissionVO correctSubmission(Long homeworkId, Map<String, Object> body) {
        checkTeacher();
        Homework hw = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new BusinessException(404, "作业不存在"));
        if (!hw.getTeacherId().equals(getTeacherId())) throw new BusinessException(403, "无权批改该作业");

        Long studentId = body.get("studentId") instanceof Number n
                ? n.longValue() : Long.parseLong(body.get("studentId").toString());

        HomeworkSubmission sub = submissionRepository
                .findByHomeworkIdAndStudentId(homeworkId, studentId)
                .orElseThrow(() -> new BusinessException(404, "该学生未提交"));

        // 追加批改图（|||| 分隔，避免 base64 逗号冲突）
        String newUrl = (String) body.get("correctedImageUrl");
        String existing = sub.getCorrectedImageUrl();
        if (existing != null && !existing.isBlank()) {
            sub.setCorrectedImageUrl(existing + "||||" + newUrl);
        } else {
            sub.setCorrectedImageUrl(newUrl);
        }
        sub.setStatus("corrected");
        sub.setCorrectedAt(LocalDateTime.now());
        submissionRepository.save(sub);

        return SubmissionVO.builder()
                .id(sub.getId()).homeworkId(sub.getHomeworkId()).studentId(sub.getStudentId())
                .studentName(getStudentName(sub.getStudentId()))
                .submittedImageUrl(sub.getSubmittedImageUrl())
                .correctedImageUrl(sub.getCorrectedImageUrl())
                .status(sub.getStatus())
                .submittedAt(sub.getSubmittedAt()).correctedAt(sub.getCorrectedAt())
                .build();
    }

    // ==================== 学生端 ====================

    @Override
    @Transactional(readOnly = true)
    public List<HomeworkVO> listStudentHomework(String subject) {
        Student student = getCurrentStudent();

        // 该学生的老师ID列表
        List<Long> teacherIds = teacherStudentRepository.findByStudentId(student.getId())
                .stream().map(TeacherStudent::getTeacherId).distinct().toList();
        if (teacherIds.isEmpty()) return List.of();

        // 直查该学生老师的作业（数据库层过滤 + 排序）
        List<Homework> list = (subject != null && !subject.isBlank())
                ? homeworkRepository.findByTeacherIdInAndSubjectOrderByCreatedAtDesc(teacherIds, subject)
                : homeworkRepository.findByTeacherIdInOrderByCreatedAtDesc(teacherIds);

        // 批量查提交记录
        List<Long> homeworkIds = list.stream().map(Homework::getId).toList();
        Map<Long, HomeworkSubmission> subMap = submissionRepository.findByStudentId(student.getId())
                .stream().filter(s -> homeworkIds.contains(s.getHomeworkId()))
                .collect(Collectors.toMap(HomeworkSubmission::getHomeworkId, s -> s, (a, b) -> a));

        List<HomeworkVO> vos = new ArrayList<>();
        for (Homework hw : list) {
            String teacherName = getTeacherName(hw.getTeacherId());
            HomeworkVO vo = toVO(hw, teacherName);

            HomeworkSubmission sub = subMap.get(hw.getId());
            if (sub != null) {
                vo.setSubmitStatus(sub.getStatus());
                vo.setSubmittedImageUrl(sub.getSubmittedImageUrl());
                vo.setCorrectedImageUrl(sub.getCorrectedImageUrl());
            } else {
                vo.setSubmitStatus("pending");
            }
            vos.add(vo);
        }
        return vos;
    }

    @Override
    @Transactional
    public SubmissionVO submitHomework(Long homeworkId, Map<String, Object> body) {
        Student student = getCurrentStudent();

        HomeworkSubmission sub = submissionRepository
                .findByHomeworkIdAndStudentId(homeworkId, student.getId())
                .orElseGet(() -> HomeworkSubmission.builder()
                        .homeworkId(homeworkId).studentId(student.getId()).build());

        // 兼容单图(String)和多图(List)
        Object imgObj = body.containsKey("imageUrl") ? body.get("imageUrl") : body.get("submittedImageUrl");
        log.info("收到提交: homeworkId={}, imageUrl={}", homeworkId,
                imgObj instanceof List<?> l ? "数组(" + l.size() + "张)" : imgObj);
        String img;
        if (imgObj instanceof List<?> list) {
            img = list.stream().map(Object::toString).collect(Collectors.joining(","));
        } else {
            img = imgObj instanceof String s ? s : "";
        }
        sub.setSubmittedImageUrl(img);
        sub.setStatus("submitted");
        sub.setSubmittedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        return SubmissionVO.builder()
                .id(sub.getId()).homeworkId(sub.getHomeworkId()).studentId(sub.getStudentId())
                .studentName(student.getName())
                .submittedImageUrl(sub.getSubmittedImageUrl())
                .correctedImageUrl(sub.getCorrectedImageUrl())
                .status(sub.getStatus())
                .submittedAt(sub.getSubmittedAt()).correctedAt(sub.getCorrectedAt())
                .build();
    }

    // ==================== 内部 ====================

    private HomeworkVO toVO(Homework hw) {
        return toVO(hw, getTeacherName(hw.getTeacherId()));
    }

    private HomeworkVO toVO(Homework hw, String teacherName) {
        return HomeworkVO.builder()
                .id(hw.getId()).teacherId(hw.getTeacherId()).teacherName(teacherName)
                .subject(hw.getSubject()).title(hw.getTitle()).description(hw.getDescription())
                .answerFileUrl(hw.getAnswerFileUrl()).answerFileName(hw.getAnswerFileName())
                .createdAt(hw.getCreatedAt())
                .build();
    }
}
