package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.dto.DeletePointHistoryRequest;
import com.eduai.security.service.PointService;
import com.eduai.security.vo.MembershipVO;
import com.eduai.security.vo.PointHistoryPageVO;
import com.eduai.security.vo.PointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 智学点 & 会员接口
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /** 查询智学点余额 */
    @GetMapping("/points")
    public Result<PointVO> getPoints() {
        return Result.ok(pointService.getPoints(StpUtil.getLoginIdAsLong()));
    }

    /** 点数变动历史 */
    @GetMapping("/points/history")
    public Result<PointHistoryPageVO> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(pointService.getHistory(StpUtil.getLoginIdAsLong(), page, pageSize));
    }

    /**
     * 批量删除点数变动记录（仅限本人流水；软删，不改余额）
     * <p>
     * 用 POST 而非 DELETE 带 body：部分网关/代理会丢弃 DELETE 的请求体。
     */
    @PostMapping("/points/history/delete")
    public Result<Map<String, Object>> deleteHistory(
            @RequestBody DeletePointHistoryRequest request) {
        int deleted = pointService.deleteHistory(StpUtil.getLoginIdAsLong(), request.getIds());
        return Result.ok(Map.of("deleted", deleted));
    }

    /** 查询会员状态 */
    @GetMapping("/membership")
    public Result<MembershipVO> getMembership() {
        return Result.ok(pointService.getMembership(StpUtil.getLoginIdAsLong()));
    }
}
