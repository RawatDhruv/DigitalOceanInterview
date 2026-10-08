package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.FeatureOverride;

import java.time.Instant;
import java.util.UUID;

public record FeatureOverrideResponse(
		UUID flagId,
		String flagName,
		String userId,
		Boolean enabled,
		String updatedBy,
		Instant updatedAt
) {
	public static FeatureOverrideResponse from(FeatureOverride override, String flagName) {
		return new FeatureOverrideResponse(
				override.getFlagId(),
				flagName,
				override.getUserId(),
				override.getEnabled(),
				override.getUpdatedBy(),
				override.getUpdatedAt()
		);
	}
}
