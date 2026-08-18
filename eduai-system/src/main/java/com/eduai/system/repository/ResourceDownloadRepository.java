package com.eduai.system.repository;

import com.eduai.system.entity.ResourceDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceDownloadRepository extends JpaRepository<ResourceDownload, Long> {

    /** 该用户是否已下载过该资源（防重复扣费） */
    boolean existsByResourceIdAndUserId(Long resourceId, Long userId);
}
