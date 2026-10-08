package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.EvaluationResponse;
import com.example.featuremanagement.entity.EvaluationReason;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureEvaluationServiceTest {

	@Mock
	private FeatureFlagService featureFlagService;

	@Mock
	private FeatureOverrideRepository overrideRepository;

	@InjectMocks
	private FeatureEvaluationService evaluationService;

	private FeatureFlag enabledFlag;
	private FeatureFlag disabledFlag;
	private UUID flagId;

	@BeforeEach
	void setUp() {
		flagId = UUID.randomUUID();
		enabledFlag = new FeatureFlag("new-checkout", "Checkout", true, "admin-1");
		ReflectionTestUtils.setField(enabledFlag, "id", flagId);
		disabledFlag = new FeatureFlag("new-checkout", "Checkout", false, "admin-1");
		ReflectionTestUtils.setField(disabledFlag, "id", flagId);
	}

	@Test
	void globalTrueWhenOverrideMissing() {
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.empty());

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void globalFalseWhenOverrideMissing() {
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(disabledFlag);
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.empty());

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isFalse();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void userOverrideFalseWinsOverGlobalTrue() {
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		FeatureOverride override = new FeatureOverride(flagId, "user-1", false, "admin-1");
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(override));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isFalse();
		assertThat(result.reason()).isEqualTo(EvaluationReason.USER_OVERRIDE);
	}

	@Test
	void userOverrideTrueWinsOverGlobalFalse() {
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(disabledFlag);
		FeatureOverride override = new FeatureOverride(flagId, "user-1", true, "admin-1");
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(override));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.USER_OVERRIDE);
	}

	@Test
	void nullOverrideFallsBackToGlobal() {
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(enabledFlag);
		FeatureOverride tombstone = new FeatureOverride(flagId, "user-1", null, "admin-1");
		when(overrideRepository.findByIdFlagIdAndIdUserId(flagId, "user-1")).thenReturn(Optional.of(tombstone));

		EvaluationResponse result = evaluationService.evaluate("new-checkout", "user-1");

		assertThat(result.enabled()).isTrue();
		assertThat(result.reason()).isEqualTo(EvaluationReason.GLOBAL);
	}

	@Test
	void missingFlagThrows() {
		when(featureFlagService.getFlagEntity("missing"))
				.thenThrow(new FeatureNotFoundException("missing"));

		assertThatThrownBy(() -> evaluationService.evaluate("missing", "user-1"))
				.isInstanceOf(FeatureNotFoundException.class);
	}
}
