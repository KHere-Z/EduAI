package com.eduai.security.repository;

import com.eduai.security.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 机构 Repository
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}