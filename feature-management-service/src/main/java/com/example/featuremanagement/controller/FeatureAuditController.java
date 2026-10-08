package com.example.featuremanagement.controller;

import com.example.featuremanagement.dto.FeatureAuditResponse;
import com.example.featuremanagement.service.FeatureAuditQueryService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flags/{name}/audits")
@RequiredArgsConstructor
@Validated
public class FeatureAuditController {

	private final FeatureAuditQueryService featureAuditQueryService;

	@GetMapping
	public Page<FeatureAuditResponse> list(
			@PathVariable
			@Size(min = 1, max = 150)
			@Pattern(regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$")
			String name,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return featureAuditQueryService.listAudits(name, pageable);
	}
}
