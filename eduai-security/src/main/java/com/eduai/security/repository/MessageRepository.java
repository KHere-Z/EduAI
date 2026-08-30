package com.eduai.security.repository;

import com.eduai.security.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 站内消息 Repository
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 按接收者分页查询（时间倒序） */
    Page<Message> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 未读数量 */
    long countByUserIdAndReadFlagFalse(Long userId);

    /** 全部标记已读 */
    @Modifying
    @Query("update Message m set m.readFlag = true where m.userId = :userId and m.readFlag = false")
    int markAllRead(Long userId);
}
