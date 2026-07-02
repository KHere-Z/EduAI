package com.eduai.system;

import com.baomidou.mybatisplus.autoconfigure.DdlAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusInnerInterceptorAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusLanguageDriverAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 *
 *
 * EduAI — 全学科 AI 智能教育平台
 * Spring Boot 启动入口
 *
 * <p>暂时排除 MyBatis-Plus 全部自动配置（当前仅使用 JPA，MyBatis-Plus 待后续集成）</p>
 */
@SpringBootApplication(exclude = {
        MybatisPlusAutoConfiguration.class,
        MybatisPlusInnerInterceptorAutoConfiguration.class,
        MybatisPlusLanguageDriverAutoConfiguration.class,
        IdentifierGeneratorAutoConfiguration.class,
        DdlAutoConfiguration.class
})
@EnableJpaRepositories(basePackages = "com.eduai")
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
