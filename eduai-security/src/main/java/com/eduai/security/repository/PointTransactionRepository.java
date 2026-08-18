package com.eduai.security.repository;

import com.eduai.security.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 点数变动记录 Repository
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /** 按用户ID分页查询 */
    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
