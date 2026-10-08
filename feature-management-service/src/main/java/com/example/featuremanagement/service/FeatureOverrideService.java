package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.FeatureOverrideResponse;
import com.example.featuremanagement.dto.UpsertOverrideRequest;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureOverrideService {

	private final FeatureFlagService featureFlagService;
	private final FeatureOverrideRepository overrideRepository;
	private final AuditService auditService;

	@Transactional
	public FeatureOverrideResponse upsertOverride(
			String flagName,
			String userId,
			UpsertOverrideRequest request,
			String actor) {
		validateUserId(userId);
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);

		FeatureOverride override = overrideRepository
				.findByIdFlagIdAndIdUserId(flag.getId(), userId)
				.orElseGet(() -> new FeatureOverride(flag.getId(), userId, request.enabled(), actor));

		override.upsertEnabled(request.enabled(), actor);
		FeatureOverride saved = overrideRepository.save(override);
		auditService.record("OVERRIDE_UPSERTED", flagName, actor);
		return FeatureOverrideResponse.from(saved, flagName);
	}

	@Transactional(readOnly = true)
	public Page<FeatureOverrideResponse> listOverrides(String flagName, Pageable pageable) {
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);
		return overrideRepository.findActiveByFlagId(flag.getId(), pageable)
				.map(override -> FeatureOverrideResponse.from(override, flagName));
	}

	@Transactional
	public void removeOverride(String flagName, String userId, String actor) {
		validateUserId(userId);
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);

		FeatureOverride override = overrideRepository
				.findByIdFlagIdAndIdUserId(flag.getId(), userId)
				.orElseGet(() -> new FeatureOverride(flag.getId(), userId, null, actor));

		override.markRemoved(actor);
		overrideRepository.save(override);
		auditService.record("OVERRIDE_REMOVED", flagName, actor);
	}

	private void validateUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > 150) {
			throw new IllegalArgumentException("userId must be 1-150 characters");
		}
	}
}
