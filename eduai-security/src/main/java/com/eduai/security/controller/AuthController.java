package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.dto.LoginDTO;
import com.eduai.security.dto.RegisterDTO;
import com.eduai.security.service.AuthService;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = authService.login(dto);
        return Result.ok(vo);
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO vo = authService.register(dto);
        return Result.ok(vo);
    }

    /** 获取当前用户 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        long userId = StpUtil.getLoginIdAsLong();
        UserVO vo = authService.me(userId);
        return Result.ok(vo);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }
}