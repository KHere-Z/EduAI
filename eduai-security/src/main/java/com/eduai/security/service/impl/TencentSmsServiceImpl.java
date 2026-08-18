package com.eduai.security.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.eduai.common.BusinessException;
import com.eduai.security.config.SmsConfig;
import com.eduai.security.enums.AuthErrorCode;
import com.eduai.security.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 腾讯云短信服务实现（Hutool HTTP + TC3-HMAC-SHA256）
 * <p>
 * 无腾讯云 SDK 依赖，直调 SMS API v3（2021-01-11）。
 * <p>
 * <b>重试策略：</b>
 * <ul>
 *   <li>仅 {@link SocketTimeoutException} / {@link ConnectException} 重试 <b>最多1次</b></li>
 *   <li>收到 HTTP 响应（含 4xx/5xx）后<b>绝不重试</b> — 短信可能已下发，只是响应丢失</li>
 *   <li>腾讯云返回业务错误（Error/SendStatusSet.Code != Ok）→ 不重试，直接抛异常</li>
 *   <li>用户手动重试需等 60s（由 AuthServiceImpl Redis 限流保证）</li>
 * </ul>
 * <p>
 * <b>日志标记：</b>
 * <ul>
 *   <li>{@code [SMS-API]} — 接口调用失败（网络/超时/非JSON），短信<b>大概率未下发</b></li>
 *   <li>{@code [SMS-SEND]} — 腾讯云返回错误，短信<b>已确认下发失败</b></li>
 * </ul>
 *
 * @see SmsConfig
 * @see MockSmsServiceImpl 开发环境兜底（secret-id 未配置时自动启用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms.tencent", name = "secret-id")
public class TencentSmsServiceImpl implements SmsService {

    private static final String ENDPOINT  = "sms.tencentcloudapi.com";
    private static final String SERVICE   = "sms";
    private static final String VERSION   = "2021-01-11";
    private static final String ACTION    = "SendSms";
    private static final String REGION    = "ap-guangzhou";
    private static final String ALGORITHM = "TC3-HMAC-SHA256";

    /** 连接超时 ms */
    private static final int CONNECT_TIMEOUT = 5_000;
    /** 读取超时 ms（短信 API 通常 <1s 返回） */
    private static final int READ_TIMEOUT = 10_000;
    /** 网络异常最多重试次数 */
    private static final int MAX_RETRIES = 1;

    private final SmsConfig smsConfig;

    // ==================== 公共接口 ====================

    @Override
    public void sendVerifyCode(String phone, String code) {
        String payload = buildPayload(phone, code);
        String timestamp = String.valueOf(ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond());
        String authorization = sign(payload, timestamp);

        doSend(phone, payload, authorization, timestamp, MAX_RETRIES);
    }

    // ==================== 请求体 ====================

    private String buildPayload(String phone, String code) {
        JSONObject body = new JSONObject();
        body.set("PhoneNumberSet",    new String[]{"+86" + phone});
        body.set("SmsSdkAppId",       smsConfig.getSdkAppId());
        body.set("SignName",          smsConfig.getSignName());
        body.set("TemplateId",        smsConfig.getTemplateId());
        body.set("TemplateParamSet",  new String[]{code});
        return body.toString();
    }

    // ==================== 发送 & 重试 ====================

    /**
     * 发送 HTTP 请求，仅在网络层异常时重试。
     * <p>
     * 设计原则：TCP 超时/连接拒绝说明请求大概率未到达腾讯云网关，可以安全重试。
     * 一旦收到 HTTP 响应（无论状态码），绝不重试 — 因为短信可能已下发。
     */
    private void doSend(String phone, String payload, String authorization,
                        String timestamp, int retriesLeft) {
        String responseBody;
        try {
            HttpResponse httpResp = HttpRequest.post("https://" + ENDPOINT)
                    .header("Authorization",  authorization)
                    .header("Content-Type",   "application/json; charset=utf-8")
                    .header("Host",           ENDPOINT)
                    .header("X-TC-Action",    ACTION)
                    .header("X-TC-Version",   VERSION)
                    .header("X-TC-Timestamp", timestamp)
                    .header("X-TC-Region",    REGION)
                    .setConnectionTimeout(CONNECT_TIMEOUT)
                    .setReadTimeout(READ_TIMEOUT)
                    .body(payload)
                    .execute();

            responseBody = httpResp.body();

        } catch (HttpException e) {
            Throwable cause = e.getCause();

            // 仅网络层异常 + 仍有重试次数 → 重试
            if (retriesLeft > 0 && isNetworkException(cause)) {
                log.warn("[SMS-API] 网络异常,第{}次重试: phone={}, type={}, msg={}",
                        MAX_RETRIES - retriesLeft + 1, maskPhone(phone),
                        cause != null ? cause.getClass().getSimpleName() : "unknown",
                        e.getMessage());
                doSend(phone, payload, authorization, timestamp, retriesLeft - 1);
                return;
            }

            // 非网络异常 或 重试耗尽 → 不重试
            log.error("[SMS-API] HTTP异常{}: phone={}, type={}, msg={}",
                    retriesLeft == 0 ? "(重试已耗尽)" : "",
                    maskPhone(phone),
                    cause != null ? cause.getClass().getSimpleName() : e.getClass().getSimpleName(),
                    e.getMessage());
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    "短信服务暂时不可用，请稍后重试");
        }

