package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.FeatureAudit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FeatureAuditResponse(
		UUID id,
		UUID flagId,
		String action,
		String actorId,
		Map<String, Map<String, Object>> changeMap,
		String requestId,
		Instant createdAt
) {

	public static FeatureAuditResponse from(FeatureAudit audit) {
		return new FeatureAuditResponse(
				audit.getId(),
				audit.getFlagId(),
				audit.getAction(),
				audit.getActorId(),
				audit.getChangeMap(),
				audit.getRequestId(),
				audit.getCreatedAt()
		);
	}
}
