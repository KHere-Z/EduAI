package com.eduai.security.repository;

import com.eduai.security.entity.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * 点数变动记录 Repository
 */
@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    /** 按用户ID分页查询未删除的流水 */
    Page<PointTransaction> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 批量软删指定用户的变动记录，返回实际标记行数。
     * <p>
     * 行本身保留（支付对账需要），只置 {@code deleted = true}，因此不影响余额。
     * <ul>
     *   <li>{@code user_id} 条件不可省略：只按 id 会越权删掉别人的流水。</li>
     *   <li>{@code deleted = false} 让重复提交/已删除 id 计为 0，返回的行数才是「本次真正删掉的」。</li>
     *   <li>调用方须先过滤空集合（{@code IN ()} 是非法 SQL）。</li>
     * </ul>
     */
    @Modifying(clearAutomatically = true)
    @Query("update PointTransaction t set t.deleted = true "
            + "where t.userId = :userId and t.id in :ids and t.deleted = false")
    int softDeleteOwnedByIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