        // 收到 HTTP 响应 → 解析业务结果（绝不再重试）
        parseResponse(responseBody, phone);
    }

    /** 仅 SocketTimeoutException / ConnectException 可安全重试 */
    private boolean isNetworkException(Throwable cause) {
        return cause instanceof SocketTimeoutException
                || cause instanceof ConnectException;
    }

    // ==================== TC3-HMAC-SHA256 签名 ====================

    /**
     * 腾讯云 API v3 签名（TC3-HMAC-SHA256）
     */
    private String sign(String payload, String timestamp) {
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC)
                .format(ZonedDateTime.now(ZoneOffset.UTC));

        // 1. 规范请求串
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n"
                + "host:" + ENDPOINT + "\n"
                + "x-tc-action:" + ACTION.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedPayload = sha256Hex(payload);

        String canonicalRequest = "POST\n/\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + hashedPayload;

        // 2. 待签字符串
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);

        String stringToSign = ALGORITHM + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + hashedCanonicalRequest;

        // 3. 派生签名密钥 → 计算签名
        byte[] secretDate    = hmacSha256(("TC3" + smsConfig.getSecretKey()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        byte[] signature     = hmacSha256(secretSigning, stringToSign);

        // 4. Authorization Header
        return ALGORITHM + " Credential=" + smsConfig.getSecretId() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + HexUtil.encodeHexStr(signature);
    }

    private byte[] hmacSha256(byte[] key, String data) {
        return new HMac("HmacSHA256", key).digest(data, StandardCharsets.UTF_8);
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexUtil.encodeHexStr(md.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256计算失败", e);
        }
    }

    // ==================== 响应解析 ====================

    /**
     * 解析腾讯云 SMS API 响应。
     * <p>
     * 三种情况：
     * <ol>
     *   <li>Response.Error 存在 → API 拒绝（签名/鉴权/参数），日志 [SMS-SEND]</li>
     *   <li>SendStatusSet[0].Code != "Ok" → 短信下发失败（黑名单/超频），日志 [SMS-SEND]</li>
     *   <li>SendStatusSet[0].Code == "Ok" → 成功</li>
     * </ol>
     */
    private void parseResponse(String responseBody, String phone) {
        JSONObject result;
        try {
            result = new JSONObject(responseBody);
        } catch (Exception e) {
            log.error("[SMS-API] 腾讯云响应非JSON: phone={}, body={}", maskPhone(phone), responseBody);
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    AuthErrorCode.SMS_SEND_FAILED.getMessage());
        }

        JSONObject resp = result.getJSONObject("Response");
        if (resp == null) {
            log.error("[SMS-API] 响应缺少Response字段: phone={}, body={}",
                    maskPhone(phone), responseBody);
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    AuthErrorCode.SMS_SEND_FAILED.getMessage());
        }

        // --- 情况1: API 层面错误（签名失败/鉴权失败/参数错误/欠费） ---
        if (resp.containsKey("Error")) {
            JSONObject error = resp.getJSONObject("Error");
            String errCode = error.getStr("Code", "Unknown");
            String errMsg  = error.getStr("Message", "Unknown");

            log.error("[SMS-SEND] 腾讯云业务拒绝: phone={}, code={}, message={}",
                    maskPhone(phone), errCode, errMsg);
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    AuthErrorCode.SMS_SEND_FAILED.getMessage());
        }

        // --- 情况2: 检查 SendStatusSet ---
        JSONArray statusSet = resp.getJSONArray("SendStatusSet");
        if (statusSet == null || statusSet.isEmpty()) {
            log.error("[SMS-SEND] 响应缺少SendStatusSet: phone={}, body={}",
                    maskPhone(phone), responseBody);
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    AuthErrorCode.SMS_SEND_FAILED.getMessage());
        }

        // --- 情况3: 逐条检查下发结果 ---
        JSONObject status = statusSet.getJSONObject(0);
        String sendCode = status.getStr("Code", "");

        if (!"Ok".equals(sendCode)) {
            String sendMsg = status.getStr("Message", "");
            log.error("[SMS-SEND] 短信下发失败: phone={}, code={}, msg={}",
                    maskPhone(phone), sendCode, sendMsg);
            throw new BusinessException(
                    AuthErrorCode.SMS_SEND_FAILED.getCode(),
                    AuthErrorCode.SMS_SEND_FAILED.getMessage());
        }

        // --- 成功 ---
        log.info("[SMS-SEND] 验证码已发送: phone={}, serialNo={}",
                maskPhone(phone), status.getStr("SerialNo", ""));
    }

    // ==================== 工具方法 ====================

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
