package com.eduai.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.ai.config.DeepSeekConfig;
import com.eduai.common.BusinessException;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.entity.AiConfig;
import com.eduai.system.repository.AiConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 功能配置 Service — 管理每项功能选择哪个模型
 * <p>
 * 32.4 改造后：只维护 module → model_value 映射，
 * 模型详情（apiUrl/apiKey）由 ai_models 表管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository aiConfigRepository;
    private final UserRepository userRepository;
    private final DeepSeekConfig deepSeekConfig;

    private void checkAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
        if (user.getRoleType() != 1) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }

    @Transactional(readOnly = true)
    public List<AiConfig> list() {
        checkAdmin();
        return aiConfigRepository.findAll();
    }

    @Transactional
    public List<AiConfig> save(List<AiConfig> configs) {
        checkAdmin();
        for (AiConfig cfg : configs) {
            AiConfig existing = aiConfigRepository.findByModule(cfg.getModule()).orElse(null);
            if (existing != null) {
                existing.setModel(cfg.getModel());
                aiConfigRepository.save(existing);
            } else {
                aiConfigRepository.save(cfg);
            }
        }
        log.info("AI 功能配置已更新: {} 项", configs.size());
        deepSeekConfig.refresh(); // 立即刷新内存缓存
        return aiConfigRepository.findAll();
    }
}
