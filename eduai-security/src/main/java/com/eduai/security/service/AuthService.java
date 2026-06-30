package com.eduai.security.service;

import com.eduai.security.dto.LoginDTO;
import com.eduai.security.dto.RegisterDTO;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     */
    UserVO register(RegisterDTO dto);

    /**
     * 获取当前登录用户
     */
    UserVO me(Long userId);

    /**
     * 退出登录
     */
    void logout();
}