package com.example.featuremanagement.controller;

import com.example.featuremanagement.dto.EvaluationResponse;
import com.example.featuremanagement.service.FeatureEvaluationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@Validated
public class FeatureEvaluationController {

	private final FeatureEvaluationService featureEvaluationService;

	@GetMapping("/{flagName}")
	public EvaluationResponse evaluate(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String flagName,
			@RequestParam
			@NotBlank
			@Size(min = 1, max = 150)
			String userId) {
		return featureEvaluationService.evaluate(flagName, userId);
	}
}
