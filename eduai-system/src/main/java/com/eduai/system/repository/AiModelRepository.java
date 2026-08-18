package com.eduai.system.repository;

import com.eduai.system.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AI 模型注册表 Repository
 */
@Repository
public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    Optional<AiModel> findByValue(String value);

    boolean existsByValue(String value);
}
