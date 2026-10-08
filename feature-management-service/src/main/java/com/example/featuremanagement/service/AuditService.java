package com.example.featuremanagement.service;

import com.example.featuremanagement.entity.FeatureAudit;
import com.example.featuremanagement.repository.FeatureAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

	private final FeatureAuditRepository auditRepository;

	@Transactional
	public void record(
			Long flagId,
			String action,
			String actorId,
			Map<String, Map<String, Object>> changeMap,
			String requestId) {
		FeatureAudit audit = new FeatureAudit(flagId, action, actorId, changeMap, requestId);
		auditRepository.save(audit);
		log.debug("Persisted audit: action={}, flagId={}, actor={}", action, flagId, actorId);
	}
}
