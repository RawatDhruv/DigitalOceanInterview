package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.EvaluationReason;

public record EvaluationResponse(
		String flag,
		String userId,
		boolean enabled,
		EvaluationReason reason
) {
}
