package com.eduai.security.repository;

import com.eduai.security.entity.UserRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户关系数据访问层
 */
@Repository
public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {

    /** 查询双方已存在的任意关系（用于防止重复请求） */
    Optional<UserRelation> findByFromUidAndToUid(Long fromUid, Long toUid);

    /** 我发出的关系 */
    List<UserRelation> findByFromUid(Long fromUid);

    /** 发给我的关系 */
    List<UserRelation> findByToUid(Long toUid);

    /** 发给我的待处理请求 */
    List<UserRelation> findByToUidAndStatus(Long toUid, String status);

    /** 我发出的 + 发给我的已接受关系 */
    List<UserRelation> findByFromUidOrToUidAndStatus(Long fromUid, Long toUid, String status);

    /** 双方存在已接受关系 */
    boolean existsByFromUidAndToUidAndStatus(Long fromUid, Long toUid, String status);
    boolean existsByFromUidAndToUidAndStatusOrFromUidAndToUidAndStatus(
            Long fromUid1, Long toUid1, String status1,
            Long fromUid2, Long toUid2, String status2);
}
