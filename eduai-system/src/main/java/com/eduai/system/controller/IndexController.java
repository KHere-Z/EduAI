package com.eduai.system.controller;

import com.eduai.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 根路由 / API 入口
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        return Result.ok(Map.of(
                "app", "EduAI Server",
                "version", "1.0.0",
                "time", LocalDateTime.now().toString(),
                "docs", "/doc.html",
                "api", "/api/v1"
        ));
    }
}