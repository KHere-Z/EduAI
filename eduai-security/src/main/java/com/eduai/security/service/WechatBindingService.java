package com.eduai.security.service;

import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.entity.UserWechat;
import com.eduai.security.enums.AuthErrorCode;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.repository.UserWechatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 微信绑定唯一性校验核心服务
 * <p>
 * 规则（不可破坏）：
 * 1. 一个手机号只能绑定唯一微信
 * 2. 一个微信（unionid）只能绑定唯一手机号
 * 3. 冲突时抛出明确业务异常，禁止静默覆盖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatBindingService {

    private final UserWechatRepository wechatRepository;
    private final UserRepository userRepository;

    /**
     * 校验并执行微信绑定（新用户 + 已有用户通用）
     * <p>
     * 调用前确保手机号已验证通过（短信验证码已校验）。
     *
     * @param phone   手机号
     * @param unionid 微信 unionid
     * @param openid  微信 openid
     * @param wxNickname 微信昵称（可选）
     * @param wxAvatar   微信头像（可选）
     * @return 绑定的用户
     * @throws BusinessException 绑定冲突时抛出
     */
    @Transactional
    public User bindWechatToPhone(String phone, String unionid, String openid,
                                   String wxNickname, String wxAvatar) {
        // 1. 双重唯一性校验（核心安全逻辑，禁止简化）
        validateBindingUniqueness(phone, unionid);

        // 2. 查找或创建手机号用户
        User user = userRepository.findByPhone(phone).orElse(null);

        // 3. 创建绑定关系（unionid 唯一索引保证数据层防重）
        UserWechat binding = UserWechat.builder()
                .userUid(user != null ? user.getUid() : null) // 临时为 null，创建用户后回填
                .openid(openid)
                .unionid(unionid)
                .nickname(wxNickname)
                .avatar(wxAvatar)
                .build();

        if (user != null) {
            // 手机号已有账号 → 直接绑定
            binding.setUserUid(user.getUid());
            wechatRepository.save(binding);
            log.info("微信绑定已有账号: phone={}, uid={}, unionid={}",
                    maskPhone(phone), user.getUid(), unionid);
        } else {
            // 新用户 → 先保存绑定关系，uid 由 AuthServiceImpl 创建用户后回填
            // 临时使用 0L 占位（外键约束需要有效 uid）
            // 实际上新用户创建流程在 AuthServiceImpl 中处理
            throw new IllegalStateException("手机号未注册，请先通过注册流程创建账号");
        }

        return user;
    }

    /**
     * 已登录用户绑定微信
     * <p>
     * 用户已登录（有 token），将微信 unionid 绑定到当前账号。
     */
    @Transactional
    public UserWechat bindWechatToCurrentUser(Long userUid, String openid, String unionid,
                                               String wxNickname, String wxAvatar) {
        // 1. 检查当前用户是否已绑定微信
        wechatRepository.findByUserUid(userUid).ifPresent(w -> {
            throw new BusinessException(
                    AuthErrorCode.WECHAT_ALREADY_BOUND.getCode(),
                    "当前账号已绑定微信，请先解绑");
        });

        // 2. 检查 unionid 是否已被其他用户绑定
        if (wechatRepository.existsByUnionidAndUserUidNot(unionid, userUid)) {
            throw new BusinessException(
                    AuthErrorCode.WECHAT_ALREADY_BOUND.getCode(),
                    AuthErrorCode.WECHAT_ALREADY_BOUND.getMessage());
        }

        // 3. 创建绑定
        UserWechat binding = UserWechat.builder()
                .userUid(userUid)
                .openid(openid)
                .unionid(unionid)
                .nickname(wxNickname)
                .avatar(wxAvatar)
                .build();

        binding = wechatRepository.save(binding);
        log.info("用户绑定微信成功: uid={}, unionid={}", userUid, unionid);
        return binding;
    }

    /**
     * 解绑微信
     */
    @Transactional
    public void unbindWechat(Long userUid) {
        UserWechat binding = wechatRepository.findByUserUid(userUid)
                .orElseThrow(() -> new BusinessException(
                        AuthErrorCode.WECHAT_NOT_BOUND.getCode(),
                        "当前账号未绑定微信"));

        wechatRepository.delete(binding);
        log.info("用户解绑微信: uid={}, unionid={}", userUid, binding.getUnionid());
    }

    /**
     * 根据 unionid 查找绑定的用户（微信登录流程用）
     */
    @Transactional(readOnly = true)
    public Optional<User> findUserByUnionid(String unionid) {
        return wechatRepository.findByUnionid(unionid)
                .map(w -> userRepository.findByUid(w.getUserUid()).orElse(null));
    }

    /**
     * 双重唯一性校验（核心方法，禁止简化）
     * <p>
     * 校验逻辑：
     * ① 该手机号是否已绑定其他微信 unionid
     * ② 该微信 unionid 是否已绑定其他手机号
     */
    private void validateBindingUniqueness(String phone, String unionid) {
        // ① 手机号 → 微信 方向校验
        //    查找该手机号对应用户已绑定的微信，且该微信 unionid 不同于当前 unionid
        User phoneUser = userRepository.findByPhone(phone).orElse(null);
        if (phoneUser != null) {
            UserWechat existing = wechatRepository.findByUserUid(phoneUser.getUid()).orElse(null);
            if (existing != null && !existing.getUnionid().equals(unionid)) {
                throw new BusinessException(
                        AuthErrorCode.PHONE_ALREADY_BOUND.getCode(),
                        AuthErrorCode.PHONE_ALREADY_BOUND.getMessage());
            }
        }

        // ② 微信 → 手机号 方向校验
        //    查找该 unionid 已绑定的用户，且该用户手机号不同于当前手机号
        UserWechat wxBinding = wechatRepository.findByUnionid(unionid).orElse(null);
        if (wxBinding != null) {
            User wxUser = userRepository.findByUid(wxBinding.getUserUid()).orElse(null);
            if (wxUser != null && !phone.equals(wxUser.getPhone())) {
                throw new BusinessException(
                        AuthErrorCode.WECHAT_ALREADY_BOUND.getCode(),
                        AuthErrorCode.WECHAT_ALREADY_BOUND.getMessage());
            }
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
