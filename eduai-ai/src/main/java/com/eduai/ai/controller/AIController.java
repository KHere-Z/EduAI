package com.eduai.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.eduai.ai.dto.ChatRequest;
import com.eduai.ai.dto.UploadResponse;
import com.eduai.ai.service.AIChatService;
import com.eduai.common.Result;
import com.eduai.common.annotation.RateLimit;
import com.eduai.common.storage.CosStorageService;
import com.eduai.security.service.PointService;
import com.eduai.security.service.impl.PointServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI Controller — 错题分析 / 试卷分析 & 文件上传
 * <p>
 * 对应 AGENTS.md 第十四章 14.3 节定义的 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIChatService aiChatService;
    private final PointService pointService;
    private final CosStorageService cosStorageService;

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    // ==================== 核心接口 ====================

    /**
     * 通用 AI 聊天（OpenAI 兼容，入参 { messages, imageUrl, systemPrompt }）
     * <p>
     * AIChat.vue「AI 聊天」通用问答页依赖此端点（解数学题/写作辅导/知识点讲解等）。
     * POST /api/v1/ai/chat
     */
    @PostMapping("/chat")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public CompletableFuture<Result<String>> chat(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_CHAT, "AI对话");
        log.info("POST /api/v1/ai/chat messages={} imageUrl={}",
                request.getMessages() != null ? request.getMessages().size() : 0,
                request.getImageUrl());
        return aiChatService.chat(request).thenApply(Result::ok);
    }

    /**
     * 通用 AI 聊天（流式 SSE）— 对应前端 ai.js 的 sendChatMessageStream（预留，待接线）
     * <p>
     * POST /api/v1/ai/chat/stream
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_CHAT, "AI对话(流式)");
        log.info("POST /api/v1/ai/chat/stream messages={}",
                request.getMessages() != null ? request.getMessages().size() : 0);
        return aiChatService.chatStream(request);
    }

    /**
     * AI 错题分析（带预设提示词）
     * <p>
     * POST /api/v1/ai/wrong-analysis
     */
    @PostMapping("/wrong-analysis")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public CompletableFuture<Result<String>> analyzeWrongQuestion(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_WRONG_ANALYSIS, "AI错题分析");
        log.info("POST /api/v1/ai/wrong-analysis messages={} imageUrl={}",
                request.getMessages() != null ? request.getMessages().size() : 0,
                request.getImageUrl());
        return aiChatService.analyzeWrongQuestion(request).thenApply(Result::ok);
    }

    /**
     * AI 错题分析（流式 SSE）
     * <p>
     * POST /api/v1/ai/wrong-analysis/stream
     */
    @PostMapping(value = "/wrong-analysis/stream", produces = "text/event-stream")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public SseEmitter analyzeWrongQuestionStream(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_WRONG_ANALYSIS, "AI错题分析");
        log.info("POST /api/v1/ai/wrong-analysis/stream messages={}",
                request.getMessages() != null ? request.getMessages().size() : 0);
        return aiChatService.analyzeWrongQuestionStream(request);
    }

    @PostMapping(value = "/exam-analysis/stream", produces = "text/event-stream")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public SseEmitter analyzeExamStream(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_EXAM_ANALYSIS, "AI试卷分析");
        log.info("POST /api/v1/ai/exam-analysis/stream messages={}",
                request.getMessages() != null ? request.getMessages().size() : 0);
        return aiChatService.analyzeExamStream(request);
    }

    /**
     * AI 试卷分析（带预设提示词）
     * <p>
     * POST /api/v1/ai/exam-analysis
     */
    @PostMapping("/exam-analysis")
    @RateLimit(limit = 10, windowSec = 60, message = "AI 调用过于频繁，请 1 分钟后再试")
    public CompletableFuture<Result<String>> analyzeExam(@RequestBody ChatRequest request) {
        pointService.consume(StpUtil.getLoginIdAsLong(),
                PointServiceImpl.COST_AI_EXAM_ANALYSIS, "AI试卷分析");
        log.info("POST /api/v1/ai/exam-analysis messages={} imageUrl={}",
                request.getMessages() != null ? request.getMessages().size() : 0,
                request.getImageUrl());
        return aiChatService.analyzeExam(request).thenApply(Result::ok);
    }

    /**
     * 文件上传 — 兼容两种格式：
     * <ul>
     *   <li>multipart/form-data：标准文件上传（Content-Type: multipart/form-data）</li>
     *   <li>JSON base64：{ "file": "base64...", "fileName": "photo.png" }（Content-Type: application/json）</li>
     * </ul>
     * POST /api/v1/ai/upload
     */
    @PostMapping("/upload")
    @RateLimit(limit = 30, windowSec = 60, message = "上传过于频繁，请 1 分钟后再试")
    public Result<UploadResponse> upload(HttpServletRequest request,
                                         @RequestParam(value = "file", required = false) MultipartFile file) {
        StpUtil.checkLogin();

        // ---- 方式 1：multipart/form-data ----
        if (file != null && !file.isEmpty()) {
            return handleMultipartUpload(file);
        }

        // ---- 方式 2：JSON base64 ----
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            return handleBase64Upload(request);
        }

        // 两种都不是
        return Result.fail("不支持的上传格式，请使用 multipart/form-data 或 application/json (base64)");
    }

    /**
     * 处理 multipart/form-data 文件上传
     */
    private Result<UploadResponse> handleMultipartUpload(MultipartFile file) {
        log.info("📎 multipart 上传: fileName={} size={}", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!isAllowedExtension(ext)) {
            return Result.fail("不支持的文件类型：" + ext + "（支持：jpg/png/gif/webp/pdf/doc/docx）");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            return Result.fail("文件大小不能超过 20MB");
        }

        try {
            return saveFile(file.getBytes(), originalName, ext);
        } catch (IOException e) {
            log.error("multipart 文件保存失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 处理 JSON base64 文件上传
     */
    @SuppressWarnings("unchecked")
    private Result<UploadResponse> handleBase64Upload(HttpServletRequest request) {
        try {
            // 先读原始请求体，方便诊断前端实际发送的格式
            String rawBody = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            log.info("📦 base64 上传原始请求体 (前300字符): {}", rawBody.length() > 300 ? rawBody.substring(0, 300) + "..." : rawBody);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> body = mapper.readValue(rawBody, Map.class);

            // file 字段可能是 String（base64）或嵌套对象
            Object fileObj = body.get("file");
            log.info("📦 fileObj 类型: {}, toString前50字符: {}",
                    fileObj != null ? fileObj.getClass().getSimpleName() : "null",
                    fileObj != null ? String.valueOf(fileObj).substring(0, Math.min(50, String.valueOf(fileObj).length())) : "null");

            String base64Data = null;
            if (fileObj instanceof String s) {
                base64Data = s;
            } else if (fileObj instanceof Map) {
                // 前端可能传 { file: { data/base64/base64Data/content: "...", name: "..." } }
                Map<String, Object> fileMap = (Map<String, Object>) fileObj;
                Object dataObj = fileMap.get("data");
                if (dataObj == null) dataObj = fileMap.get("base64");
                if (dataObj == null) dataObj = fileMap.get("base64Data");
                if (dataObj == null) dataObj = fileMap.get("content");
                if (dataObj == null) dataObj = fileMap.get("file"); // 递归
                base64Data = dataObj instanceof String s ? s : null;
            }

            String originalName = null;
            Object nameObj = body.get("fileName");
            if (nameObj instanceof String s) {
                originalName = s;
            } else {
                originalName = "image.png";
            }

            if (base64Data == null || base64Data.isBlank()) {
                log.warn("base64 上传 file 字段为空或格式不正确: type={}",
                        fileObj != null ? fileObj.getClass().getSimpleName() : "null");
                // file 是空对象 {} → 前端把 JS File 对象直接 JSON.stringify 了
                if (fileObj instanceof Map && ((Map<?, ?>) fileObj).isEmpty()) {
                    return Result.fail(
                            "文件上传失败：前端请使用 FormData 上传（multipart/form-data），" +
                                    "或先用 FileReader.readAsDataURL() 将文件转成 base64 后再发送 JSON。" +
                                    "错误原因：检测到 file 字段是空对象 {}，说明 JS File 对象被直接 JSON 序列化了。");
                }
                return Result.fail("缺少有效的 file 数据（base64 格式）");
            }

            // 处理 data URL 前缀：data:image/png;base64,xxxx
            String pureBase64 = base64Data;
            String ext = "png"; // 默认
            if (base64Data.startsWith("data:")) {
                int commaIdx = base64Data.indexOf(",");
                if (commaIdx > 0) {
                    String header = base64Data.substring(0, commaIdx);
                    pureBase64 = base64Data.substring(commaIdx + 1);
                    // 从 data:image/png;base64 中提取扩展名
                    if (header.contains("image/")) {
                        ext = header.substring(header.indexOf("image/") + 6).replace(";base64", "");
                    }
                }
            } else if (originalName.contains(".")) {
                ext = getExtension(originalName);
            }

            log.info("📦 base64 上传: fileName={}, dataLen={}", originalName, base64Data.length());

            if (pureBase64.length() > 30 * 1024 * 1024) { // base64 编码后约 30MB
                return Result.fail("文件大小不能超过 20MB");
            }

            byte[] bytes = Base64.getDecoder().decode(pureBase64);
            return saveFile(bytes, originalName, ext);

        } catch (IllegalArgumentException e) {
            log.error("base64 解码失败: {}", e.getMessage());
            return Result.fail("文件数据格式错误，请确保是有效的 base64 编码");
        } catch (IOException e) {
            log.error("base64 上传失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 保存文件到磁盘，返回访问 URL
     */
    private Result<UploadResponse> saveFile(byte[] bytes, String originalName, String ext) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String storedName = UUID.randomUUID().toString() + "." + ext;
        String key = "ai/" + dateDir + "/" + storedName;

        // 优先上传 COS，图片流量卸载到对象存储，减轻 ECS 带宽
        String cosUrl = cosStorageService.upload(bytes, key);
        if (cosUrl != null) {
            log.info("✅ 文件已上传 COS: {} → {} ({} bytes)", originalName, cosUrl, bytes.length);
            return Result.ok(new UploadResponse(cosUrl, originalName));
        }

        // COS 未配置 / 失败 → 回退本地磁盘
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = basePath.resolve("ai").resolve(dateDir);
        Path target = dir.resolve(storedName);

        // 双重确保父目录存在
        Files.createDirectories(dir);
        Files.createDirectories(target.getParent());

        Files.write(target, bytes);

        String url = "/uploads/ai/" + dateDir + "/" + storedName;
        log.info("✅ 文件保存成功(本地): {} → {} ({} bytes, 路径: {})",
                originalName, url, bytes.length, target.toAbsolutePath());

        return Result.ok(new UploadResponse(url, originalName));
    }

    // ==================== 内部方法 ====================

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String ext) {
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx" -> true;
            default -> false;
        };
    }
}