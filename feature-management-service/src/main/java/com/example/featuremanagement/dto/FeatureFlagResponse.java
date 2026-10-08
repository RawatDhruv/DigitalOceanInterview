package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FlagState;

import java.time.Instant;

public record FeatureFlagResponse(
		Long id,
		String name,
		String description,
		boolean globalEnabled,
		FlagState state,
		Long version,
		String createdBy,
		Instant createdAt,
		String updatedBy,
		Instant updatedAt
) {
	public static FeatureFlagResponse from(FeatureFlag flag) {
		return new FeatureFlagResponse(
				flag.getId(),
				flag.getName(),
				flag.getDescription(),
				flag.isGlobalEnabled(),
				flag.getState(),
				flag.getVersion(),
				flag.getCreatedBy(),
				flag.getCreatedAt(),
				flag.getUpdatedBy(),
				flag.getUpdatedAt()
		);
	}
}
