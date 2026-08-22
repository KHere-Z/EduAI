package com.eduai.security.repository;

import com.eduai.security.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按 uid 查找 */
    Optional<User> findByUid(Long uid);

    /** 按手机号查找 */
    Optional<User> findByPhone(String phone);

    /** 手机号是否已存在 */
    boolean existsByPhone(String phone);

    /** 按角色类型查询用户列表 */
    List<User> findByRoleType(Integer roleType);

    /** 按角色类型统计用户数 */
    long countByRoleType(Integer roleType);

    /**
     * 按 ID 加悲观写锁查询（SELECT ... FOR UPDATE），用于智学点扣减等需防并发超扣的场景。
     * 必须在事务内调用，否则锁立即释放无效。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
