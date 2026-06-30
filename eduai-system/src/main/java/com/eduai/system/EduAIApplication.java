package com.eduai.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * EduAI — 全学科 AI 智能教育平台
 * Spring Boot 启动入口
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.eduai")
@EntityScan(basePackages = "com.eduai")
public class EduAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduAIApplication.class, args);
        System.out.println("""

                ============================================
                  📖  EduAI Server 已启动
                  全学科 AI 智能教育平台
                  http://localhost:8080
                  API 文档: http://localhost:8080/doc.html
                ============================================
                """);
    }
}
