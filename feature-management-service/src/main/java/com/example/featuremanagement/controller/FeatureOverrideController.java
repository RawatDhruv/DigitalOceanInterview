package com.example.featuremanagement.controller;

import com.example.featuremanagement.dto.FeatureOverrideResponse;
import com.example.featuremanagement.dto.UpsertOverrideRequest;
import com.example.featuremanagement.service.FeatureOverrideService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flags/{name}/overrides")
@RequiredArgsConstructor
@Validated
public class FeatureOverrideController {

	private static final String ACTOR_HEADER = "X-Actor-Id";
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final String DEFAULT_ACTOR = "local-admin";

	private final FeatureOverrideService featureOverrideService;

	@PutMapping("/{userId}")
	public FeatureOverrideResponse upsert(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@PathVariable
			@Size(min = 1, max = 150)
			String userId,
			@Valid @RequestBody UpsertOverrideRequest request,
			@RequestHeader(value = ACTOR_HEADER, defaultValue = DEFAULT_ACTOR) String actor,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		return featureOverrideService.upsertOverride(
				name, userId, request, actor, RequestIds.resolve(requestId));
	}

	@GetMapping
	public Page<FeatureOverrideResponse> list(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return featureOverrideService.listOverrides(name, pageable);
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> remove(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@PathVariable
			@Size(min = 1, max = 150)
			String userId,
			@RequestHeader(value = ACTOR_HEADER, defaultValue = DEFAULT_ACTOR) String actor,
			@RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
		featureOverrideService.removeOverride(name, userId, actor, RequestIds.resolve(requestId));
		return ResponseEntity.noContent().build();
	}
}
