package com.example.featuremanagement.cache;

import com.example.featuremanagement.entity.FeatureFlag;

import java.time.Instant;

public record CachedFlag(
		Long flagId,
		String name,
		boolean globalEnabled,
		Instant updatedAt
) {
	public static CachedFlag from(FeatureFlag flag) {
		return new CachedFlag(
				flag.getId(),
				flag.getName(),
				flag.isGlobalEnabled(),
				flag.getUpdatedAt() != null ? flag.getUpdatedAt() : Instant.EPOCH
		);
	}
}
