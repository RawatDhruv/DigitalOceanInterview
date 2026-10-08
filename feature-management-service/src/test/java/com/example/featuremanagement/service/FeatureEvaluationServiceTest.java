package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.cache.CachedOverride;
import com.example.featuremanagement.cache.OverrideCacheValue;
import com.example.featuremanagement.config.ResilienceProperties;
import com.example.featuremanagement.dto.EvaluationResponse;
import com.example.featuremanagement.entity.EvaluationReason;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureEvaluationServiceTest {

	@Mock
	private FeatureFlagService featureFlagService;

	@Mock
	private FeatureOverrideRepository overrideRepository;

	@Mock
	private FeatureCacheService featureCacheService;

	private FeatureEvaluationService evaluationService;

	private FeatureFlag enabledFlag;
	private FeatureFlag disabledFlag;
	private Long flagId;

	@BeforeEach
	void setUp() {
		flagId = 42L;
		enabledFlag = new FeatureFlag("new-checkout", "Checkout", true, "admin-1");
		ReflectionTestUtils.setField(enabledFlag, "id", flagId);
		ReflectionTestUtils.setField(enabledFlag, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
		disabledFlag = new FeatureFlag("new-checkout", "Checkout", false, "admin-1");
		ReflectionTestUtils.setField(disabledFlag, "id", flagId);
		ReflectionTestUtils.setField(disabledFlag, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));

		DbFallbackGuard guard = new DbFallbackGuard(new ResilienceProperties(32, 5, Duration.ofSeconds(2)));
		FeatureMetrics metrics = new FeatureMetrics(new SimpleMeterRegistry());
		evaluationService = new FeatureEvaluationService(
				featureFlagService,
				overrideRepository,
				featureCacheService,
				guard,
				metrics
		);
	}

	@Test
	void globalTrueWhenOverrideMissing() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.empty());
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.empty());

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
		verify(featureCacheService).putFlag(any(CachedFlag.class));
		verify(featureCacheService).putInherit(eq(flagId), eq("user-1"), any(Instant.class));
	}

	@Test
	void usesCachedFlagAndOverrideWithoutHittingDatabase() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.of(
				new CachedFlag(flagId, "new-checkout", true, Instant.parse("2026-01-01T00:00:00Z"))
		));
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.of(
				new CachedOverride(OverrideCacheValue.FALSE, Instant.parse("2026-01-01T00:00:01Z"))
		));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isFalse();
		assertThat(result.reason()).isEqualTo(EvaluationReason.USER_OVERRIDE);
		verify(featureFlagService, never()).getFlagEntity(any());
		verify(overrideRepository, never()).findByIdFlagIdAndIdUserId(any(), any());
	}

	@Test
	void globalFalseWhenOverrideMissing() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(disabledFlag);
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.empty());
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.empty());

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isFalse();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void userOverrideFalseWinsOverGlobalTrue() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.empty());
		FeatureOverride override = new FeatureOverride(flagId, "user-1", false, "admin-1");
		ReflectionTestUtils.setField(override, "updatedAt", Instant.parse("2026-01-01T00:00:02Z"));
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(override));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isFalse();
		assertThat(result.reason()).isEqualTo(EvaluationReason.USER_OVERRIDE);
		verify(featureCacheService).putOverride(eq(flagId), eq("user-1"), any(CachedOverride.class));
	}

	@Test
	void userOverrideTrueWinsOverGlobalFalse() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(disabledFlag);
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.empty());
		FeatureOverride override = new FeatureOverride(flagId, "user-1", true, "admin-1");
		ReflectionTestUtils.setField(override, "updatedAt", Instant.parse("2026-01-01T00:00:02Z"));
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(override));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.USER_OVERRIDE);
	}

	@Test
	void nullOverrideFallsBackToGlobal() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.empty());
		FeatureOverride tombstone = new FeatureOverride(flagId, "user-1", null, "admin-1");
		ReflectionTestUtils.setField(tombstone, "updatedAt", Instant.parse("2026-01-01T00:00:02Z"));
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(tombstone));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void cachedInheritUsesGlobal() {
		when(featureCacheService.getFlag("new-checkout")).thenReturn(Optional.of(
				new CachedFlag(flagId, "new-checkout", true, Instant.parse("2026-01-01T00:00:00Z"))
		));
		when(featureCacheService.getOverride(flagId, "user-1")).thenReturn(Optional.of(
				CachedOverride.inherit(Instant.parse("2026-01-01T00:00:01Z"))
		));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void missingFlagThrows() {
		when(featureCacheService.getFlag("missing")).thenReturn(Optional.empty());
		when(featureFlagService.getFlagEntity("missing"))
				.thenThrow(new FeatureNotFoundException("missing"));

		assertThatThrownBy(() -> evaluationService.evaluate("missing", "user-1"))
				.isInstanceOf(FeatureNotFoundException.class);
	}
}
