package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

	Optional<FeatureFlag> findByName(String name);

	boolean existsByName(String name);
}
