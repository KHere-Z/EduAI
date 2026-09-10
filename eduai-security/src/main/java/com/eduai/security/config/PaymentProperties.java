package com.eduai.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置占位（真实 SDK 接入预留）
 * <p>
 * 配置前缀 {@code eduai.payment}。开发环境 {@code channel=mock}（Mock 支付）；
 * 生产接入真实支付宝/微信 SDK 时，将 {@code channel} 改为 {@code alipay} / {@code wechat}，
 * 填充对应凭证字段，并实现 {@link com.eduai.security.service.PaymentService}（参考
 * {@link com.eduai.security.service.impl.MockPaymentServiceImpl}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "eduai.payment")
public class PaymentProperties {

    /** 支付渠道：mock（开发）/ alipay / wechat */
    private String channel = "mock";

    /** 支付宝配置（channel=alipay 时生效） */
    private Alipay alipay = new Alipay();

    /** 微信支付配置（channel=wechat 时生效） */
    private Wechat wechat = new Wechat();

    @Data
    public static class Alipay {
        /** 应用 ID */
        private String appId;
        /** 应用私钥（PKCS8） */
        private String privateKey;
        /** 支付宝公钥 */
        private String alipayPublicKey;
        /** 网关地址 */
        private String gateway = "https://openapi.alipay.com/gateway.do";
        /** 异步通知回调地址 */
        private String notifyUrl;
    }

    @Data
    public static class Wechat {
        /** 公众号/小程序 appId */
        private String appId;
        /** 商户号 */
        private String mchId;
        /** APIv3 密钥 */
        private String apiV3Key;
        /** 商户证书序列号 */
        private String certSerialNo;
        /** 商户私钥文件路径 */
        private String privateKeyPath;
        /** 微信支付公钥文件路径（公钥验签模式） */
        private String publicKeyPath;
        /** 微信支付公钥 ID（PUB_KEY_ID_xxx） */
        private String publicKeyId;
        /** 异步通知回调地址 */
        private String notifyUrl;
    }
}
