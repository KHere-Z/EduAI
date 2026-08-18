package com.eduai.system.repository;

import com.eduai.system.entity.QuestionKnowledgePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 题目-知识点关联 Repository
 */
@Repository
public interface QuestionKnowledgePointRepository extends JpaRepository<QuestionKnowledgePoint, Long> {

    /** 删除某题的全部知识点关联（更新题目时先清后写） */
    void deleteByQuestionId(Long questionId);

    /** 按知识点ID查关联的题目ID（筛题走 idx_qkp_kp 索引） */
    @Query("SELECT qkp.questionId FROM QuestionKnowledgePoint qkp WHERE qkp.knowledgePointId = :kpId")
    List<Long> findQuestionIdsByKnowledgePointId(@Param("kpId") Long kpId);
}
