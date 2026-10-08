package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.entity.FeatureOverrideId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeatureOverrideRepository extends JpaRepository<FeatureOverride, FeatureOverrideId> {

	Optional<FeatureOverride> findByIdFlagIdAndIdUserId(Long flagId, String userId);

	@Query("""
			SELECT o FROM FeatureOverride o
			WHERE o.id.flagId = :flagId
			  AND o.enabled IS NOT NULL
			""")
	Page<FeatureOverride> findActiveByFlagId(@Param("flagId") Long flagId, Pageable pageable);
}
