package com.eduai.system.repository;

import com.eduai.system.entity.ResourceSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习资源 · 小节 Repository
 */
@Repository
public interface ResourceSectionRepository extends JpaRepository<ResourceSection, Long> {

    /** 按章节查询小节（排序升序） */
    List<ResourceSection> findByChapterIdOrderBySortOrderAsc(Long chapterId);

    /** 取同章节下排序最大的小节（用于新建时 sort_order = max + 1） */
    Optional<ResourceSection> findTopByChapterIdOrderBySortOrderDesc(Long chapterId);
}
