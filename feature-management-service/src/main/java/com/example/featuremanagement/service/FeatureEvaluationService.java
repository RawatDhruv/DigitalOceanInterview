package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.EvaluationResponse;
import com.example.featuremanagement.entity.EvaluationReason;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEvaluationService {

	private final FeatureFlagService featureFlagService;
	private final FeatureOverrideRepository overrideRepository;

	@Transactional(readOnly = true)
	public EvaluationResponse evaluate(String flagName, String userId) {
		validateUserId(userId);
		FeatureFlag flag = featureFlagService.getFlagEntity(flagName);

		FeatureOverride override = overrideRepository
				.findByIdFlagIdAndIdUserId(flag.getId(), userId)
				.orElse(null);

		if (override != null && override.isActiveOverride()) {
			log.debug("Evaluating flag={} userId={} reason=USER_OVERRIDE enabled={}",
					flagName, userId, override.getEnabled());
			return new EvaluationResponse(
					flag.getName(),
					userId,
					override.getEnabled(),
					EvaluationReason.USER_OVERRIDE
			);
		}

		log.debug("Evaluating flag={} userId={} reason=GLOBAL enabled={}",
				flagName, userId, flag.isGlobalEnabled());
		return new EvaluationResponse(
				flag.getName(),
				userId,
				flag.isGlobalEnabled(),
				EvaluationReason.GLOBAL
		);
	}

	private void validateUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > 150) {
			throw new IllegalArgumentException("userId must be 1-150 characters");
		}
	}
}
