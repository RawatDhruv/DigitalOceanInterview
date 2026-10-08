package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.cache.CachedOverride;
import com.example.featuremanagement.dto.EvaluationResponse;
import com.example.featuremanagement.entity.EvaluationReason;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.exception.EvaluationUnavailableException;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEvaluationService {

	private final FeatureFlagService featureFlagService;
	private final FeatureOverrideRepository overrideRepository;
	private final FeatureCacheService featureCacheService;
	private final DbFallbackGuard dbFallbackGuard;
	private final FeatureMetrics featureMetrics;

	@Transactional(readOnly = true)
	public EvaluationResponse evaluate(String flagName, String userId) {
		long start = System.nanoTime();
		try {
			validateUserId(userId);

			CachedFlag flag = resolveFlag(flagName);
			CachedOverride override = resolveOverride(flag.flagId(), userId);

			if (override.value().isExplicit()) {
				log.debug("Evaluating flag={} userId={} reason=USER_OVERRIDE enabled={} (cache-aware)",
						flagName, userId, override.value().asBoolean());
				return new EvaluationResponse(
						flag.name(),
						userId,
						override.value().asBoolean(),
						EvaluationReason.USER_OVERRIDE
				);
			}

			log.debug("Evaluating flag={} userId={} reason=GLOBAL enabled={} (cache-aware)",
					flagName, userId, flag.globalEnabled());
			return new EvaluationResponse(
					flag.name(),
					userId,
					flag.globalEnabled(),
					EvaluationReason.GLOBAL
			);
		}
		catch (FeatureNotFoundException | IllegalArgumentException | EvaluationUnavailableException ex) {
			featureMetrics.recordEvaluationError();
			throw ex;
		}
		catch (RuntimeException ex) {
			featureMetrics.recordEvaluationError();
			throw new EvaluationUnavailableException("Evaluation unavailable", ex);
		}
		finally {
			featureMetrics.recordEvaluationLatency(System.nanoTime() - start);
		}
	}

	private CachedFlag resolveFlag(String flagName) {
		Optional<CachedFlag> cached = featureCacheService.getFlag(flagName);
		if (cached.isPresent()) {
			return cached.get();
		}

		featureMetrics.recordDbFallback();
		FeatureFlag flag = dbFallbackGuard.execute(() -> featureFlagService.getFlagEntity(flagName));
		CachedFlag loaded = CachedFlag.from(flag);
		featureCacheService.putFlag(loaded);
		return loaded;
	}

	private CachedOverride resolveOverride(Long flagId, String userId) {
		Optional<CachedOverride> cached = featureCacheService.getOverride(flagId, userId);
		if (cached.isPresent()) {
			return cached.get();
		}

		featureMetrics.recordDbFallback();
		Optional<FeatureOverride> fromDb = dbFallbackGuard.execute(
				() -> overrideRepository.findByIdFlagIdAndIdUserId(flagId, userId)
		);
		if (fromDb.isPresent()) {
			CachedOverride loaded = CachedOverride.from(fromDb.get());
			featureCacheService.putOverride(flagId, userId, loaded);
			return loaded;
		}

		CachedOverride inherit = CachedOverride.inherit(Instant.now());
		featureCacheService.putInherit(flagId, userId, inherit.updatedAt());
		return inherit;
	}

	private void validateUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > 150) {
			throw new IllegalArgumentException("userId must be 1-150 characters");
		}
	}
}
