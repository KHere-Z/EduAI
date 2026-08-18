package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.dto.RelationRequest;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.security.service.RelationService;
import com.eduai.security.vo.RelationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户关系接口
 * <p>
 * 师生关系管理（老师↔学生双向绑定）。
 */
@RestController
@RequestMapping("/api/v1/relations")
@RequiredArgsConstructor
public class RelationController {

    private final RelationService relationService;
    private final UserRepository userRepository;

    /** 我的关联列表（已接受关系）；?type=colleague 只返回同事 */
    @GetMapping
    public Result<List<RelationVO>> getMyRelations(@RequestParam(required = false) String type) {
        Long myUid = getMyUid();
        return Result.ok(relationService.getMyRelations(myUid, type));
    }

    /** 收到的待处理请求；?type=colleague 只返回同事请求 */
    @GetMapping("/incoming")
    public Result<List<RelationVO>> getIncoming(@RequestParam(required = false) String type) {
        Long myUid = getMyUid();
        return Result.ok(relationService.getIncomingRelations(myUid, type));
    }

    /** 发送关联请求 */
    @PostMapping("/request")
    public Result<RelationVO> sendRequest(@RequestBody RelationRequest req) {
        Long myUid = getMyUid();
        return Result.ok(relationService.sendRequest(myUid, req.getTargetUid(), req.getType()));
    }

    /** 同意请求（接收方操作） */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Long myUid = getMyUid();
        relationService.approve(id, myUid);
        return Result.ok();
    }

    /** 拒绝请求 */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        Long myUid = getMyUid();
        relationService.reject(id, myUid);
        return Result.ok();
    }

    /** 移除已有关联 */
    @DeleteMapping("/{id}")
    public Result<Void> removeRelation(@PathVariable Long id) {
        Long myUid = getMyUid();
        relationService.removeRelation(id, myUid);
        return Result.ok();
    }

    /** 从 Sa-Token 获取当前登录用户，返回 UID（老用户自动补 uid） */
    private Long getMyUid() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.eduai.common.BusinessException(401, "用户不存在"));
        if (user.getUid() == null) {
            user.setUid(com.eduai.security.service.impl.AuthServiceImpl.generateUid());
            userRepository.save(user);
        }
        return user.getUid();
    }
}
