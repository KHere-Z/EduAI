package com.eduai.system.repository;

import com.eduai.system.entity.KpResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识点资源 Repository
 */
@Repository
public interface KpResourceRepository extends JpaRepository<KpResource, Long> {

    /** 按知识点ID查询所有资源（按创建时间倒序） */
    List<KpResource> findByKpIdOrderByCreatedAtDesc(Long kpId);
}
