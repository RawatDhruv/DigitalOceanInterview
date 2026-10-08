package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.FeatureAuditResponse;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.repository.FeatureAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeatureAuditQueryService {

	private final FeatureFlagService featureFlagService;
	private final FeatureAuditRepository auditRepository;

	@Transactional(readOnly = true)
	public Page<FeatureAuditResponse> listAudits(String flagName, Pageable pageable) {
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);
		return auditRepository.findByFlagIdOrderByCreatedAtDesc(flag.getId(), pageable)
				.map(FeatureAuditResponse::from);
	}
}
