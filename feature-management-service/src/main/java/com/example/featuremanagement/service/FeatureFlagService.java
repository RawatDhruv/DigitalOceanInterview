package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.FeatureFlagResponse;
import com.example.featuremanagement.dto.UpdateFlagRequest;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.exception.DuplicateFeatureException;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

	private final FeatureFlagRepository repository;
	private final AuditService auditService;

	@Transactional(readOnly = true)
	public FeatureFlag getFlagEntity(String name) {
		log.debug("Fetching feature flag: {}", name);
		return repository.findByName(name)
				.orElseThrow(() -> new FeatureNotFoundException(name));
	}

	@Transactional(readOnly = true)
	public FeatureFlagResponse getFlag(String name) {
		return FeatureFlagResponse.from(getFlagEntity(name));
	}

	@Transactional(readOnly = true)
	public Page<FeatureFlagResponse> listFlags(Pageable pageable) {
		return repository.findAll(pageable).map(FeatureFlagResponse::from);
	}

	@Transactional
	public FeatureFlagResponse createFlag(CreateFlagRequest request, String actor) {
		if (repository.existsByName(request.name())) {
			throw new DuplicateFeatureException(request.name());
		}

		FeatureFlag flag = new FeatureFlag(
				request.name(),
				request.description(),
				request.globalEnabled(),
				actor
		);
		if (request.state() != null) {
			flag.updateState(request.state(), actor);
		}

		FeatureFlag saved = repository.save(flag);
		auditService.record("FLAG_CREATED", saved.getName(), actor);
		return FeatureFlagResponse.from(saved);
	}

	@Transactional
	public FeatureFlagResponse updateFlag(String name, UpdateFlagRequest request, String actor) {
		FeatureFlag flag = getFlagEntity(name);

		if (request.description() != null) {
			flag.updateDescription(request.description(), actor);
		}
		if (request.globalEnabled() != null) {
			flag.updateGlobalEnabled(request.globalEnabled(), actor);
		}
		if (request.state() != null) {
			flag.updateState(request.state(), actor);
		}

		FeatureFlag saved = repository.save(flag);
		auditService.record("FLAG_UPDATED", saved.getName(), actor);
		return FeatureFlagResponse.from(saved);
	}

	@Transactional
	public void deleteFlag(String name, String actor) {
		FeatureFlag flag = getFlagEntity(name);
		repository.delete(flag);
		auditService.record("FLAG_DELETED", name, actor);
	}
}
