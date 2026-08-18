package com.eduai.system.config;

import com.eduai.system.entity.Question;
import com.eduai.system.repository.QuestionRepository;
import com.eduai.system.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 一次性迁移：把 {@code question_bank} 三列遗留的 base64 图片落盘为 {@code /uploads/question-images/**} URL。
 * <p>
 * 仅当 {@code eduai.question-image-migrate=true} 时启用，例如启动参数
 * {@code --eduai.question-image-migrate=true} 或环境变量 {@code EDUAI_QUESTION_IMAGE_MIGRATE=true}。
 * 迁移幂等：落盘后的行三列不再以 {@code data:} 开头，二次启动不会被 {@code findWithBase64Images()} 命中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eduai", name = "question-image-migrate", havingValue = "true")
public class QuestionImageMigrationRunner implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    public void run(String... args) {
        List<Question> list = questionRepository.findWithBase64Images();
        if (list.isEmpty()) {
            log.info("[图片迁移] 无 base64 图片，跳过。");
            return;
        }
        log.info("[图片迁移] 开始：共 {} 条题目含 base64 图片...", list.size());

        for (Question q : list) {
            q.setOriginalImageUrl(imageStorageService.persistIfBase64(q.getOriginalImageUrl(), "original"));
            q.setDiagramImageUrl(imageStorageService.persistIfBase64(q.getDiagramImageUrl(), "diagram"));
            q.setTeacherAnalysisImage(imageStorageService.persistIfBase64(q.getTeacherAnalysisImage(), "analysis"));
        }
        // 实体处于托管态，事务提交时 dirty-checking 自动 flush 更新
        log.info("[图片迁移] 完成：{} 条题目已处理。", list.size());
    }
}
