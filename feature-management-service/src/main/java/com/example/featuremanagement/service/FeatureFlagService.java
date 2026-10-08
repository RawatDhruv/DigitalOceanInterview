package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.FeatureFlagResponse;
import com.example.featuremanagement.dto.UpdateFlagRequest;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FlagState;
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
	private final FeatureCacheService featureCacheService;
	private final AfterCommitExecutor afterCommitExecutor;

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
	public FeatureFlagResponse createFlag(CreateFlagRequest request, String actor, String requestId) {
		if (repository.existsByName(request.name())) {
			throw new DuplicateFeatureException(request.name());
		}

		FeatureFlag flag = new FeatureFlag(
				request.name(),
				request.description(),
				request.globalEnabled(),
				actor
		);
		FlagState initialState = request.state() != null ? request.state() : FlagState.DRAFT;
		if (request.state() != null) {
			flag.updateState(request.state(), actor);
		}

		FeatureFlag saved = repository.saveAndFlush(flag);

		ChangeMap changeMap = ChangeMap.create()
				.put("name", null, saved.getName())
				.put("description", null, saved.getDescription())
				.put("globalEnabled", null, saved.isGlobalEnabled())
				.put("state", null, initialState.name());

		auditService.record(saved.getId(), "FLAG_CREATED", actor, changeMap.toMap(), requestId);
		scheduleFlagCachePut(saved);
		return FeatureFlagResponse.from(saved);
	}

	@Transactional
	public FeatureFlagResponse updateFlag(String name, UpdateFlagRequest request, String actor, String requestId) {
		FeatureFlag flag = getFlagEntity(name);

		ChangeMap changeMap = ChangeMap.create();
		if (request.description() != null) {
			changeMap.put("description", flag.getDescription(), request.description());
			flag.updateDescription(request.description(), actor);
		}
		if (request.globalEnabled() != null) {
			changeMap.put("globalEnabled", flag.isGlobalEnabled(), request.globalEnabled());
			flag.updateGlobalEnabled(request.globalEnabled(), actor);
		}
		if (request.state() != null) {
			changeMap.put("state", flag.getState().name(), request.state().name());
			flag.updateState(request.state(), actor);
		}

		FeatureFlag saved = repository.saveAndFlush(flag);
		if (!changeMap.isEmpty()) {
			auditService.record(saved.getId(), "FLAG_UPDATED", actor, changeMap.toMap(), requestId);
		}
		scheduleFlagCachePut(saved);
		return FeatureFlagResponse.from(saved);
	}

	@Transactional
	public void deleteFlag(String name, String actor, String requestId) {
		FeatureFlag flag = getFlagEntity(name);

		ChangeMap changeMap = ChangeMap.create()
				.put("name", flag.getName(), null)
				.put("description", flag.getDescription(), null)
				.put("globalEnabled", flag.isGlobalEnabled(), null)
				.put("state", flag.getState().name(), null);

		auditService.record(flag.getId(), "FLAG_DELETED", actor, changeMap.toMap(), requestId);
		repository.delete(flag);
		afterCommitExecutor.execute(() -> featureCacheService.evictFlag(name));
	}

	private void scheduleFlagCachePut(FeatureFlag flag) {
		CachedFlag cached = CachedFlag.from(flag);
		afterCommitExecutor.execute(() -> featureCacheService.putFlag(cached));
	}
}
