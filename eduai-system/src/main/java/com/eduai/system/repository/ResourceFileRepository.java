package com.eduai.system.repository;

import com.eduai.system.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学习资源 · 资源文件 Repository
 */
@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, Long> {

    /** 按小节查询资源文件（创建时间倒序） */
    List<ResourceFile> findBySectionIdOrderByCreatedAtDesc(Long sectionId);
}
