package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedOverride;
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
	private final FeatureCacheService featureCacheService;
	private final AfterCommitExecutor afterCommitExecutor;

	@Transactional
	public FeatureOverrideResponse upsertOverride(
			String flagName,
			String userId,
			UpsertOverrideRequest request,
			String actor,
			String requestId) {
		validateUserId(userId);
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);

		var existing = overrideRepository.findByIdFlagIdAndIdUserId(flag.getId(), userId);
		Boolean previous = existing.map(FeatureOverride::getEnabled).orElse(null);
		log.debug(
				"Upsert override flag={} flagId={} userId={} previousEnabled={} newEnabled={} actor={}",
				flagName,
				flag.getId(),
				userId,
				previous,
				request.enabled(),
				actor
		);
		FeatureOverride override = existing.orElseGet(
				() -> new FeatureOverride(flag.getId(), userId, null, actor));
		override.upsertEnabled(request.enabled(), actor);
		FeatureOverride saved = overrideRepository.saveAndFlush(override);

		ChangeMap changeMap = ChangeMap.create()
				.put("userId", null, userId)
				.put("enabled", previous, saved.getEnabled());
		auditService.record(flag.getId(), "OVERRIDE_UPSERTED", actor, changeMap.toMap(), requestId);

		scheduleOverrideCachePut(flag.getId(), userId, saved);
		return FeatureOverrideResponse.from(saved, flagName);
	}

	@Transactional(readOnly = true)
	public Page<FeatureOverrideResponse> listOverrides(String flagName, Pageable pageable) {
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);
		return overrideRepository.findActiveByFlagId(flag.getId(), pageable)
				.map(override -> FeatureOverrideResponse.from(override, flagName));
	}

	@Transactional
	public void removeOverride(String flagName, String userId, String actor, String requestId) {
		validateUserId(userId);
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);

		FeatureOverride override = overrideRepository
				.findByIdFlagIdAndIdUserId(flag.getId(), userId)
				.orElseGet(() -> new FeatureOverride(flag.getId(), userId, null, actor));

		Boolean previous = override.getEnabled();
		log.debug(
				"Remove override flag={} flagId={} userId={} previousEnabled={} actor={}",
				flagName,
				flag.getId(),
				userId,
				previous,
				actor
		);
		override.markRemoved(actor);
		FeatureOverride saved = overrideRepository.saveAndFlush(override);

		ChangeMap changeMap = ChangeMap.create()
				.put("userId", null, userId)
				.put("enabled", previous, null);
		auditService.record(flag.getId(), "OVERRIDE_REMOVED", actor, changeMap.toMap(), requestId);

		scheduleOverrideCachePut(flag.getId(), userId, saved);
	}

	private void scheduleOverrideCachePut(Long flagId, String userId, FeatureOverride override) {
		CachedOverride cached = CachedOverride.from(override);
		afterCommitExecutor.execute(() -> {
			log.debug(
					"After-commit cache put override flagId={} userId={} value={}",
					flagId,
					userId,
					cached.value()
			);
			featureCacheService.putOverride(flagId, userId, cached);
		});
	}

	private void validateUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > 150) {
			throw new IllegalArgumentException("userId must be 1-150 characters");
		}
	}
}
