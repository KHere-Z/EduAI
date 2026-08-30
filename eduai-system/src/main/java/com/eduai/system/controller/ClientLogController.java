package com.eduai.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.BusinessException;
import com.eduai.common.Result;
import com.eduai.security.entity.User;
import com.eduai.security.repository.UserRepository;
import com.eduai.system.dto.ClientLogEntry;
import com.eduai.system.dto.ClientLogReportDTO;
import com.eduai.system.service.ClientLogBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 前端日志采集 Controller
 * <p>
 * 上报接口（POST）匿名可调，仅入队即返回；查询接口（GET）仅管理员可调。
 * 日志存内存环形缓冲，不落盘、不占磁盘。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/client-log")
@RequiredArgsConstructor
public class ClientLogController {

    private final ClientLogBuffer clientLogBuffer;
    private final UserRepository userRepository;

    /**
     * 批量上报前端日志，立即入队并返回。
     */
    @PostMapping
    public Result<Void> report(@RequestBody(required = false) ClientLogReportDTO dto) {
        if (dto == null || dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return Result.ok();
        }
        Integer role = dto.getRole();
        String uid = dto.getUid();
        for (ClientLogEntry entry : dto.getEntries()) {
            if (entry == null) {
                continue;
            }
            // 顶层 role/uid 回填到条目，覆盖前端单条未带的情况
            if (entry.getRole() == null) {
                entry.setRole(role);
            }
            if (entry.getUid() == null || entry.getUid().isBlank()) {
                entry.setUid(uid);
            }
            clientLogBuffer.append(entry);
        }
        return Result.ok();
    }

    /**
     * 管理员查询日志（level/uid/role 过滤 + 分页）。
     */
    @GetMapping("/query")
    public Result<Map<String, Object>> query(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) Integer role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        checkAdmin();
        int cappedSize = Math.min(Math.max(size, 1), 200);
        return Result.ok(clientLogBuffer.query(level, uid, role, page, cappedSize));
    }

    private void checkAdmin() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "请先登录"));
        if (user.getRoleType() != 1) {
            throw new BusinessException(403, "仅管理员可访问");
        }
    }
}
