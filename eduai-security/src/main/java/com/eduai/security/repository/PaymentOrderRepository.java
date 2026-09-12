package com.eduai.security.repository;

import com.eduai.security.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付订单 Repository
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    /** 按商户订单号查询 */
    Optional<PaymentOrder> findByOrderId(String orderId);

    /**
     * 原子认领订单：仅当仍为 pending 时才置为 paid。
     * <p>
     * 返回受影响行数，调用方据此判断「本次是否抢到了处理权」，
     * 用 DB 层的原子性防住渠道重复回调导致的重复发点。
     *
     * @return 1 = 本次认领成功；0 = 已被其他回调处理过（或订单不存在/已关闭）
     */
    @Modifying
    @Query("update PaymentOrder o set o.status = 'paid', o.transactionId = :txId, o.updatedAt = CURRENT_TIMESTAMP "
            + "where o.orderId = :orderId and o.status = 'pending'")
    int markPaid(@Param("orderId") String orderId, @Param("txId") String transactionId);
}
