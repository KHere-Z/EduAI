package com.eduai.security.repository;

import com.eduai.security.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员 Repository
 */
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    /** 按用户ID查询 */
    Optional<Membership> findByUserId(Long userId);

    /** 按用户ID和状态查询有效会员 */
    Optional<Membership> findByUserIdAndStatus(Long userId, String status);
}
