package com.eduai.security.service;

import com.eduai.security.dto.*;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /** 用户名密码登录（保留兼容） */
    LoginVO login(LoginDTO dto);

    /** 用户名密码注册（保留兼容） */
    UserVO register(RegisterDTO dto);

    /** 获取当前用户信息 */
    UserVO me(Long userId);

    /** 更新个人信息 */
    UserVO updateProfile(Long userId, UpdateProfileRequest req);

    /** 上传头像（multipart 单文件），写入 users.avatar，返回 {url} */
    Map<String, String> uploadAvatar(Long userId, MultipartFile file);

    /** 退出登录 */
    void logout();

    // ==================== 手机号短信登录 ====================

    /** 发送短信验证码 */
    void sendSms(String phone);

    /** 短信验证码登录/注册 */
    LoginVO loginBySms(LoginBySmsRequest request);

    // ==================== 微信登录 ====================

    /** 微信登录（含自动绑定检测） */
    LoginVO wechatLogin(WechatLoginRequest request);

    /** 微信绑定手机号（合并注册） */
    LoginVO bindPhone(BindPhoneRequest request);

    // ==================== 已登录用户微信管理 ====================

    /** 已登录用户绑定微信（userId = 内部自增ID） */
    void bindWechat(Long userId, BindWechatRequest request);

    /** 已登录用户解绑微信（userId = 内部自增ID） */
    void unbindWechat(Long userId);
}
