package com.example.featuremanagement.service;

import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

	private final FeatureFlagRepository repository;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public FeatureFlag getFlag(String name) {
		log.debug("Fetching feature flag: {}", name);

		return repository.findByName(name)
				.orElseThrow(() -> new FeatureNotFoundException(name));
	}
}
