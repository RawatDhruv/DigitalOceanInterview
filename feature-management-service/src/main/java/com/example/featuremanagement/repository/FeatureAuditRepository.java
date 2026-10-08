package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureAuditRepository extends JpaRepository<FeatureAudit, Long> {

	Page<FeatureAudit> findByFlagIdOrderByCreatedAtDesc(Long flagId, Pageable pageable);

	Optional<FeatureAudit> findFirstByRequestIdAndActionOrderByCreatedAtAsc(String requestId, String action);
}
