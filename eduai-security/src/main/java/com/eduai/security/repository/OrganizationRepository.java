package com.eduai.security.repository;

import com.eduai.security.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 机构 Repository
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /** 按名称查找机构 */
    Optional<Organization> findByName(String name);
}