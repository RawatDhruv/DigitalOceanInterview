package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.CreateFlagResult;
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

import java.util.UUID;

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
		log.debug("DB lookup feature flag by name={}", name);
		return repository.findByName(name)
				.orElseThrow(() -> {
					log.debug("Feature flag not found name={}", name);
					return new FeatureNotFoundException(name);
				});
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
	public CreateFlagResult createFlag(CreateFlagRequest request, String actor, String clientRequestKey) {
		if (clientRequestKey != null && !clientRequestKey.isBlank()) {
			var existingFlagId = auditService.findFlagIdForIdempotentRequest(clientRequestKey, "FLAG_CREATED");
			if (existingFlagId.isPresent()) {
				FeatureFlag existing = repository.findById(existingFlagId.get())
						.orElseThrow(() -> new FeatureNotFoundException(request.name()));
				log.debug(
						"Idempotent create replay requestKey={} flagId={} name={}",
						clientRequestKey,
						existing.getId(),
						existing.getName()
				);
				return new CreateFlagResult(FeatureFlagResponse.from(existing), true);
			}
		}

		if (repository.existsByName(request.name())) {
			log.debug("Create rejected; duplicate flag name={}", request.name());
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

		String auditRequestId = (clientRequestKey == null || clientRequestKey.isBlank())
				? UUID.randomUUID().toString()
				: clientRequestKey;
		auditService.record(saved.getId(), "FLAG_CREATED", actor, changeMap.toMap(), auditRequestId);
		log.debug(
				"Flag created id={} name={} globalEnabled={} state={} actor={}",
				saved.getId(),
				saved.getName(),
				saved.isGlobalEnabled(),
				saved.getState(),
				actor
		);
		scheduleFlagCachePut(saved);
		return new CreateFlagResult(FeatureFlagResponse.from(saved), false);
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
			log.debug(
					"Flag updated id={} name={} changeMap={} actor={}",
					saved.getId(),
					saved.getName(),
					changeMap.toMap(),
					actor
			);
		}
		else {
			log.debug("Flag update no-op name={} actor={}", name, actor);
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
		log.debug("Flag deleted id={} name={} actor={}", flag.getId(), name, actor);
		repository.delete(flag);
		afterCommitExecutor.execute(() -> {
			log.debug("After-commit cache evict for deleted flag name={}", name);
			featureCacheService.evictFlag(name);
		});
	}

	private void scheduleFlagCachePut(FeatureFlag flag) {
		CachedFlag cached = CachedFlag.from(flag);
		afterCommitExecutor.execute(() -> {
			log.debug("After-commit cache put for flag name={} flagId={}", cached.name(), cached.flagId());
			featureCacheService.putFlag(cached);
		});
	}
}
