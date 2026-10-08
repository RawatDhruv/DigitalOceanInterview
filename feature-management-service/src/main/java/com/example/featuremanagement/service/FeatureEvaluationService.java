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
		log.debug("Evaluate start flag={} userId={}", flagName, userId);
		try {
			validateUserId(userId);

			CachedFlag flag = resolveFlag(flagName);
			CachedOverride override = resolveOverride(flag.flagId(), userId);

			if (override.value().isExplicit()) {
				log.debug(
						"Evaluate result flag={} userId={} enabled={} reason=USER_OVERRIDE overrideValue={}",
						flagName,
						userId,
						override.value().asBoolean(),
						override.value()
				);
				return new EvaluationResponse(
						flag.name(),
						userId,
						override.value().asBoolean(),
						EvaluationReason.USER_OVERRIDE
				);
			}

			log.debug(
					"Evaluate result flag={} userId={} enabled={} reason=GLOBAL (inherit/no explicit override)",
					flagName,
					userId,
					flag.globalEnabled()
			);
			return new EvaluationResponse(
					flag.name(),
					userId,
					flag.globalEnabled(),
					EvaluationReason.GLOBAL
			);
		}
		catch (FeatureNotFoundException | IllegalArgumentException | EvaluationUnavailableException ex) {
			log.debug("Evaluate failed flag={} userId={} error={}", flagName, userId, ex.toString());
			featureMetrics.recordEvaluationError();
			throw ex;
		}
		catch (RuntimeException ex) {
			log.debug("Evaluate unexpected failure flag={} userId={}", flagName, userId, ex);
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
			CachedFlag flag = cached.get();
			log.debug(
					"Flag cache hit name={} flagId={} globalEnabled={} updatedAt={}",
					flag.name(),
					flag.flagId(),
					flag.globalEnabled(),
					flag.updatedAt()
			);
			return flag;
		}

		log.debug("Flag cache miss name={}; looking up PostgreSQL", flagName);
		featureMetrics.recordDbFallback();
		FeatureFlag flag = dbFallbackGuard.execute(() -> featureFlagService.getFlagEntity(flagName));
		CachedFlag loaded = CachedFlag.from(flag);
		log.debug(
				"Flag loaded from DB name={} flagId={} globalEnabled={}; writing cache",
				loaded.name(),
				loaded.flagId(),
				loaded.globalEnabled()
		);
		featureCacheService.putFlag(loaded);
		return loaded;
	}

	private CachedOverride resolveOverride(Long flagId, String userId) {
		Optional<CachedOverride> cached = featureCacheService.getOverride(flagId, userId);
		if (cached.isPresent()) {
			CachedOverride override = cached.get();
			log.debug(
					"Override cache hit flagId={} userId={} value={} updatedAt={}",
					flagId,
					userId,
					override.value(),
					override.updatedAt()
			);
			return override;
		}

		log.debug("Override cache miss flagId={} userId={}; looking up PostgreSQL", flagId, userId);
		featureMetrics.recordDbFallback();
		Optional<FeatureOverride> fromDb = dbFallbackGuard.execute(
				() -> overrideRepository.findByIdFlagIdAndIdUserId(flagId, userId)
		);
		if (fromDb.isPresent()) {
			FeatureOverride entity = fromDb.get();
			CachedOverride loaded = CachedOverride.from(entity);
			if (loaded.value().isExplicit()) {
				log.debug(
						"User override found in DB flagId={} userId={} enabled={}; writing cache",
						flagId,
						userId,
						entity.getEnabled()
				);
			}
			else {
				log.debug(
						"Override tombstone (INHERIT) found in DB flagId={} userId={}; writing cache",
						flagId,
						userId
				);
			}
			featureCacheService.putOverride(flagId, userId, loaded);
			return loaded;
		}

		log.debug(
				"No override row in DB flagId={} userId={}; caching INHERIT and using global",
				flagId,
				userId
		);
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
