package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureAuditRepository extends JpaRepository<FeatureAudit, Long> {

	Page<FeatureAudit> findByFlagIdOrderByCreatedAtDesc(Long flagId, Pageable pageable);
}
