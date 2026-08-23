package com.eduai.ai.service;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.ai.config.DeepSeekConfig;
import com.eduai.ai.dto.ChatRequest;
import com.eduai.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/**
 * AI 聊天服务 — 调用 DeepSeek API（OpenAI 兼容格式）
 * <p>
 * 核心职责：接收前端对话历史 + 系统提示词 → 转发 DeepSeek → 返回 markdown 文本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${eduai.upload.dir:uploads}")
    private String uploadDir;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** 共享 Dispatcher：放大 AI 并发上限（默认每主机仅 5，会卡死 100+ 并发） */
    private static final Dispatcher AI_DISPATCHER = new Dispatcher();
    static {
        AI_DISPATCHER.setMaxRequests(300);          // 全局在途请求上限
        AI_DISPATCHER.setMaxRequestsPerHost(200);   // 单主机（DeepSeek/Doubao）并发上限
    }

    /** 共享 OkHttpClient（连接池复用，避免每次新建 TCP+TLS 握手）；DeepSeek 错题分析最多 10 分钟 */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .dispatcher(AI_DISPATCHER)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(200, 5, TimeUnit.MINUTES))
            .build();

    /** Doubao 专用 client（更长超时，独立连接池）；豆包试卷分析 5-10 分钟，读到 20 分钟 */
    private final OkHttpClient doubaoHttpClient = new OkHttpClient.Builder()
            .dispatcher(AI_DISPATCHER)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(1200, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(200, 5, TimeUnit.MINUTES))
            .build();

    /** 模型上下文上限（留 200K 余量给响应） */
    private static final long MAX_CONTEXT_TOKENS = 800_000L;
    /** 最少保留的消息对数 */
    private static final int MIN_MESSAGE_PAIRS = 3;
    /** 图片最大边长（超过则等比缩放） */
    private static final int MAX_IMAGE_DIMENSION = 2048;
    /** Doubao 图片最大边长（豆包上传较慢，更激进压缩） */
    private static final int DOUBAO_MAX_IMAGE_DIMENSION = 1024;
    /** Doubao API 地址（固定） */
    private static final String DOUBAO_API_URL = "https://ark.cn-beijing.volces.com/api/v3/responses";
    /** Doubao API Key 兜底（环境变量 DOUBAO_API_KEY 注入，勿硬编码） */
    @Value("${eduai.ai.doubao-api-key:}")
    private String doubaoFallbackKey;

    /** 加载 Doubao 专用 Key（通过 DeepSeekConfig.resolveModel 两级查找） */
    private String loadDoubaoKey() {
        Map<String, String> resolved = config.resolveModel("exam_analysis");
        String key = resolved.get("apiKey");
        if (key != null && !key.isBlank()) {
            log.debug("Doubao Key 来源: resolveModel(exam_analysis)");
            return key;
        }
        log.warn("Doubao Key 未配置（环境变量 DOUBAO_API_KEY 为空）");
        return doubaoFallbackKey;
    }

    /**
     * 执行 AI 调用（共享核心逻辑，不含 validate/autoDetect）。
     * 由 analyzeWrongQuestion / analyzeExam 等专用端点调用。
     */
    private String executeChat(ChatRequest request) {
        List<Map<String, Object>> messages = buildMessages(request);
        messages = trimMessages(messages);

        if (isDoubao(request.getModel())) {
            Map<String, Object> body = buildDoubaoBody(request, false);
            log.info("Doubao chat 请求: model={}, imageCount={}", getEffectiveModel(request.getModel()),
                    getImageUrls(request).size());
            return callDoubaoApi(body, getEffectiveApiKey(request.getApiKey()));
        }

        String effectiveModel = getEffectiveModel(request.getModel());
        String effectiveUrl = config.resolveApiUrl(effectiveModel);
        String effectiveKey = config.resolveApiKey(effectiveModel);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", effectiveModel);
        body.put("messages", messages);
        body.put("temperature", 1.0);
        body.put("max_tokens", 4096);
        body.put("stream", false);

        log.info("AI chat 请求: model={}, messagesCount={}, apiUrl={}",
                effectiveModel, messages.size(), effectiveUrl);
        return callDeepSeekApi(body, "/chat/completions", effectiveUrl, effectiveKey);
    }

    /**
     * 执行 AI 流式调用（共享核心逻辑，不含 validate/autoDetect）。
     * 由 analyzeWrongQuestionStream / analyzeExamStream 等专用端点调用。
     */
    private SseEmitter executeChatStream(ChatRequest request) {
        List<Map<String, Object>> messages = trimMessages(buildMessages(request));

        // Doubao 流式
        if (isDoubao(request.getModel())) {
            Map<String, Object> body = buildDoubaoBody(request, true);
            return callDoubaoApiStream(body, getEffectiveApiKey(request.getApiKey()));
        }

        String effectiveModel = getEffectiveModel(request.getModel());
        String effectiveUrl = config.resolveApiUrl(effectiveModel);
        String effectiveKey = config.resolveApiKey(effectiveModel);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", effectiveModel);
        body.put("messages", messages);
        body.put("temperature", 1.0);
        body.put("max_tokens", 4096);
        body.put("stream", true);

        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟超时（错题分析/通用聊天，DeepSeek）

        try {
            String normalizedUrl = effectiveUrl.replaceAll("/+$", "");
            String fullUrl = normalizedUrl + "/chat/completions";
            String jsonBody = objectMapper.writeValueAsString(body);

            log.info("SSE 流式请求: model={}, url={}, messagesCount={}, bodySize={}",
                    effectiveModel, effectiveUrl,
                    messages.size(), jsonBody.length());

            okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + effectiveKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    log.error("SSE 流式中断: {}", e.getMessage());
                    safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
                    emitter.complete();
                }
                @Override public void onResponse(Call call, Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = responseBody != null ? responseBody.string() : "";
                            log.error("SSE 流式请求失败: HTTP {} body={}", response.code(),
                                    errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody);
                            safeEmit(emitter, "[ERROR] AI 调用失败：" + parseError(errorBody));
                            emitter.complete();
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        int tokenCount = 0;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ") && !"data: [DONE]".equals(line.trim())) {
                                String data = line.substring(6);
                                try {
                                    var node = objectMapper.readTree(data);
                                    var choices = node.get("choices");
                                    if (choices != null && !choices.isEmpty()) {
                                        var delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String content = delta.get("content").asText();
                                            if (!content.isEmpty()) {
                                                // JSON 编码 token，保留 markdown 换行/特殊字符，前端 JSON.parse 还原
                                                String safe = objectMapper.writeValueAsString(content);
                                                emitter.send(SseEmitter.event().data(safe));
                                                tokenCount++;
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // 跳过无法解析的行
                                }
                            }
                        }
                        log.info("SSE 流式完成: {} tokens", tokenCount);
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("SSE 流式中断: {}", e.getMessage());
                        safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE 流式异常: {}", e.getMessage(), e);
                        safeEmit(emitter, "[ERROR] " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("SSE 流式请求构造失败: {}", e.getMessage());
            safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    // ==================== 模型路由 ====================

    /** 获取生效的模型名（请求级 > 全局配置） */
    private String getEffectiveModel(String requestModel) {
        return (requestModel != null && !requestModel.isBlank())
                ? requestModel : config.getEffectiveModel();
    }

    /** 获取生效的 API URL（请求级 > 全局配置） */
    private String getEffectiveApiUrl(String requestUrl) {
        return (requestUrl != null && !requestUrl.isBlank())
                ? requestUrl : config.getEffectiveApiUrl();
    }

    /** 获取生效的 API Key（请求级 > 全局配置） */
    private String getEffectiveApiKey(String requestKey) {
        return (requestKey != null && !requestKey.isBlank())
                ? requestKey : config.getApiKey();
    }

    /** 判断是否使用 Doubao 格式（请求级 model > 全局配置） */
    private boolean isDoubao(String requestModel) {
        String model = getEffectiveModel(requestModel);
        return model != null && (model.contains("doubao") || model.startsWith("ep-"));
    }

    /** 判断是否使用 Doubao 格式（无请求 model，仅看全局配置） */
    private boolean isDoubao() {
        return isDoubao(null);
    }

    /**
     * 构建 Doubao API 请求体（按 spec 格式）
     * <p>
     * 格式: {"model":"...", "input":[{system}, {user with content array}]}
     */
    private Map<String, Object> buildDoubaoBody(ChatRequest request, boolean stream) {
        List<Map<String, Object>> input = new ArrayList<>();

        // System prompt
        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是安文AI教育的智能学习助手，擅长K12全学科辅导、错题分析和解题技巧。请用中文回答，回答简洁明了。";
        }
        input.add(Map.of("role", "system", "content", systemPrompt));

        // 最后一条 user 消息：文本 + 图片
        List<ChatRequest.Message> reqMessages = request.getMessages();
        String userText = "";
        if (reqMessages != null && !reqMessages.isEmpty()) {
            ChatRequest.Message lastMsg = reqMessages.getLast();
            if ("user".equals(lastMsg.getRole())) {
                userText = lastMsg.getContent() != null ? lastMsg.getContent() : "";
            }
        }

        List<String> imageUrls = getImageUrls(request);

        List<Map<String, Object>> content = new ArrayList<>();
        // 多图支持（Doubao vision 格式）
        for (String url : imageUrls) {
            String resolvedUrl = resolveImageUrl(url, DOUBAO_MAX_IMAGE_DIMENSION);
            if (resolvedUrl != null) {
                content.add(Map.of("type", "input_image", "image_url", resolvedUrl));
            }
        }
        content.add(Map.of("type", "input_text", "text", userText));
        input.add(Map.of("role", "user", "content", content));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", getEffectiveModel(request.getModel()));
        body.put("input", input);
        // 关闭深度思考：seed 系列默认开启推理（吐 reasoning 事件 + 拖慢 30s+）。
        // 错题分析/试卷分析需要快速直接作答，推理内容本就被 extractDoubaoToken 过滤丢弃，
        // thinking=disabled 强制跳过推理阶段，直接输出正文。
        body.put("thinking", Map.of("type", "disabled"));
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    /**
     * 调用 Doubao API（非流式）
     */
    @SuppressWarnings("unchecked")
    private String callDoubaoApi(Map<String, Object> body, String apiKey) {
        // 如果不是 Doubao key 格式(ark-xxx)，尝试从 ai_config 读，再兜底硬编码
        if (apiKey == null || !apiKey.startsWith("ark-")) {
            log.warn("Doubao Key 格式不正确(当前={})，尝试加载正确 Key...",
                    apiKey == null ? "null" : apiKey.substring(0, Math.min(8, apiKey.length())) + "***");
            apiKey = loadDoubaoKey();
        }
        log.info("Doubao 实际使用 Key: {}***", apiKey.substring(0, Math.min(8, apiKey.length())));

        // URL 优先从 ai_models 解析，兜底硬编码
        String fullUrl = DOUBAO_API_URL;
        try {
            String resolvedUrl = config.resolveModel("exam_analysis").get("apiUrl");
            if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                fullUrl = resolvedUrl;
            }
        } catch (Exception ignored) {}

        OkHttpClient client = doubaoHttpClient;

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Doubao 请求体大小: {} bytes", jsonBody.length());

            okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            try (okhttp3.Response response = client.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("Doubao 响应: HTTP {} ({} bytes)", response.code(), responseBody.length());

                if (!response.isSuccessful()) {
                    log.error("Doubao API 返回错误: HTTP {} body={}", response.code(),
                            responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
                    throw new BusinessException(500, "AI 调用失败：" + parseError(responseBody));
                }

                String content = parseDoubaoContent(responseBody);
                logTokenUsage(responseBody, body.get("model"));
                return content;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("Doubao 网络异常: {}", e.getMessage(), e);
            throw new BusinessException(500, "AI 调用失败：网络异常 — " + e.getMessage());
        } catch (Exception e) {
            log.error("Doubao 调用异常: {}", e.getMessage(), e);
            throw new BusinessException(500, "AI 调用失败：" + e.getMessage());
        }
    }

    /**
     * 调用 Doubao API（非流式，异步 enqueue）— 不占用线程阻塞等待，AI 并发由 OkHttp dispatcher 管
     */
    private CompletableFuture<String> callDoubaoApiAsync(Map<String, Object> body, String apiKey) {
        if (apiKey == null || !apiKey.startsWith("ark-")) {
            apiKey = loadDoubaoKey();
        }
        final String resolvedKey = apiKey;

        String fullUrl = DOUBAO_API_URL;
        try {
            String resolvedUrl = config.resolveModel("exam_analysis").get("apiUrl");
            if (resolvedUrl != null && !resolvedUrl.isBlank()) {
                fullUrl = resolvedUrl;
            }
        } catch (Exception ignored) {}

        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            Request httpRequest = new Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + resolvedKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            doubaoHttpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    log.error("Doubao 网络异常: {}", e.getMessage(), e);
                    future.completeExceptionally(new BusinessException(500, "AI 调用失败：网络异常 — " + e.getMessage()));
                }
                @Override public void onResponse(Call call, Response response) {
                    try (ResponseBody rb = response.body()) {
                        String responseBody = rb != null ? rb.string() : "";
                        log.info("Doubao 响应: HTTP {} ({} bytes)", response.code(), responseBody.length());
                        if (!response.isSuccessful()) {
                            log.error("Doubao API 返回错误: HTTP {} body={}", response.code(),
                                    responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
                            future.completeExceptionally(new BusinessException(500, "AI 调用失败：" + parseError(responseBody)));
                            return;
                        }
                        String content = parseDoubaoContent(responseBody);
                        logTokenUsage(responseBody, body.get("model"));
                        future.complete(content);
                    } catch (Exception e) {
                        log.error("Doubao 异步响应处理异常: {}", e.getMessage(), e);
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 调用 Doubao API（流式 SSE）
     */
    @SuppressWarnings("unchecked")
    private SseEmitter callDoubaoApiStream(Map<String, Object> body, String apiKey) {
        // 与非流式一致：非 ark- 格式的 key 尝试从 ai_config 重新加载正确 Key
        if (apiKey == null || !apiKey.startsWith("ark-")) {
            apiKey = loadDoubaoKey();
        }
        final String resolvedKey = apiKey;
        String fullUrl = DOUBAO_API_URL;
        SseEmitter emitter = new SseEmitter(1_200_000L); // 20 分钟（试卷分析 5-10 分钟，Doubao 较慢）

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("Doubao SSE 流式请求: bodySize={}", jsonBody.length());

            okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + resolvedKey)
                    .addHeader("Content-Type", "application/json")
                    .post(okhttp3.RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            doubaoHttpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    log.error("Doubao SSE 流式中断: {}", e.getMessage());
                    safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
                    emitter.complete();
                }
                @Override public void onResponse(Call call, Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = responseBody != null ? responseBody.string() : "";
                            log.error("Doubao SSE 请求失败: HTTP {} body={}", response.code(),
                                    errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody);
                            safeEmit(emitter, "[ERROR] AI 调用失败：" + parseError(errorBody));
                            emitter.complete();
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8));
                        String line;
                        int tokenCount = 0;
                        boolean firstToken = true;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ") && !"data: [DONE]".equals(line.trim())) {
                                long parseStart = System.currentTimeMillis();
                                String data = line.substring(6);
                                String token = extractDoubaoToken(data);
                                if (token != null && !token.isEmpty()) {
                                    // JSON 编码 token，保留 markdown 换行/特殊字符，前端 JSON.parse 还原
                                    String safe = objectMapper.writeValueAsString(token);
                                    emitter.send(SseEmitter.event().data(safe));
                                    long elapsed = System.currentTimeMillis() - parseStart;
                                    if (firstToken) {
                                        firstToken = false;
                                        log.info("⏱ Doubao SSE 首token: parse+send {}ms, token={}",
                                                elapsed, safe.length() > 30 ? safe.substring(0, 30) + "..." : safe);
                                    }
                                    tokenCount++;
                                } else {
                                    log.trace("Doubao SSE 非token事件: {}",
                                            data.length() > 100 ? data.substring(0, 100) : data);
                                }
                            }
                        }
                        log.info("Doubao SSE 流式完成: {} tokens", tokenCount);
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("Doubao SSE 流式中断: {}", e.getMessage());
                        safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Doubao SSE 流式异常: {}", e.getMessage(), e);
                        safeEmit(emitter, "[ERROR] " + e.getMessage());
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("Doubao SSE 流式请求构造失败: {}", e.getMessage());
            safeEmit(emitter, "[ERROR] 网络异常：" + e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    /** 安全发送 SSE 事件（忽略连接关闭等异常） */
    private void safeEmit(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException ignored) {}
    }

    /**
     * 从 Doubao SSE 数据块中提取 token 文本
     * <p>
     * 尝试多种路径: output[0].content[0].text / output[0].delta.text / delta.content
     */
    @SuppressWarnings("unchecked")
    private String extractDoubaoToken(String data) {
        try {
            var node = objectMapper.readTree(data);
            // 推理模型的 reasoning 事件（思考过程）不是正文，跳过，只保留 output_text 正文
            if (node.has("type")) {
                String type = node.get("type").asText();
                if (type.contains("reasoning")) {
                    return null;
                }
            }
            // 路径1: 顶层 delta（output_text 等正文事件）
            var delta = node.get("delta");
            if (delta != null && delta.isTextual()) {
                return delta.asText();
            }
            var output = node.get("output");
            if (output != null && output.isArray() && !output.isEmpty()) {
                for (var block : output) {
                    if (!"message".equals(block.has("type") ? block.get("type").asText() : "")) continue;
                    var content = block.get("content");
                    if (content != null && content.isArray() && !content.isEmpty()) {
                        var text = content.get(0).get("text");
                        if (text != null) return text.asText();
                    }
                    var blockDelta = block.get("delta");
                    if (blockDelta != null && blockDelta.has("text")) {
                        return blockDelta.get("text").asText();
                    }
                }
            }
            // fallback: OpenAI 格式
            var choices = node.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                var choiceDelta = choices.get(0).get("delta");
                if (choiceDelta != null && choiceDelta.has("content")) {
                    return choiceDelta.get("content").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 解析 Doubao 响应: output[0].content[0].text
     */
    @SuppressWarnings("unchecked")
    private String parseDoubaoContent(String responseBody) {
        try {
            Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> output = (List<Map<String, Object>>) map.get("output");
            if (output == null || output.isEmpty()) {
                log.error("Doubao 响应无 output: {}", responseBody);
                throw new BusinessException(500, "AI 返回异常：无有效回复");
            }
            // 遍历 output 数组，取 type="message" 的块
            for (Map<String, Object> block : output) {
                if (!"message".equals(block.get("type"))) continue;
                Object content = block.get("content");
                if (content instanceof List<?> list && !list.isEmpty()) {
                    Object firstBlock = list.getFirst();
                    if (firstBlock instanceof Map<?, ?> fb && fb.get("text") instanceof String text) {
                        return text;
                    }
                }
            }
            log.error("Doubao 响应格式未知: {}", responseBody);
            throw new BusinessException(500, "AI 返回异常：无法解析回复");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 Doubao 响应失败: {}", responseBody, e);
            throw new BusinessException(500, "AI 返回异常：" + e.getMessage());
        }
    }

    // ==================== 提取的共享方法 ====================

    /** 校验请求合法性 */
    private void validate(ChatRequest request) {
        StpUtil.checkLogin();
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException("消息不能为空");
        }
    }

    /** 合并单图(imageUrl)和多图(imageUrls)字段，返回非空图片列表 */
    private List<String> getImageUrls(ChatRequest request) {
        List<String> urls = new ArrayList<>();
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            urls.add(request.getImageUrl());
        }
        if (request.getImageUrls() != null) {
            for (String u : request.getImageUrls()) {
                if (u != null && !u.isBlank()) urls.add(u);
            }
        }
        return urls;
    }

    /** 构建消息列表（文本 + Vision） */
    private List<Map<String, Object>> buildMessages(ChatRequest request) {
        List<String> images = getImageUrls(request);
        boolean hasImage = !images.isEmpty();
        List<Map<String, Object>> messages = new ArrayList<>();

        String systemPrompt = request.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "你是安文AI教育的智能学习助手，擅长K12全学科辅导、错题分析和解题技巧。请用中文回答，回答简洁明了。";
        }
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (int idx = 0; idx < request.getMessages().size(); idx++) {
            ChatRequest.Message msg = request.getMessages().get(idx);
            String role = msg.getRole();
            if ("system".equals(role)) continue;

            String text = msg.getContent() != null ? msg.getContent() : "";
            boolean isLastUser = "user".equals(role) && idx == request.getMessages().size() - 1;

            if (hasImage && isLastUser) {
                messages.add(buildVisionMessage(role, text, images));
            } else {
                messages.add(Map.of("role", role, "content", text));
            }
        }

        if (hasImage && messages.stream().noneMatch(m -> "user".equals(m.get("role")))) {
            messages.add(buildVisionMessage("user", "请分析这张图片", images));
        }

        return messages;
    }

    /**
     * 估算消息列表的总 token 数
     * <p>
     * 文本: 2 字符 ≈ 1 token
     * 图片: base64 数据每 1.5 字节 ≈ 1 token（DeepSeek Vision 图片按数据量计费）
     */
    private long estimateTokens(List<Map<String, Object>> messages) {
        long total = 0;
        for (Map<String, Object> msg : messages) {
            Object content = msg.get("content");
            if (content instanceof String s) {
                total += s.length() / 2;
            } else if (content instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> block) {
                        if (block.get("text") instanceof String t) {
                            total += t.length() / 2;
                        }
                        if (block.get("image_url") instanceof Map<?, ?> img
                                && img.get("url") instanceof String url
                                && url.contains("base64,")) {
                            int base64Start = url.indexOf("base64,") + 7;
                            total += (url.length() - base64Start) / 2; // base64 每 2 字符 ≈ 1 字节 ≈ 1 token
                        }
                    }
                }
            }
        }
        return total;
    }

    /**
     * 裁剪消息历史，确保不超过模型上下文上限
     * <p>
     * 策略：保留 system 消息 + 最后 N 对 user/assistant 消息，从前面删除最旧的消息
     */
    private List<Map<String, Object>> trimMessages(List<Map<String, Object>> messages) {
        if (messages.isEmpty()) return messages;

        long estimatedTokens = estimateTokens(messages);
        if (estimatedTokens <= MAX_CONTEXT_TOKENS) {
            log.debug("消息 token 估算: {} ≤ {}, 无需裁剪", estimatedTokens, MAX_CONTEXT_TOKENS);
            return messages;
        }

        log.warn("⚠️ 消息 token 估算 {} 超过上限 {}，开始裁剪...", estimatedTokens, MAX_CONTEXT_TOKENS);

        // 找出 system 消息，其他消息按 user/assistant 对裁剪
        List<Map<String, Object>> nonSystem = new ArrayList<>();
        Map<String, Object> systemMsg = null;
        for (Map<String, Object> msg : messages) {
            if ("system".equals(msg.get("role"))) {
                systemMsg = msg;
            } else {
                nonSystem.add(msg);
            }
        }

        // 至少保留最后 N 对消息（MIN_MESSAGE_PAIRS * 2 条）
        int keepCount = Math.min(MIN_MESSAGE_PAIRS * 2, nonSystem.size());
        List<Map<String, Object>> kept = new ArrayList<>(nonSystem.subList(nonSystem.size() - keepCount, nonSystem.size()));

        // 从保留消息向前逐步添加，直到接近上限的 80%
        long targetTokens = (long) (MAX_CONTEXT_TOKENS * 0.8);
        long currentTokens = estimateTokens(kept);
        int startIdx = nonSystem.size() - keepCount - 1;

        while (startIdx >= 0 && currentTokens < targetTokens) {
            Map<String, Object> msg = nonSystem.get(startIdx);
            kept.addFirst(msg);
            currentTokens = estimateTokens(kept);
            startIdx--;
        }

        // 组装最终消息列表：system + 裁剪后的消息
        List<Map<String, Object>> result = new ArrayList<>();
        if (systemMsg != null) {
            result.add(systemMsg);
        }
        result.addAll(kept);

        long finalTokens = estimateTokens(result);
        log.info("✂️ 消息裁剪完成: {} 条 → {} 条, token 估算: {} → {}",
                messages.size(), result.size(), estimatedTokens, finalTokens);
        return result;
    }

    /**
     * 通用 AI 聊天 — 通用问答场景（解数学题 / 写作辅导 / 知识点讲解 / 学习方法等）。
     * <p>
     * 不预设场景提示词、不强制路由到特定模块：使用全局配置模型，前端可传入自定义
     * systemPrompt 和可选 base64 图片（imageUrl）。对应 AIChat.vue「AI 聊天」页。
     */
    public CompletableFuture<String> chat(ChatRequest request) {
        return executeChatAsync(request);
    }

    /**
     * 通用 AI 聊天（流式 SSE）— 通用问答场景，不预设场景提示词。
     * 对应前端 ai.js 的 sendChatMessageStream（预留，待前端接线）。
     */
    public SseEmitter chatStream(ChatRequest request) {
        return executeChatStream(request);
    }

    /**
     * 错题分析 — 带错题场景预设提示词，自动路由到 wrong_analysis 模块的模型
     */
    public CompletableFuture<String> analyzeWrongQuestion(ChatRequest request) {
        if (request.getSystemPrompt() == null || request.getSystemPrompt().isBlank()) {
            request.setSystemPrompt(
                    "你是资深K12学科老师，专门做错题分析。收到学生错题后，请按以下格式分析：\n" +
                            "1. **错误根因**：分析学生为什么会错\n" +
                            "2. **正确解法**：给出完整解题步骤\n" +
                            "3. **知识点梳理**：列出本题涉及的知识点\n" +
                            "4. **举一反三**：给出1-2道类似题目\n" +
                            "请用中文回答，格式清晰，适合学生阅读。"
            );
        }
        // 专用端点始终使用后端配置的模型，不受前端传参影响
        Map<String, String> resolved = config.resolveModel("wrong_analysis");
        request.setModel(resolved.get("model"));
        request.setApiUrl(resolved.get("apiUrl"));
        request.setApiKey(resolved.get("apiKey"));
        log.info("📝 analyzeWrongQuestion 路由: resolveModel(wrong_analysis) → model={}, url={}, keyPrefix={}",
                resolved.get("model"), resolved.get("apiUrl"),
                resolved.get("apiKey") != null ? resolved.get("apiKey").substring(0, Math.min(8, resolved.get("apiKey").length())) + "***" : "NULL");
        return executeChatAsync(request);
    }

    /**
     * 流式错题分析（SSE）— 自动路由到 wrong_analysis 模块的模型
     */
    public SseEmitter analyzeWrongQuestionStream(ChatRequest request) {
        if (request.getSystemPrompt() == null || request.getSystemPrompt().isBlank()) {
            request.setSystemPrompt(
                    "你是资深K12学科老师，专门做错题分析。收到学生错题后，请按以下格式分析：\n" +
                            "1. **错误根因**：分析学生为什么会错\n" +
                            "2. **正确解法**：给出完整解题步骤\n" +
                            "3. **知识点梳理**：列出本题涉及的知识点\n" +
                            "4. **举一反三**：给出1-2道类似题目\n" +
                            "请用中文回答，格式清晰，适合学生阅读。"
            );
        }
        // 专用端点始终使用后端配置的模型，不受前端传参影响
        Map<String, String> resolved = config.resolveModel("wrong_analysis");
        request.setModel(resolved.get("model"));
        request.setApiUrl(resolved.get("apiUrl"));
        request.setApiKey(resolved.get("apiKey"));
        log.info("📝 analyzeWrongQuestionStream 路由: resolveModel(wrong_analysis) → model={}, url={}, keyPrefix={}",
                resolved.get("model"), resolved.get("apiUrl"),
                resolved.get("apiKey") != null ? resolved.get("apiKey").substring(0, Math.min(8, resolved.get("apiKey").length())) + "***" : "NULL");
        return executeChatStream(request);
    }

    /**
     * 试卷分析 — 带试卷场景预设提示词，自动路由到 exam_analysis 模块的模型
     */
    public CompletableFuture<String> analyzeExam(ChatRequest request) {
        if (request.getSystemPrompt() == null || request.getSystemPrompt().isBlank()) {
            request.setSystemPrompt(
                    "你是资深K12学科老师，专门做试卷分析。收到学生试卷后，请按以下格式分析：\n" +
                            "1. **扣分分布**：按知识点/题型统计扣分\n" +
                            "2. **薄弱环节**：指出需要加强的知识点\n" +
                            "3. **提升建议**：给出具体的学习计划\n" +
                            "4. **目标预估**：下次考试可能的提升空间\n" +
                            "请用中文回答，数据要具体、建议要可操作。"
            );
        }
        // 专用端点始终使用后端配置的模型，不受前端传参影响
        Map<String, String> resolved = config.resolveModel("exam_analysis");
        request.setModel(resolved.get("model"));
        request.setApiUrl(resolved.get("apiUrl"));
        request.setApiKey(resolved.get("apiKey"));
        log.info("📝 analyzeExam 路由: resolveModel(exam_analysis) → model={}, url={}, keyPrefix={}",
                resolved.get("model"), resolved.get("apiUrl"),
                resolved.get("apiKey") != null ? resolved.get("apiKey").substring(0, Math.min(8, resolved.get("apiKey").length())) + "***" : "NULL");
        return executeChatAsync(request);
    }

    /**
     * 流式试卷分析（SSE）— 自动路由到 exam_analysis 模块的模型
     */
    public SseEmitter analyzeExamStream(ChatRequest request) {
        if (request.getSystemPrompt() == null || request.getSystemPrompt().isBlank()) {
            request.setSystemPrompt(
                    "你是资深K12学科老师，专门做试卷分析。收到学生试卷后，请按以下格式分析：\n" +
                            "1. **扣分分布**：按知识点/题型统计扣分\n" +
                            "2. **薄弱环节**：指出需要加强的知识点\n" +
                            "3. **提升建议**：给出具体的学习计划\n" +
                            "4. **目标预估**：下次考试可能的提升空间\n" +
                            "请用中文回答，数据要具体、建议要可操作。"
            );
        }
        // 专用端点始终使用后端配置的模型，不受前端传参影响
        Map<String, String> resolved = config.resolveModel("exam_analysis");
        request.setModel(resolved.get("model"));
        request.setApiUrl(resolved.get("apiUrl"));
        request.setApiKey(resolved.get("apiKey"));
        log.info("📝 analyzeExamStream 路由: resolveModel(exam_analysis) → model={}, url={}, keyPrefix={}",
                resolved.get("model"), resolved.get("apiUrl"),
                resolved.get("apiKey") != null ? resolved.get("apiKey").substring(0, Math.min(8, resolved.get("apiKey").length())) + "***" : "NULL");
        return executeChatStream(request);
    }

    /**
     * 题目 AI 批改 — 判断学生作答是否正确并给出批改说明。
     * <p>
     * <b>必须同步</b>（不加 {@code @Async}）：调用方 {@code QuestionBankServiceImpl.gradeQuestion}
     * 在同一事务内先扣点、再调本方法，等待 AI 返回后决定 COMMIT/ROLLBACK。异步会脱离事务，
     * AI 失败也无法回滚扣点。返回约定 JSON 文本，由调用方解析 {@code {correct, result}}。
     * <p>
     * 复用 {@code wrong_analysis} 模块已配的 vision 模型，不新增 ai_config 配置。
     */
    public String gradeQuestion(ChatRequest request) {
        request.setSystemPrompt(
                "你是资深K12学科老师，负责批改学生提交的题目作答。\n" +
                        "请严格判断学生答案是否正确，并给出简洁的批改说明。\n" +
                        "只输出一个 JSON 对象，不要输出任何其他文字或 markdown 代码块，格式如下：\n" +
                        "{\"correct\": true或false, \"result\": \"批改说明，指出对错原因和正确解法（若错误）\"}\n" +
                        "correct 为布尔值：true 表示作答正确，false 表示作答错误。"
        );
        // 专用端点始终使用后端配置的模型，不受前端传参影响
        Map<String, String> resolved = config.resolveModel("wrong_analysis");
        request.setModel(resolved.get("model"));
        request.setApiUrl(resolved.get("apiUrl"));
        request.setApiKey(resolved.get("apiKey"));
        log.info("📝 gradeQuestion 路由: resolveModel(wrong_analysis) → model={}, url={}, keyPrefix={}",
                resolved.get("model"), resolved.get("apiUrl"),
                resolved.get("apiKey") != null ? resolved.get("apiKey").substring(0, Math.min(8, resolved.get("apiKey").length())) + "***" : "NULL");
        return executeChat(request);
    }

    // ==================== 内部方法 ====================

    /**
     * 执行 AI 调用（异步 enqueue，不占线程阻塞等待）
     * <p>
     * 非流式 chat/analyzeWrongQuestion/analyzeExam 走此路径；gradeQuestion 仍需同步阻塞（同事务回滚扣点），不走这里。
     */
    private CompletableFuture<String> executeChatAsync(ChatRequest request) {
        List<Map<String, Object>> messages = trimMessages(buildMessages(request));

        if (isDoubao(request.getModel())) {
            Map<String, Object> body = buildDoubaoBody(request, false);
            log.info("Doubao chat 异步请求: model={}, imageCount={}",
                    getEffectiveModel(request.getModel()), getImageUrls(request).size());
            return callDoubaoApiAsync(body, getEffectiveApiKey(request.getApiKey()));
        }

        String effectiveModel = getEffectiveModel(request.getModel());
        String effectiveUrl = config.resolveApiUrl(effectiveModel);
        String effectiveKey = config.resolveApiKey(effectiveModel);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", effectiveModel);
        body.put("messages", messages);
        body.put("temperature", 1.0);
        body.put("max_tokens", 4096);
        body.put("stream", false);

        log.info("AI chat 异步请求: model={}, messagesCount={}, apiUrl={}",
                effectiveModel, messages.size(), effectiveUrl);
        return callDeepSeekApiAsync(body, "/chat/completions", effectiveUrl, effectiveKey);
    }

    /**
     * 调用 DeepSeek Chat Completions API（OpenAI 兼容，异步 enqueue）
     * <p>
     * 不占线程阻塞等待，AI 并发由 OkHttp dispatcher 管。
     */
    private CompletableFuture<String> callDeepSeekApiAsync(Map<String, Object> body, String endpoint, String baseUrl, String apiKey) {
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        String normalizedEndpoint = endpoint.replaceAll("^/+", "/");
        String fullUrl = normalizedBase + normalizedEndpoint;

        log.info("🚀 调用 AI API(异步): url={}, model={}, apiKeyPrefix={}",
                fullUrl, body.get("model"),
                apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "***" : "NULL");

        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("AI API 请求体大小: {} bytes", jsonBody.length());

            Request httpRequest = new Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    log.error("AI API 网络异常: {}", e.getMessage(), e);
                    future.completeExceptionally(new BusinessException(500, "AI 调用失败：网络异常 — " + e.getMessage()));
                }
                @Override public void onResponse(Call call, Response response) {
                    try (ResponseBody responseBody = response.body()) {
                        String bodyStr = responseBody != null ? responseBody.string() : "";
                        log.info("AI API 响应: HTTP {} ({} bytes)", response.code(), bodyStr.length());
                        if (!response.isSuccessful()) {
                            if (response.code() == 400 && bodyStr.contains("unknown variant `image_url`")) {
                                log.warn("⚠️ 当前模型不支持图片识别（Vision）：请求中携带图片，且 OCR 服务不可用");
                                future.completeExceptionally(new BusinessException(500, "题目图片识别失败，请确认 OCR 服务正常后重试，或手动输入题目文字"));
                                return;
                            }
                            log.error("AI API 返回错误: HTTP {} body={}", response.code(),
                                    bodyStr.length() > 500 ? bodyStr.substring(0, 500) + "..." : bodyStr);
                            future.completeExceptionally(new BusinessException(500, "AI 调用失败：" + parseError(bodyStr)));
                            return;
                        }
                        String content = parseContent(bodyStr);
                        logTokenUsage(bodyStr, body.get("model"));
                        log.info("AI API 返回内容长度: {} chars", content.length());
                        future.complete(content);
                    } catch (Exception e) {
                        log.error("AI API 异步响应处理异常: {}", e.getMessage(), e);
                        future.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 调用 DeepSeek Chat Completions API（OpenAI 兼容）
     * <p>
     * 如果 DeepSeek 模型不支持 Vision（image_url），自动降级为纯文本并重试一次。
     */
    @SuppressWarnings("unchecked")
    private String callDeepSeekApi(Map<String, Object> body, String endpoint, String baseUrl, String apiKey) {
        // 防止双斜杠：去掉 baseUrl 末尾的 /，去掉 endpoint 开头的 /
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        String normalizedEndpoint = endpoint.replaceAll("^/+", "/");
        String fullUrl = normalizedBase + normalizedEndpoint;

        log.info("🚀 调用 AI API: url={}, model={}, apiKeyPrefix={}",
                fullUrl, body.get("model"),
                apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "***" : "NULL");

        OkHttpClient client = httpClient;

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("AI API 请求体大小: {} bytes", jsonBody.length());

            Request httpRequest = new Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.getBytes(StandardCharsets.UTF_8), JSON))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("AI API 响应: HTTP {} ({} bytes)", response.code(), responseBody.length());

                if (!response.isSuccessful()) {
                    // DeepSeek 纯文本模型不支持图片（image_url）——说明请求中携带了图片且 OCR 未先识别成功，
                    // 继续降级重试也拿不到图片内容，直接给出明确错误让前端/用户知道是 OCR 环节的问题
                    if (response.code() == 400 && responseBody.contains("unknown variant `image_url`")) {
                        log.warn("⚠️ 当前模型不支持图片识别（Vision）：请求中携带图片，且 OCR 服务不可用");
                        throw new BusinessException(500, "题目图片识别失败，请确认 OCR 服务正常后重试，或手动输入题目文字");
                    }

                    log.error("AI API 返回错误: HTTP {} body={}", response.code(),
                            responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
                    throw new BusinessException(500, "AI 调用失败：" + parseError(responseBody));
                }

                String content = parseContent(responseBody);
                logTokenUsage(responseBody, body.get("model"));
                log.info("AI API 返回内容长度: {} chars", content.length());
                return content;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("AI API 网络异常: {}", e.getMessage(), e);
            throw new BusinessException(500, "AI 调用失败：网络异常 — " + e.getMessage());
        } catch (Exception e) {
            log.error("AI API 未预期的异常: {}", e.getMessage(), e);
            throw new BusinessException(500, "AI 调用失败：" + e.getMessage());
        }
    }

    /**
     * 解析 DeepSeek 响应，提取 choices[0].message.content
     */
    @SuppressWarnings("unchecked")
    private String parseContent(String responseBody) {
        try {
            Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("AI 响应无 choices: {}", responseBody);
                throw new BusinessException(500, "AI 返回异常：无有效回复");
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.error("AI 响应无 message: {}", responseBody);
                throw new BusinessException(500, "AI 返回异常：消息为空");
            }
            Object content = message.get("content");
            return content != null ? content.toString() : "";
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", responseBody, e);
            throw new BusinessException(500, "AI 返回异常：" + e.getMessage());
        }
    }

    /**
     * 解析错误信息
     */
    private String parseError(String responseBody) {
        try {
            Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
            Object err = map.get("error");
            if (err instanceof Map) {
                Object msg = ((Map<?, ?>) err).get("message");
                return msg != null ? msg.toString() : responseBody;
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
        }
    }

    /**
     * 从响应体中提取并打印 token 消耗日志
     * <p>
     * 兼容 OpenAI 格式 (prompt_tokens/completion_tokens/total_tokens)
     * 和 Doubao Volcano Ark 格式 (input_tokens/output_tokens/total_tokens)
     */
    @SuppressWarnings("unchecked")
    private void logTokenUsage(String responseBody, Object model) {
        try {
            Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
            Object usageObj = map.get("usage");
            if (usageObj instanceof Map<?, ?> usage) {
                long prompt = extractTokenField(usage, "prompt_tokens", "input_tokens");
                long completion = extractTokenField(usage, "completion_tokens", "output_tokens");
                long total = extractTokenField(usage, "total_tokens");

                log.info("💰 Token 消耗: model={} | prompt={} completion={} total={}",
                        model != null ? model.toString() : "unknown",
                        prompt, completion, total);
            } else {
                log.debug("响应中无 usage 字段，无法统计 token 消耗, model={}", model);
            }
        } catch (Exception e) {
            log.debug("解析 token 用量失败: {}", e.getMessage());
        }
    }

    /** 从 usage Map 中按优先级尝试多个字段名提取 token 数 */
    private long extractTokenField(Map<?, ?> usage, String... fieldNames) {
        for (String name : fieldNames) {
            Object val = usage.get(name);
            if (val instanceof Number num) {
                return num.longValue();
            }
        }
        return 0;
    }

    /**
     * 构建 DeepSeek Vision 格式的消息（OpenAI 兼容，支持多图）
     * <p>
     * 如果 imageUrl 是本地路径（/uploads/...），自动读取文件转为 base64 data URL，
     * 因为 DeepSeek 服务器无法访问 localhost。
     */
    private Map<String, Object> buildVisionMessage(String role, String text, List<String> imageUrls) {
        List<Map<String, Object>> content = new ArrayList<>();

        // 文本块
        if (text != null && !text.isBlank()) {
            content.add(Map.of("type", "text", "text", text));
        }

        // 图片块（支持多张）
        for (String url : imageUrls) {
            String resolvedUrl = resolveImageUrl(url);
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", resolvedUrl)
            ));
            log.debug("👁️ 图片块: urlType={}, urlPreview={}",
                    resolvedUrl.startsWith("data:") ? "base64" : "http",
                    resolvedUrl.length() > 80 ? resolvedUrl.substring(0, 80) + "..." : resolvedUrl);
        }

        log.info("👁️ 构建 Vision 消息: role={}, textLen={}, imageCount={}",
                role, text != null ? text.length() : 0, imageUrls.size());

        return Map.of("role", role, "content", content);
    }

    /**
     * 解析图片 URL：本地路径 → 读取文件转 base64 data URL；远程/已编码 URL 直接返回
     */
    private String resolveImageUrl(String imageUrl) {
        return resolveImageUrl(imageUrl, MAX_IMAGE_DIMENSION);
    }

    /** 解析图片 URL（指定最大边长） */
    private String resolveImageUrl(String imageUrl, int maxDimension) {
        if (imageUrl == null) return null;

        // 已是 data URL → 无需转换
        if (imageUrl.startsWith("data:")) {
            return imageUrl;
        }

        // 远程 HTTP URL → DeepSeek 可直接访问
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        // 本地路径（如 /uploads/ai/2026-07-05/xxx.png）→ 读文件转 base64
        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            // 去掉 URL 前缀 /uploads/ → 拼接本地路径
            String relativePath = imageUrl.replaceFirst("^/uploads/", "");
            Path imagePath = basePath.resolve(relativePath);

            if (!Files.exists(imagePath)) {
                log.warn("图片文件不存在: {}, 使用原始 URL", imagePath);
                return imageUrl;
            }

            byte[] bytes = Files.readAllBytes(imagePath);
            long originalSize = bytes.length;

            // 图片过大时缩放
            byte[] processed = resizeIfNeeded(bytes, maxDimension);

            String base64 = Base64.getEncoder().encodeToString(processed);

            // 根据扩展名确定 MIME 类型（缩放后统一用 JPEG，仅 PNG 如需透明则保留 PNG）
            String ext = getExtension(relativePath);
            boolean isResized = processed != bytes;
            String mime = isResized ? "image/jpeg" : switch (ext) {
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "gif" -> "image/gif";
                case "webp" -> "image/webp";
                default -> "image/png";
            };

            String dataUrl = "data:" + mime + ";base64," + base64;
            if (isResized) {
                log.info("📷 本地图片缩放+转 base64: {} → {} ({} bytes → {} bytes), dataUrl 长度: {}",
                        imagePath.getFileName(), isResized ? "JPEG" : ext, originalSize, processed.length, dataUrl.length());
            } else {
                log.info("📷 本地图片转 base64: {} → {} bytes, dataUrl 长度: {}",
                        imagePath.getFileName(), bytes.length, dataUrl.length());
            }
            return dataUrl;

        } catch (IOException e) {
            log.error("读取本地图片失败: {}, 使用原始 URL", imageUrl, e);
            return imageUrl;
        }
    }

    /**
     * 图片缩放（如果需要）- 缩放至最大边长限定值并转换为 JPEG
     * <p>
     * 如果图片任一边长超过 maxDimension，等比缩放。如果未缩放则返回原数据。
     * 失败时 Fallback 返回原数据（不阻断流程）。
     */
    private byte[] resizeIfNeeded(byte[] imageBytes, int maxDimension) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) return imageBytes;

            int w = original.getWidth();
            int h = original.getHeight();
            if (w <= maxDimension && h <= maxDimension && imageBytes.length <= 1_048_576) {
                return imageBytes;
            }

            double scale = Math.min((double) maxDimension / w, (double) maxDimension / h);
            if (scale >= 1.0) scale = 1.0;
            int newW = (int) (w * scale);
            int newH = (int) (h * scale);

            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, newW, newH, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "JPEG", out);
            log.info("🔧 图片缩放: {}×{} → {}×{}, {} bytes → {} bytes",
                    w, h, newW, newH, imageBytes.length, out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.warn("图片缩放失败，使用原图: {}", e.getMessage());
            return imageBytes;
        }
    }

    /**
     * 从文件名/路径提取扩展名（小写）
     */
    private String getExtension(String path) {
        if (path == null || !path.contains(".")) return "png";
        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        // 去掉可能的查询参数
        int queryIdx = ext.indexOf('?');
        if (queryIdx > 0) ext = ext.substring(0, queryIdx);
        return ext;
    }
}