package com.eduai.security.repository;

import com.eduai.security.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付订单 Repository
 */
@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    /** 按商户订单号查询 */
    Optional<PaymentOrder> findByOrderId(String orderId);
}
