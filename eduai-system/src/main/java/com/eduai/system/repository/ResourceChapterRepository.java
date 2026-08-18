package com.eduai.system.repository;

import com.eduai.system.entity.ResourceChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习资源 · 章节 Repository
 */
@Repository
public interface ResourceChapterRepository extends JpaRepository<ResourceChapter, Long> {

    /** 按教材查询章节（排序升序） */
    List<ResourceChapter> findByTextbookIdOrderBySortOrderAsc(Long textbookId);

    /** 取同教材下排序最大的章节（用于新建时 sort_order = max + 1） */
    Optional<ResourceChapter> findTopByTextbookIdOrderBySortOrderDesc(Long textbookId);
}
