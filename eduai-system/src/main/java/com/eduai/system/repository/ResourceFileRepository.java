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

    /** 按节点查询资源文件（创建时间倒序） */
    List<ResourceFile> findByNodeTypeAndNodeIdOrderByCreatedAtDesc(String nodeType, Long nodeId);

    /** 按节点批量查询（同 nodeType，创建时间倒序，用于聚合子级资源） */
    List<ResourceFile> findByNodeTypeAndNodeIdInOrderByCreatedAtDesc(String nodeType, List<Long> nodeIds);

    /** 按审核状态查询（创建时间倒序，用于待审核列表） */
    List<ResourceFile> findByStatusOrderByCreatedAtDesc(String status);
}
