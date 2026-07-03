package com.eduai.security.repository;

import com.eduai.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * 按角色类型查询用户列表
     */
    List<User> findByRoleType(Integer roleType);

    /**
     * 按角色类型统计用户数
     */
    long countByRoleType(Integer roleType);
}