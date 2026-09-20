package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.HomeworkDTO;
import com.eduai.system.entity.*;
import com.eduai.system.repository.*;
import com.eduai.system.service.HomeworkService;
import com.eduai.system.service.ImageStorageService;
import com.eduai.system.vo.HomeworkVO;
import com.eduai.system.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
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
    private final ImageStorageService imageStorageService;

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

    /**
     * 同 {@link #getCurrentStudent()}，但「学生档案尚未建立」返回 {@code null} 而不抛 404。
     * <p>
     * 档案在<b>自助注册时</b>就已建立（{@code AuthServiceImpl.createStudentProfile}），
     * 所以 {@code null} 现在只对应两类账号：本次改动<b>之前</b>注册、且从未被老师添加过的
     * <b>存量账号</b>，以及档案被管理员删除过的账号。仍然保留这条软化路径，是为了让这两类
     * 账号进首页时不弹红条 —— 它们「没有档案」是既成事实，空列表才是诚实答案。
     * <p>
     * <b>只给「空是诚实答案」的读接口用</b>（如 {@link #listStudentHomework}）。提交作业
     * （{@code submitHomework}）必须继续走 {@link #getCurrentStudent()}：它要真实
     * {@code student.id} 才能落库。401、403 仍照抛。
     */
    private Student getCurrentStudentOrNull() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 4) throw new BusinessException(403, "仅学生可访问");
        return studentRepository.findByUserId(userId).orElse(null);
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
        // 答案解析图是 \n 拼接（MathManage.vue:515 拼、:474 拆），逐张落盘
        hw.setAnswerFileUrl(persistImages(body.get("answerFileUrl"), "\n", "homework-answer"));
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

        // 追加批改图（|||| 分隔，避免 base64 逗号冲突）。
        // 新图先落盘：corrected_image_url 是 TEXT(64KB)，直接塞 base64 会 1406 或静默截断。
        String newUrl = persistImages((String) body.get("correctedImageUrl"), "||||", "homework-correct");
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
        // 还没建档 ＝ 还没被任何老师添加，空列表就是诚实答案（下面 teacherIds 为空时本就 return List.of()）
        Student student = getCurrentStudentOrNull();
        if (student == null) return List.of();

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
        // 分隔符必须保持 ","：读取端 Homework.vue:105 与 MathManage.vue:448 都按逗号拆，
        // 改成 JSON 数组字符串（["data:...","data:..."]）会让两处同时哑掉。
        // 前端传的是**数组**（Homework.vue:157 { imageUrl: allImgs }），所以这里逐个元素落盘，
        // 不按逗号拆 —— data URL 头部 data:image/jpeg;base64, 自带一个逗号，拆了就坏。
        String img;
        if (imgObj instanceof List<?> list) {
            img = list.stream().map(Object::toString)
                    .map(v -> imageStorageService.persistIfBase64(v, "homework-submit"))
                    .collect(Collectors.joining(","));
        } else {
            // 字符串分支（旧客户端单图）：整体落盘。若是多张拼成的串，persistIfBase64 内部
            // 解码会因非法字符抛异常并原样返回 —— 退化为无操作，不会写坏数据。
            img = imgObj instanceof String s ? imageStorageService.persistIfBase64(s, "homework-submit") : "";
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

    /**
     * 把「分隔符拼接的多图字符串」逐段过一遍 {@link ImageStorageService#persistIfBase64}，
     * 让 base64 data URL 落盘成 URL 后再入库 —— 与题库（{@code QuestionBankServiceImpl} 的
     * uploadQuestion / updateQuestion / gradeQuestion / addWrongQuestion / saveAnswer）同一口径。
     * <p>
     * <b>为什么必须做</b>：homework / homework_submissions 的图片列是 {@code TEXT}(64KB)，
     * 而题库三列是 {@code MEDIUMTEXT}。学生端压 1024px/JPEG 0.7（{@code Homework.vue:148}）、
     * 老师端 800px（{@code MathManage.vue} 的 fileToBase64），单张 base64 就已接近甚至超过 64KB，
     * 多张拼接必超 —— 严格模式报 1406（HTTP 500），非严格模式静默截断成坏图。
     * 落盘后 DB 只留轻量 URL，与 {@code ImageStorageService} 类头「base64 整列载入是内存热点」的
     * 结论一致。
     * <p>
     * <b>分隔符必须是 base64 字母表外的字符</b>：{@code \n} 与 {@code ||||} 都安全；
     * <b>逗号不安全</b> —— data URL 头部 {@code data:image/jpeg;base64,} 自带一个逗号，
     * 按逗号拆会把图拆坏。学生提交那条路因此不在这里拆（前端传的是数组，逐元素处理）。
     * <p>
     * 全段都不含 base64 时直接返回原值，不产生额外开销（已落盘的 URL 会被原样带回重发）。
     */
    private String persistImages(String joined, String delimiter, String subDir) {
        if (joined == null || joined.isBlank() || !joined.contains("data:image/")) {
            return joined;
        }
        return Arrays.stream(joined.split(Pattern.quote(delimiter), -1))
                .map(v -> imageStorageService.persistIfBase64(v, subDir))
                .collect(Collectors.joining(delimiter));
    }

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
