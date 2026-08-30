package com.eduai.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.ExamPaperDTO;
import com.eduai.system.entity.ExamPaper;
import com.eduai.system.entity.Student;
import com.eduai.system.repository.ExamPaperRepository;
import com.eduai.system.repository.StudentRepository;
import com.eduai.system.repository.TeacherStudentRepository;
import com.eduai.system.service.ExamPaperService;
import com.eduai.system.service.ImageStorageService;
import com.eduai.system.vo.ExamPaperVO;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eduai.system.util.LatexRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 试卷分析 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperRepository examPaperRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final ImageStorageService imageStorageService;

    /** 上传目录（限制本地图片读取范围，防止任意文件读取） */
    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    private static final Map<String, String> SUBJECT_MAP = Map.ofEntries(
            Map.entry("math", "数学"), Map.entry("english", "英语"),
            Map.entry("chinese", "语文"), Map.entry("physics", "物理"),
            Map.entry("chemistry", "化学"), Map.entry("biology", "生物"),
            Map.entry("history", "历史"), Map.entry("politics", "政治"),
            Map.entry("geography", "地理")
    );

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

    /** Entity → VO（批量场景用，studentName 由调用方填充） */
    private ExamPaperVO toVO(ExamPaper paper) {
        return toVO(paper, null);
    }

    /** Entity → VO（带学生姓名） */
    private ExamPaperVO toVO(ExamPaper paper, String studentName) {
        List<String> images = null;
        if (paper.getPaperImages() != null && !paper.getPaperImages().isBlank()) {
            images = Arrays.stream(paper.getPaperImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return ExamPaperVO.builder()
                .id(paper.getId())
                .studentId(paper.getStudentId())
                .studentName(studentName)
                .subject(paper.getSubject())
                .examType(paper.getExamType())
                .school(paper.getSchool())
                .paperImages(images)
                .wrongQuestions(paper.getWrongQuestions())
                .paperAnalysis(paper.getPaperAnalysis())
                .suggestions(paper.getSuggestions())
                .score(paper.getScore())
                .kpList(paper.getKpList())
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ExamPaperVO createExamPaper(ExamPaperDTO dto) {
        Student student = getCurrentStudent();

        // paperImages 列表 → base64 落盘/落 COS 转 URL → 逗号分隔
        // （前端传 base64 会撞 paper_images TEXT 64KB 上限，此处统一转 URL 存库）
        String imagesStr = null;
        if (dto.getPaperImages() != null && !dto.getPaperImages().isEmpty()) {
            imagesStr = dto.getPaperImages().stream()
                    .map(img -> imageStorageService.persistIfBase64(img, "exam"))
                    .collect(Collectors.joining(","));
        }

        ExamPaper paper = ExamPaper.builder()
                .studentId(student.getId())
                .subject(dto.getSubject())
                .examType(dto.getExamType())
                .school(dto.getSchool())
                .paperImages(imagesStr)
                .wrongQuestions(dto.getWrongQuestions())
                .paperAnalysis(dto.getPaperAnalysis())
                .suggestions(dto.getSuggestions())
                .score(dto.getScore())
                .kpList(dto.getKpList())
                .build();
        paper = examPaperRepository.save(paper);

        log.info("学生{} 上传试卷: id={}, subject={}, examType={}",
                student.getId(), paper.getId(), dto.getSubject(), dto.getExamType());
        return toVO(paper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamPaperVO> listExamPapers(String subject) {
        Student student = getCurrentStudent();

        List<ExamPaper> papers;
        if (subject != null && !subject.isBlank()) {
            papers = examPaperRepository.findByStudentIdAndSubjectOrderByCreatedAtDesc(student.getId(), subject);
        } else {
            papers = examPaperRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        }

        return papers.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPaperVO getExamPaperDetail(Long id) {
        Student student = getCurrentStudent();

        ExamPaper paper = examPaperRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));
        return toVO(paper);
    }

    @Override
    @Transactional
    public void deleteExamPaper(Long id) {
        Student student = getCurrentStudent();

        ExamPaper paper = examPaperRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));
        examPaperRepository.delete(paper);
        log.info("学生{} 删除试卷: id={}, subject={}", student.getId(), id, paper.getSubject());
    }

    // ==================== 老师端 ====================

    /** 校验当前用户是否为教师 */
    private void checkTeacher() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (user.getRoleType() != 3) {
            throw new BusinessException(403, "仅教师可访问");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamPaperVO> listTeacherExamPapers(Long studentId, String subject) {
        checkTeacher();
        Long userId = StpUtil.getLoginIdAsLong();

        // 获取该老师的所有学生
        List<Long> studentIds = teacherStudentRepository.findByTeacherId(userId)
                .stream().map(ts -> ts.getStudentId()).distinct().toList();

        if (studentIds.isEmpty()) return List.of();

        // 如果指定了 studentId，验证是否属于该老师
        if (studentId != null) {
            if (!studentIds.contains(studentId)) {
                throw new BusinessException(403, "无权查看该学生的试卷");
            }
            studentIds = List.of(studentId);
        }

        List<ExamPaper> papers;
        if (subject != null && !subject.isBlank()) {
            papers = examPaperRepository.findByStudentIdInAndSubjectOrderByCreatedAtDesc(studentIds, subject);
        } else {
            papers = examPaperRepository.findByStudentIdInOrderByCreatedAtDesc(studentIds);
        }

        // 批量加载学生姓名
        Map<Long, String> studentNameMap = studentRepository.findAllById(
                papers.stream().map(ExamPaper::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(Student::getId, Student::getName));

        return papers.stream()
                .map(p -> toVO(p, studentNameMap.get(p.getStudentId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExamPaperVO getTeacherExamPaperDetail(Long id) {
        checkTeacher();
        Long userId = StpUtil.getLoginIdAsLong();

        // 验证试卷存在且属于该老师的学生
        ExamPaper paper = examPaperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));

        List<Long> studentIds = teacherStudentRepository.findByTeacherId(userId)
                .stream().map(ts -> ts.getStudentId()).distinct().toList();

        if (!studentIds.contains(paper.getStudentId())) {
            throw new BusinessException(403, "无权查看该试卷");
        }

        String studentName = studentRepository.findById(paper.getStudentId())
                .map(Student::getName).orElse(null);
        return toVO(paper, studentName);
    }

    @Override
    @Transactional
    public ExamPaperVO updateTeacherExamPaper(Long id, Map<String, Object> body) {
        checkTeacher();
        Long userId = StpUtil.getLoginIdAsLong();

        ExamPaper paper = examPaperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));

        // 验证所有权
        List<Long> studentIds = teacherStudentRepository.findByTeacherId(userId)
                .stream().map(ts -> ts.getStudentId()).distinct().toList();
        if (!studentIds.contains(paper.getStudentId())) {
            throw new BusinessException(403, "无权修改该试卷");
        }

        if (body.containsKey("score")) {
            Object scoreObj = body.get("score");
            if (scoreObj instanceof Number) {
                paper.setScore(((Number) scoreObj).doubleValue());
            } else if (scoreObj instanceof String s && !s.isBlank()) {
                paper.setScore(Double.parseDouble(s));
            }
        }
        if (body.containsKey("paperAnalysis")) {
            paper.setPaperAnalysis((String) body.get("paperAnalysis"));
        }
        if (body.containsKey("suggestions")) {
            paper.setSuggestions((String) body.get("suggestions"));
        }
        if (body.containsKey("kpList")) {
            paper.setKpList((String) body.get("kpList"));
        }

        examPaperRepository.save(paper);
        log.info("老师{} 编辑试卷: id={}, score={}", userId, id, paper.getScore());

        String studentName = studentRepository.findById(paper.getStudentId())
                .map(Student::getName).orElse(null);
        return toVO(paper, studentName);
    }

    @Override
    @Transactional
    public ExamPaperVO updateStudentExamPaper(Long id, Map<String, Object> body) {
        Student student = getCurrentStudent();

        ExamPaper paper = examPaperRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));

        if (body.containsKey("score")) {
            Object scoreObj = body.get("score");
            if (scoreObj instanceof Number) {
                paper.setScore(((Number) scoreObj).doubleValue());
            } else if (scoreObj instanceof String s && !s.isBlank()) {
                paper.setScore(Double.parseDouble(s));
            }
        }

        examPaperRepository.save(paper);
        log.info("学生{} 填写分数: examId={}, score={}", student.getId(), id, paper.getScore());
        return toVO(paper, student.getName());
    }

    @Override
    @Transactional
    public void deleteTeacherExamPaper(Long id) {
        checkTeacher();
        Long userId = StpUtil.getLoginIdAsLong();

        ExamPaper paper = examPaperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));

        List<Long> studentIds = teacherStudentRepository.findByTeacherId(userId)
                .stream().map(ts -> ts.getStudentId()).distinct().toList();
        if (!studentIds.contains(paper.getStudentId())) {
            throw new BusinessException(403, "无权删除该试卷");
        }

        examPaperRepository.delete(paper);
        log.info("老师{} 删除试卷: id={}, subject={}", userId, id, paper.getSubject());
    }

    // ==================== 试卷分析 PDF 导出 ====================

    private static final ObjectMapper PDF_JSON = new ObjectMapper();

    /** 中文字体路径（按优先级尝试：classpath 捆绑 → Windows 系统 → macOS 系统） */
    private static final String[] CHINESE_FONT_PATHS = {
            "fonts/NotoSansSC-Regular.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "/System/Library/Fonts/PingFang.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    };

    @Override
    public byte[] generatePdf(Long id) {
        Student student = getCurrentStudent();
        ExamPaper paper = examPaperRepository.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new BusinessException(404, "试卷不存在"));

        String html = buildPdfHtml(paper);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            registerChineseFont(renderer);
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BusinessException(500, "PDF生成失败: " + e.getMessage());
        }
    }

    // ==================== 出卷 PDF 导出 ====================

    @Override
    @SuppressWarnings("unchecked")
    public byte[] generateExamPaperPdf(Map<String, Object> body) {
        String html = buildExamPaperHtml(body);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            registerChineseFont(renderer);
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new BusinessException(500, "出卷PDF生成失败: " + e.getMessage());
        }
    }

    /** 根据前端出卷 JSON 构建 HTML */
    private String buildExamPaperHtml(Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "数学试卷");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pages = (List<Map<String, Object>>) body.get("pages");

        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html><html><head><meta charset="UTF-8"/>
                <style>
                  @page { size: A4; margin: 2cm 2.5cm; }
                  body { font-family: "Noto Sans SC","SimSun","PingFang SC","Microsoft YaHei",sans-serif;
                         font-size: 12pt; line-height: 2; color: #333; }
                  .main-title { text-align: center; font-size: 20pt; font-weight: bold; margin-bottom: 8px; }
                  .info-line { text-align: center; font-size: 11pt; color: #666; margin-bottom: 24px; }
                  .section-title { font-size: 14pt; font-weight: bold; margin: 18px 0 10px 0;
                                   border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; }
                  .question { margin: 10px 0; padding-left: 12px; }
                  .question img.formula { vertical-align: baseline; margin: 0 2px; }
                  .question img.diagram { display: block; max-width: 80%; margin: 8px 0; }
                  .page-break { page-break-before: always; }
                </style></head><body>
                """);

        for (int pi = 0; pi < pages.size(); pi++) {
            Map<String, Object> page = pages.get(pi);

            if (pi > 0) {
                html.append("<div class=\"page-break\"></div>\n");
            }

            // 试卷标题（仅第一页）
            if (pi == 0) {
                html.append("<div class=\"main-title\">").append(escapeHtml(title)).append("</div>\n");
                html.append("<div class=\"info-line\">")
                        .append("姓名：________  班级：________  得分：________")
                        .append("</div>\n");
            }

            // 板块
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections =
                    (List<Map<String, Object>>) page.get("sections");

            for (Map<String, Object> section : sections) {
                String secTitle = (String) section.get("title");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questions =
                        (List<Map<String, Object>>) section.get("questions");

                if (questions == null || questions.isEmpty()) continue;

                html.append("<div class=\"section-title\">")
                        .append(escapeHtml(secTitle)).append("</div>\n");

                for (int qi = 0; qi < questions.size(); qi++) {
                    Map<String, Object> qData = questions.get(qi);
                    String qTitle = (String) qData.get("title");
                    if (qTitle == null || qTitle.isBlank()) continue;

                    html.append("<div class=\"question\">");
                    html.append("<b>").append(qi + 1).append(".</b> ");
                    renderQuestionText(html, qTitle);
                    html.append("</div>\n");

                    // 题目配图
                    String imageUrl = (String) qData.get("imageUrl");
                    if (imageUrl == null) imageUrl = (String) qData.get("diagramImageUrl");
                    if (imageUrl != null && !imageUrl.isBlank()) {
                        String dataUrl = resolveImageDataUrl(imageUrl);
                        if (dataUrl != null) {
                            html.append("<img class=\"diagram\" src=\"")
                                    .append(dataUrl).append("\" alt=\"\"/>\n");
                        } else {
                            html.append("<div style=\"color:#999;font-size:10pt\">")
                                    .append("[图片：").append(escapeHtml(imageUrl)).append("]")
                                    .append("</div>\n");
                        }
                    }
                }
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    /** 将题目文本中的 $...$ LaTeX 公式渲染为 &lt;img&gt; 嵌入 HTML，基线与正文对齐 */
    private void renderQuestionText(StringBuilder html, String text) {
        String normalized = text.replaceAll("\\$\\$([^$]+)\\$\\$", "\\$$1\\$");
        String[] parts = normalized.split("\\$");

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (i % 2 == 1) {
                try {
                    LatexRenderer.FormulaResult r = LatexRenderer.render(parts[i].trim(), 12f);
                    // vertical-align 负值将公式下移，使公式基线与文本基线对齐
                    html.append("<img class=\"formula\" style=\"vertical-align: -")
                            .append(r.depth()).append("px\" src=\"")
                            .append(r.dataUrl()).append("\" alt=\"")
                            .append(escapeHtml(parts[i].trim())).append("\"/>");
                } catch (Exception e) {
                    log.warn("公式渲染失败, 回退为纯文本: latex={}, err={}", parts[i], e.getMessage());
                    html.append("<span style=\"font-style:italic\">")
                            .append(escapeHtml(parts[i].trim())).append("</span>");
                }
            } else {
                html.append(escapeHtml(parts[i]));
            }
        }
    }

    /** 将本地图片转为 base64 data URL（仅允许 data URL 或 uploads 目录内图片，防止任意文件读取） */
    private String resolveImageDataUrl(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isBlank()) return null;
            if (imageUrl.startsWith("data:")) {
                return imageUrl;
            }
            // 归一化：拒绝绝对路径 / Windows 盘符 / 路径穿越（../）
            String normalized = imageUrl.replace('\\', '/');
            Path uploadsDir = Path.of(uploadDir).toAbsolutePath().normalize();

            Path imgPath;
            if (normalized.startsWith("/")) {
                // web URL（/uploads/...）→ 去掉 /uploads/ 前缀，相对 uploads 目录解析
                String prefix = "/uploads/";
                if (!normalized.startsWith(prefix)) {
                    log.warn("拒绝非法图片路径: {}", imageUrl);
                    return null;
                }
                imgPath = uploadsDir.resolve(normalized.substring(prefix.length())).normalize();
            } else {
                // 历史相对路径（如 uploads/ai/xxx.png，相对进程 CWD）
                if (normalized.matches("^[a-zA-Z]:.*") || normalized.contains("..")) {
                    log.warn("拒绝非法图片路径: {}", imageUrl);
                    return null;
                }
                imgPath = Path.of(normalized).toAbsolutePath().normalize();
            }
            if (!imgPath.startsWith(uploadsDir)) {
                log.warn("拒绝越界图片路径: {}", imageUrl);
                return null;
            }
            if (!Files.exists(imgPath)) {
                log.warn("图片文件不存在: {}", imgPath);
                return null;
            }
            String fileName = imgPath.getFileName().toString();
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
            String mime = switch (ext) {
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "jpg", "jpeg" -> "image/jpeg";
                case "svg" -> "image/svg+xml";
                case "webp" -> "image/webp";
                default -> null;
            };
            if (mime == null) {
                log.warn("不支持的图片类型: {}", ext);
                return null;
            }
            byte[] bytes = Files.readAllBytes(imgPath);
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("图片读取失败: url={}, err={}", imageUrl, e.getMessage());
            return null;
        }
    }

    /** HTML 特殊字符转义 */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ==================== 中文字体注册 ====================

    private void registerChineseFont(ITextRenderer renderer) {
        // 1. 尝试 classpath 捆绑字体
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansSC-Regular.ttf")) {
            if (is != null) {
                Path tmp = Files.createTempFile("pdf-cjk-", ".ttf");
                Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                renderer.getFontResolver().addFont(tmp.toAbsolutePath().toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                tmp.toFile().deleteOnExit();
                log.info("PDF 中文字体: classpath 捆绑字体已注册");
                return;
            }
        } catch (Exception ignored) {}

        // 2. 回退：尝试系统字体路径
        for (String fontPath : CHINESE_FONT_PATHS) {
            if (fontPath.startsWith("fonts/")) continue;
            try {
                Path p = Path.of(fontPath);
                if (Files.exists(p)) {
                    renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.info("PDF 中文字体: 系统字体 {} 已注册", fontPath);
                    return;
                }
            } catch (Exception ignored) {}
        }

        log.warn("未找到中文字体文件，PDF 中文可能乱码。请将字体放到 src/main/resources/fonts/");
    }

    // ==================== 试卷分析报告 HTML 构建 ====================

    private String buildPdfHtml(ExamPaper paper) {
        String studentName = escapeHtml(getStudentName(paper.getStudentId()));
        String now = LocalDate.now().toString();
        String subjectName = escapeHtml(SUBJECT_MAP.getOrDefault(paper.getSubject(), paper.getSubject()));
        String examType = escapeHtml(paper.getExamType() != null ? paper.getExamType() : "");
        String scoreStr = paper.getScore() != null ? paper.getScore() + "分" : "";

        String raw = paper.getPaperAnalysis();
        String overview = clean(extract(raw, "overview"));
        String wrongQuestions = extract(raw, "wrongQuestions");
        String scoreRoadmap = extract(raw, "scoreRoadmap");
        String abilities = extract(raw, "abilities");
        String suggestions = extract(raw, "suggestions");
        String kpList = extract(raw, "kpList");

        return String.format("""
        <!DOCTYPE html><html><head><meta charset="UTF-8"/>
        <style>
          *{margin:0;padding:0;box-sizing:border-box}
          body{font-family:"Noto Sans SC","SimSun","PingFang SC","Microsoft YaHei",sans-serif;padding:8px 48px 40px 48px;color:#333;max-width:900px;margin:0 auto;font-size:14px;line-height:1.8}
          .main-title{text-align:center;font-size:22px;font-weight:bold;color:#4F46E5;margin-bottom:2px;padding-bottom:2px}
          .header{text-align:center;padding:10px 20px;background:linear-gradient(135deg,#6366F1,#8B5CF6);border-radius:12px;color:#fff;margin-bottom:2px}
          .header h1{font-size:16px;margin-bottom:2px}.header .tags span{background:rgba(255,255,255,.2);padding:2px 12px;border-radius:10px;font-size:12px;margin:0 4px}
          .card{margin-bottom:10px;border-radius:10px;border-left:3px solid #6366F1;padding:8px 16px;background:#fafbff}
          .card h3{font-size:15px;margin-bottom:6px}
          .wrong-item{margin-bottom:10px;padding-bottom:8px;border-bottom:1px dashed #e5e7eb}
          .footer{text-align:center;color:#aaa;font-size:11px;margin-top:24px;padding-top:12px;border-top:1px solid #eee}
          .flow-item{display:flex;align-items:flex-start;margin-bottom:10px}
          .flow-dot{width:12px;height:12px;border-radius:50%%;background:#10B981;margin-top:5px;flex-shrink:0;margin-right:12px}
          .flow-body{flex:1;font-size:13px;padding-bottom:10px;border-left:2px solid #10B981;padding-left:12px}
          .flow-body b{color:#059669}
          .flow-arrow{color:#10B981;font-size:16px;margin:0 4px}
        </style></head><body>
        <div class="main-title">AI考试分析报告</div>
        <div class="header">
          <h1>%s · %s</h1>
          <div class="tags"><span>%s</span><span>%s</span><span>%s</span></div>
        </div>
        %s
        %s
        %s
        %s
        %s
        %s
        %s
        <div class="footer">智学AI教育 · 智能试卷分析</div>
        </body></html>""",
        subjectName, examType, studentName, scoreStr, now,
        overview != null ? card("试卷概览", "#6366F1", overview) : "",
        scoreStr.isEmpty() ? "" : card("得分", "#10B981", scoreStr),
        wrongQuestions != null ? card("错题详解", "#EF4444", formatWrongQuestions(clean(wrongQuestions))) : "",
        scoreRoadmap != null ? card("追分路径", "#10B981", formatRoadmap(scoreRoadmap)) : "",
        abilities != null ? card("能力画像", "#8B5CF6", renderRadarPng(abilities)) : "",
        suggestions != null ? card("复习建议", "#F59E0B", clean(suggestions)) : "",
        kpList != null ? card("知识点清单", "#6366F1", clean(kpList)) : "");
    }

    private String extract(String text, String tag) {
        if (text == null) return null;
        Pattern p = Pattern.compile("\\[" + tag + "\\]([\\s\\S]*?)\\[/" + tag + "\\]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() && !m.group(1).trim().isEmpty() ? m.group(1).trim() : null;
    }

    private String clean(String text) {
        if (text == null) return "";
        return escapeHtml(stripKatex(text).replaceAll("\\[/?[a-zA-Z]+\\]", ""));
    }

    private String formatWrongQuestions(String text) {
        if (text == null || text.isBlank()) return "";
        String[] items = text.split("\n\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (item.isEmpty()) continue;
            if (i > 0) sb.append("</div>");
            sb.append("<div class=\"wrong-item\">").append(item.replace("\n", "<br/>"));
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String formatRoadmap(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|");
            String content;
            if (parts.length >= 3) {
                String module = escapeHtml(parts[0].trim());
                String current = escapeHtml(parts[1].trim());
                String target = escapeHtml(parts[2].trim());
                String strategy = parts.length > 3 ? "（" + escapeHtml(parts[3].trim()) + "）" : "";
                content = "<b>" + module + "</b>：" + current + " <span class=\"flow-arrow\">→</span> " + target + "分 " + strategy;
            } else {
                content = escapeHtml(line);
            }
            boolean last = (i == lines.length - 1);
            sb.append("<div class=\"flow-item\">")
              .append("<div class=\"flow-dot\" style=\"background:").append(last ? "#10B981" : "#6EE7B7").append("\"></div>")
              .append("<div class=\"flow-body\" style=\"").append(last ? "border-left:none" : "").append("\">")
              .append(content).append("</div></div>");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderRadarPng(String json) {
        try {
            List<Map<String, Object>> dims = new ArrayList<>();
            try {
                List<Object> arr = PDF_JSON.readValue(json, List.class);
                for (Object item : arr) {
                    if (item instanceof Map) dims.add((Map<String, Object>) item);
                }
            } catch (Exception e) {
                Map<String, Object> obj = PDF_JSON.readValue(json, Map.class);
                for (Map.Entry<String, Object> entry : obj.entrySet()) {
                    Map<String, Object> dim = new java.util.LinkedHashMap<>();
                    dim.put("name", entry.getKey());
                    dim.put("score", entry.getValue());
                    dims.add(dim);
                }
            }
            int n = dims.size();
            if (n < 3) return "<p>" + escapeHtml(json) + "</p>";

            int size = 400, cx = size / 2, cy = size / 2, r = 130;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

            double angleStep = 2 * Math.PI / n;
            double startAngle = -Math.PI / 2;

            for (int level = 1; level <= 5; level++) {
                g.setColor(level == 5 ? new Color(0xBBBBBB) : new Color(0xE8E8E8));
                g.setStroke(new BasicStroke(level == 5 ? 1.5f : 0.8f));
                int[] xs = new int[n], ys = new int[n];
                double lr = r * level / 5.0;
                for (int i = 0; i < n; i++) {
                    double a = startAngle + i * angleStep;
                    xs[i] = (int) (cx + lr * Math.cos(a));
                    ys[i] = (int) (cy + lr * Math.sin(a));
                }
                g.drawPolygon(xs, ys, n);
            }

            g.setStroke(new BasicStroke(0.8f));
            g.setColor(new Color(0xE0E0E0));
            for (int i = 0; i < n; i++) {
                double a = startAngle + i * angleStep;
                g.drawLine(cx, cy, (int) (cx + r * Math.cos(a)), (int) (cy + r * Math.sin(a)));
            }

            int[] dataX = new int[n], dataY = new int[n];
            for (int i = 0; i < n; i++) {
                Object scoreObj = dims.get(i).get("score");
                double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 50;
                double lr = r * score / 100.0;
                double a = startAngle + i * angleStep;
                dataX[i] = (int) (cx + lr * Math.cos(a));
                dataY[i] = (int) (cy + lr * Math.sin(a));
            }
            g.setColor(new Color(139, 92, 246, 60));
            g.fillPolygon(dataX, dataY, n);
            g.setColor(new Color(0x8B5CF6));
            g.setStroke(new BasicStroke(2f));
            g.drawPolygon(dataX, dataY, n);

            for (int i = 0; i < n; i++) {
                Object scoreObj = dims.get(i).get("score");
                double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 50;
                double a = startAngle + i * angleStep;
                double lr = r * score / 100.0;
                int px = (int) (cx + lr * Math.cos(a));
                int py = (int) (cy + lr * Math.sin(a));
                g.setColor(new Color(0x8B5CF6));
                g.fillOval(px - 4, py - 4, 8, 8);

                double la = startAngle + i * angleStep;
                int lx = (int) (cx + (r + 38) * Math.cos(la));
                int ly = (int) (cy + (r + 38) * Math.sin(la));
                String name = String.valueOf(dims.get(i).getOrDefault("name", ""));
                String label = name + " " + (int) score;
                g.setColor(new Color(0x555555));
                int tw = g.getFontMetrics().stringWidth(label);
                g.drawString(label, lx - tw / 2, ly + 5);
            }

            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            String base64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            return "<img src=\"data:image/png;base64," + base64 + "\" style=\"width:340px;display:block;margin:0 auto\"/>";
        } catch (Exception e) {
            log.warn("雷达图渲染失败: {}", e.getMessage());
            return "<p>" + (json != null ? json : "") + "</p>";
        }
    }

    private String card(String title, String color, String body) {
        return "<div class=\"card\" style=\"border-left-color:" + color + "\">" +
               "<h3 style=\"color:" + color + "\">" + title + "</h3>" +
               "<div>" + (body != null ? body : "") + "</div>" +
               "</div>";
    }

    private String stripKatex(String text) {
        if (text == null) return "";
        String result = text;
        result = replaceAllFraction(result);
        result = result.replaceAll("\\\\sqrt\\{([^}]+)\\}", "√($1)");
        result = result.replaceAll("\\^\\{2\\}", "\u00B2");
        result = result.replaceAll("\\^\\{3\\}", "\u00B3");
        result = result.replaceAll("\\^\\{4\\}", "\u2074");
        result = result.replaceAll("\\^\\{([^}]+)\\}", "^($1)");
        result = result.replaceAll("_\\{([^}]+)\\}", "_($1)");
        result = result.replaceAll("\\\\alpha\\b","\u03B1");
        result = result.replaceAll("\\\\beta\\b","\u03B2");
        result = result.replaceAll("\\\\gamma\\b","\u03B3");
        result = result.replaceAll("\\\\delta\\b","\u03B4");
        result = result.replaceAll("\\\\theta\\b","\u03B8");
        result = result.replaceAll("\\\\lambda\\b","\u03BB");
        result = result.replaceAll("\\\\mu\\b","\u03BC");
        result = result.replaceAll("\\\\pi\\b","\u03C0");
        result = result.replaceAll("\\\\sigma\\b","\u03C3");
        result = result.replaceAll("\\\\omega\\b","\u03C9");
        result = result.replaceAll("\\\\Delta\\b","\u0394");
        result = result.replaceAll("\\\\Omega\\b","\u03A9");
        result = result.replaceAll("\\\\Sigma\\b","\u03A3");
        result = result.replaceAll("\\\\Theta\\b","\u0398");
        result = result.replaceAll("\\\\Pi\\b","\u03A0");
        result = result.replaceAll("\\\\varepsilon\\b","\u03B5");
        result = result.replaceAll("\\\\rho\\b","\u03C1");
        result = result.replaceAll("\\\\varphi\\b","\u03C6");
        result = result.replaceAll("\\\\triangle\\b","\u25B3");
        result = result.replaceAll("\\\\times\\b","\u00D7");
        result = result.replaceAll("\\\\div\\b","\u00F7");
        result = result.replaceAll("\\\\pm\\b","\u00B1");
        result = result.replaceAll("\\\\cdot\\b","\u00B7");
        result = result.replaceAll("\\\\infty\\b","\u221E");
        result = result.replaceAll("\\\\angle\\b","\u2220");
        result = result.replaceAll("\\\\circ\\b","\u00B0");
        result = result.replaceAll("\\\\odot\\b","\u2299");
        result = result.replaceAll("\\\\sim\\b","\u223C");
        result = result.replaceAll("\\\\cong\\b","\u2245");
        result = result.replaceAll("\\\\neq\\b","\u2260");
        result = result.replaceAll("\\\\leq\\b","\u2264");
        result = result.replaceAll("\\\\geq\\b","\u2265");
        result = result.replaceAll("\\\\approx\\b","\u2248");
        result = result.replaceAll("\\\\perp\\b","\u22A5");
        result = result.replaceAll("\\\\parallel\\b","\u2225");
        result = result.replaceAll("\\\\Rightarrow\\b","\u21D2");
        result = result.replaceAll("\\\\leftrightarrow\\b","\u21D4");
        result = result.replaceAll("\\\\rightarrow\\b","\u2192");
        result = result.replaceAll("\\\\leftarrow\\b","\u2190");
        result = result.replaceAll("\\\\therefore\\b","\u2234");
        result = result.replaceAll("\\\\because\\b","\u2235");
        result = result.replaceAll("\\\\%","%");
        result = result.replaceAll("\\\\square\\b","\u25A1");
        result = result.replaceAll("\\$\\$","");
        result = result.replaceAll("\\$","");
        return result;
    }

    private String replaceAllFraction(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String num = m.group(1);
            String den = m.group(2);
            boolean needParen = num.matches(".*[-+*/].*") || den.matches(".*[-+*/].*");
            m.appendReplacement(sb, needParen ? "(" + java.util.regex.Matcher.quoteReplacement(num) + ")/(" + java.util.regex.Matcher.quoteReplacement(den) + ")" : num + "/" + den);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String getStudentName(Long studentId) {
        if (studentId == null) return "";
        return studentRepository.findById(studentId)
                .map(Student::getName).orElse("");
    }
}
