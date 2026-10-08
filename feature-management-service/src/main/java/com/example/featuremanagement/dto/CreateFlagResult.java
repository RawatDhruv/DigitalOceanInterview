package com.example.featuremanagement.dto;

public record CreateFlagResult(
		FeatureFlagResponse flag,
		boolean idempotentReplay
) {
}
