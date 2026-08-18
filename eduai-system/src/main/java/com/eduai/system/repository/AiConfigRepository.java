package com.eduai.system.repository;

import com.eduai.system.entity.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AI 配置 Repository
 */
@Repository
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {

    Optional<AiConfig> findByModule(String module);
}
