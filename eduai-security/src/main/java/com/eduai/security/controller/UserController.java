package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.common.Result;
import com.eduai.security.entity.User;
import com.eduai.security.enums.RoleEnum;
import com.eduai.security.repository.UserRelationRepository;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.impl.AuthServiceImpl;
import com.eduai.security.vo.PublicUserVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户公开信息查询
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserRelationRepository relationRepository;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 按 UID 查询用户公开信息 */
    @GetMapping("/{uidStr}")
    public Result<PublicUserVO> getUserByUid(@PathVariable String uidStr) {
        Long targetUid = AuthServiceImpl.parseUid(uidStr);
        if (targetUid == null) {
            throw new BusinessException(400, "UID格式不正确");
        }

        User user = userRepository.findByUid(targetUid)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 判断当前登录用户是否与此用户关联
        boolean isRelated = false;
        try {
            Long myUserId = StpUtil.getLoginIdAsLong();
            User me = userRepository.findById(myUserId).orElse(null);
            if (me != null && me.getUid() != null) {
                Long myUid = me.getUid();
                isRelated = relationRepository.existsByFromUidAndToUidAndStatus(myUid, targetUid, "accepted")
                        || relationRepository.existsByFromUidAndToUidAndStatus(targetUid, myUid, "accepted");
            }
        } catch (Exception ignored) {
            // 未登录时 isRelated = false
        }

        return Result.ok(PublicUserVO.builder()
                .id(user.getId())
                .uid(AuthServiceImpl.formatUid(user.getUid()))
                .name(user.getRealName() != null ? user.getRealName() : user.getNickname())
                .role(roleLabel(user.getRoleType()))
                .subjects(parseJsonArray(user.getSubjects()))
                .bio(user.getBio())
                .avatar(user.getAvatar())
                .isRelated(isRelated)
                .build());
    }

    private String roleLabel(Integer roleType) {
        if (roleType == null) return null;
        try { return RoleEnum.fromDbValue(roleType).getCode(); } catch (Exception e) { return null; }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return JSON.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
