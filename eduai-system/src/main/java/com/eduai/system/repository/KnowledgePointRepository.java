package com.eduai.system.repository;

import com.eduai.system.entity.KnowledgePoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 知识点 Repository
 */
@Repository
public interface KnowledgePointRepository extends JpaRepository<KnowledgePoint, Long>,
        JpaSpecificationExecutor<KnowledgePoint> {

    /**
     * 按学科和年级筛选，分页查询
     */
    Page<KnowledgePoint> findBySubjectAndGradeLevel(String subject, String gradeLevel, Pageable pageable);

    /**
     * 按学科查询全部（不分年级）
     */
    Page<KnowledgePoint> findBySubject(String subject, Pageable pageable);

    /**
     * 按学科 + 多个年级批量查询（学生端用）
     */
    Page<KnowledgePoint> findBySubjectAndGradeLevelIn(String subject, Collection<String> gradeLevels, Pageable pageable);

    /**
     * 按学科 + 名称查找（用于去重检查）
     */
    boolean existsBySubjectAndName(String subject, String name);

    /**
     * 按学科查询所有（列表）
     */
    List<KnowledgePoint> findBySubjectOrderBySortOrderAsc(String subject);
}