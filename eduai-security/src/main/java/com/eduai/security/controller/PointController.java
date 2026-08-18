package com.eduai.security.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.eduai.common.Result;
import com.eduai.security.service.PointService;
import com.eduai.security.vo.MembershipVO;
import com.eduai.security.vo.PointHistoryPageVO;
import com.eduai.security.vo.PointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    /** 查询会员状态 */
    @GetMapping("/membership")
    public Result<MembershipVO> getMembership() {
        return Result.ok(pointService.getMembership(StpUtil.getLoginIdAsLong()));
    }
}
