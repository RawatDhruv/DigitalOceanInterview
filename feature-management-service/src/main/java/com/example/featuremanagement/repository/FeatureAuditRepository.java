package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeatureAuditRepository extends JpaRepository<FeatureAudit, UUID> {

	Page<FeatureAudit> findByFlagIdOrderByCreatedAtDesc(UUID flagId, Pageable pageable);
}
