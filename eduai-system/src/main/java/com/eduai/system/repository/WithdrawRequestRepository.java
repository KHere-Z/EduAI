package com.eduai.system.repository;

import com.eduai.system.entity.WithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {

    List<WithdrawRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<WithdrawRequest> findAllByOrderByCreatedAtDesc();

    /** 老师已占用（未驳回）的提现总额（分） */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawRequest w WHERE w.teacherId = :teacherId AND w.status <> 'rejected'")
    long sumNonRejectedAmountByTeacherId(@Param("teacherId") Long teacherId);
}
