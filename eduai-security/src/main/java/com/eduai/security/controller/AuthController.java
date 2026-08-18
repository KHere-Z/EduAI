package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.common.annotation.RateLimit;
import com.eduai.security.dto.*;
import com.eduai.security.service.AuthService;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * <p>
 * 支持三种登录方式：
 * 1. 用户名+密码（保留兼容）
 * 2. 手机号+短信验证码
 * 3. 微信开放平台
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== 原有登录（保留兼容） ====================

    /** 用户名密码登录 */
    @PostMapping("/login")
    @RateLimit(limit = 20, windowSec = 60, message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    /** 用户名密码注册 */
    @PostMapping("/register")
    @RateLimit(limit = 10, windowSec = 60, message = "注册操作过于频繁，请稍后再试")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    /** 获取当前用户 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me(StpUtil.getLoginIdAsLong()));
    }

    /** 更新个人信息 */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UpdateProfileRequest req) {
        return Result.ok(authService.updateProfile(StpUtil.getLoginIdAsLong(), req));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    // ==================== 手机号短信登录 ====================

    /** 发送短信验证码（无需登录） */
    @PostMapping("/send-sms")
    @RateLimit(limit = 5, windowSec = 60, message = "验证码发送过于频繁，请稍后再试")
    public Result<Void> sendSms(@Valid @RequestBody SendSmsRequest req) {
        authService.sendSms(req.getPhone());
        return Result.ok();
    }

    /** 短信验证码登录/注册（无需登录） */
    @PostMapping("/login-sms")
    @RateLimit(limit = 20, windowSec = 60, message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginVO> loginBySms(@Valid @RequestBody LoginBySmsRequest req) {
        return Result.ok(authService.loginBySms(req));
    }

    // ==================== 微信登录 ====================

    /** 微信登录（无需登录；未绑定返回 needBindPhone=true） */
    @PostMapping("/wechat-login")
    public Result<LoginVO> wechatLogin(@RequestBody WechatLoginRequest req) {
        return Result.ok(authService.wechatLogin(req));
    }

    /** 微信绑定手机号（无需登录；合并注册） */
    @PostMapping("/bind-phone")
    public Result<LoginVO> bindPhone(@Valid @RequestBody BindPhoneRequest req) {
        return Result.ok(authService.bindPhone(req));
    }

    // ==================== 已登录用户微信管理 ====================

    /** 已登录用户绑定微信（需登录） */
    @PostMapping("/bind-wechat")
    public Result<Void> bindWechat(@Valid @RequestBody BindWechatRequest req) {
        authService.bindWechat(StpUtil.getLoginIdAsLong(), req);
        return Result.ok();
    }

    /** 已登录用户解绑微信（需登录） */
    @DeleteMapping("/unbind-wechat")
    public Result<Void> unbindWechat() {
        authService.unbindWechat(StpUtil.getLoginIdAsLong());
        return Result.ok();
    }
}
