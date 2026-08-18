package com.eduai.security.repository;

import com.eduai.security.entity.UserWechat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 微信绑定数据访问层
 */
@Repository
public interface UserWechatRepository extends JpaRepository<UserWechat, Long> {

    /** 按 unionid 查找绑定 */
    Optional<UserWechat> findByUnionid(String unionid);

    /** 按用户 uid 查找绑定 */
    Optional<UserWechat> findByUserUid(Long userUid);

    /** unionid 是否已被某用户绑定 */
    boolean existsByUnionid(String unionid);

    /** 该 unionid 是否已被其他用户绑定（排除指定 uid） */
    boolean existsByUnionidAndUserUidNot(String unionid, Long userUid);
}
