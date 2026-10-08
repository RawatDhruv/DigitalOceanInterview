package com.example.featuremanagement.cache;

import com.example.featuremanagement.entity.FeatureOverride;

import java.time.Instant;

public record CachedOverride(
		OverrideCacheValue value,
		Instant updatedAt
) {
	public static CachedOverride from(FeatureOverride override) {
		return new CachedOverride(
				OverrideCacheValue.fromEnabled(override.getEnabled()),
				override.getUpdatedAt() != null ? override.getUpdatedAt() : Instant.EPOCH
		);
	}

	public static CachedOverride inherit(Instant updatedAt) {
		return new CachedOverride(OverrideCacheValue.INHERIT, updatedAt);
	}
}
