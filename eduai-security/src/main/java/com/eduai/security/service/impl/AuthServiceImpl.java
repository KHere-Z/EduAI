package com.eduai.security.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.security.dto.LoginDTO;
import com.eduai.security.dto.RegisterDTO;
import com.eduai.security.entity.Teacher;
import com.eduai.security.entity.User;
import com.eduai.security.repository.OrganizationRepository;
import com.eduai.security.repository.TeacherRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.AuthService;
import com.eduai.security.vo.LoginVO;
import com.eduai.security.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 2. 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 3. 校验密码（明文）
        if (!dto.getPassword().equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 4. Sa-Token 登录，并获取创建的 token
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        // 5. 构建 UserVO（含教师信息）
        return LoginVO.builder()
                .user(toUserVO(user))
                .token(tokenValue)
                .build();
    }

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = User.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .realName(dto.getRealName())
                .roleType(dto.getRoleType())
                .status(1)
                .build();

        userRepository.save(user);

        // 如果是教师角色，同时创建教师记录
        if (dto.getRoleType() == 3 && dto.getSubjectIds() != null && !dto.getSubjectIds().isBlank()) {
            Teacher teacher = Teacher.builder()
                    .userId(user.getId())
                    .subjectIds(dto.getSubjectIds())
                    .orgId(dto.getOrgId())
                    .title(dto.getTitle())
                    .build();
            teacherRepository.save(teacher);
        }

        return toUserVO(user);
    }

    @Override
    public UserVO me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toUserVO(user);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 实体 → VO（含教师信息增强）
     */
    private UserVO toUserVO(User user) {
        UserVO.UserVOBuilder builder = UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .roleType(user.getRoleType())
                .status(user.getStatus());

        // 教师角色：查询 teachers 表，填充学科/机构/职称等
        if (user.getRoleType() != null && user.getRoleType() == 3) {
            teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                // 解析 subject_ids → List<String>
                List<String> subjects = parseSubjectIds(teacher.getSubjectIds());
                builder.subjects(subjects)
                        .orgId(teacher.getOrgId())
                        .title(teacher.getTitle())
                        .avatar(teacher.getAvatar());

                // 查询机构名称
                if (teacher.getOrgId() != null) {
                    organizationRepository.findById(teacher.getOrgId()).ifPresent(org ->
                            builder.orgName(org.getName()));
                }
            });
        }

        return builder.build();
    }

    /**
     * "math,physics" → ["math", "physics"]
     */
    private List<String> parseSubjectIds(String subjectIds) {
        if (subjectIds == null || subjectIds.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(subjectIds.split(","));
    }
}