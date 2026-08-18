package com.eduai.system.repository;

import com.eduai.system.entity.ResourceTextbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学习资源 · 教材 Repository
 */
@Repository
public interface ResourceTextbookRepository extends JpaRepository<ResourceTextbook, Long> {

    /** 按学科查询教材（排序升序） */
    List<ResourceTextbook> findBySubjectOrderBySortOrderAsc(String subject);

    /** 取同学科下排序最大的教材（用于新建时 sort_order = max + 1） */
    Optional<ResourceTextbook> findTopBySubjectOrderBySortOrderDesc(String subject);
}
