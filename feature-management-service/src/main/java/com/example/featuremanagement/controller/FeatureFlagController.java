package com.example.featuremanagement.controller;

import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.CreateFlagResult;
import com.example.featuremanagement.dto.FeatureFlagResponse;
import com.example.featuremanagement.dto.UpdateFlagRequest;
import com.example.featuremanagement.service.FeatureFlagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
@Validated
public class FeatureFlagController {

	private static final String ACTOR_HEADER = "X-Actor-Id";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
	private static final String DEFAULT_ACTOR = "local-admin";

	private final FeatureFlagService featureFlagService;

	@PostMapping
	public ResponseEntity<FeatureFlagResponse> create(
			@Valid @RequestBody CreateFlagRequest request,
			@RequestHeader(value = ACTOR_HEADER, defaultValue = DEFAULT_ACTOR) String actor,
			@RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		CreateFlagResult result = featureFlagService.createFlag(
				request,
				actor,
				RequestIds.optional(idempotencyKey, requestId)
		);
		return ResponseEntity
				.status(result.idempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED)
				.body(result.flag());
	}

	@GetMapping
	public Page<FeatureFlagResponse> list(
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return featureFlagService.listFlags(pageable);
	}

	@GetMapping("/{name}")
	public FeatureFlagResponse get(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name) {
		return featureFlagService.getFlag(name);
	}

	@PatchMapping("/{name}")
	public FeatureFlagResponse update(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@Valid @RequestBody UpdateFlagRequest request,
			@RequestHeader(value = ACTOR_HEADER, defaultValue = DEFAULT_ACTOR) String actor,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return featureFlagService.updateFlag(name, request, actor, RequestIds.resolve(requestId));
	}

	@DeleteMapping("/{name}")
	public ResponseEntity<Void> delete(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@RequestHeader(value = ACTOR_HEADER, defaultValue = DEFAULT_ACTOR) String actor,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		featureFlagService.deleteFlag(name, actor, RequestIds.resolve(requestId));
		return ResponseEntity.noContent().build();
	}
}
