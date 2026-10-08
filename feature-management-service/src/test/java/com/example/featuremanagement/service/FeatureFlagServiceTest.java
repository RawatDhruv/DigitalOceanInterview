package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.FeatureFlagResponse;
import com.example.featuremanagement.dto.UpdateFlagRequest;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FlagState;
import com.example.featuremanagement.exception.DuplicateFeatureException;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

	@Mock
	private FeatureFlagRepository repository;

	@Mock
	private AuditService auditService;

	@Mock
	private FeatureCacheService featureCacheService;

	@Spy
	private AfterCommitExecutor afterCommitExecutor = new AfterCommitExecutor();

	@InjectMocks
	private FeatureFlagService featureFlagService;

	@Test
	void getFlagReturnsResponse() {
		FeatureFlag flag = new FeatureFlag("new-checkout", "Checkout", true, "admin-1");
		when(repository.findByName("new-checkout")).thenReturn(Optional.of(flag));

		FeatureFlagResponse result = featureFlagService.getFlag("new-checkout");

		assertThat(result.name()).isEqualTo("new-checkout");
		assertThat(result.globalEnabled()).isTrue();
	}

	@Test
	void getFlagThrowsWhenMissing() {
		when(repository.findByName("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> featureFlagService.getFlag("missing"))
				.isInstanceOf(FeatureNotFoundException.class);
	}

	@Test
	void createFlagPersistsAndAudits() {
		when(repository.existsByName("dark-mode")).thenReturn(false);
		when(repository.saveAndFlush(any(FeatureFlag.class))).thenAnswer(invocation -> {
			FeatureFlag flag = invocation.getArgument(0);
			ReflectionTestUtils.setField(flag, "id", 1L);
			ReflectionTestUtils.setField(flag, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
			return flag;
		});

		FeatureFlagResponse created = featureFlagService.createFlag(
				new CreateFlagRequest("dark-mode", "Theme", false, FlagState.ACTIVE),
				"admin-1",
				"req-1"
		);

		assertThat(created.name()).isEqualTo("dark-mode");
		assertThat(created.state()).isEqualTo(FlagState.ACTIVE);
		verify(auditService).record(any(Long.class), eq("FLAG_CREATED"), eq("admin-1"), any(Map.class), eq("req-1"));
		verify(featureCacheService).putFlag(any());
	}

	@Test
	void createFlagRejectsDuplicate() {
		when(repository.existsByName("dark-mode")).thenReturn(true);

		assertThatThrownBy(() -> featureFlagService.createFlag(
				new CreateFlagRequest("dark-mode", null, true, null),
				"admin-1",
				"req-1"
		)).isInstanceOf(DuplicateFeatureException.class);
	}

	@Test
	void updateFlagAppliesPartialChanges() {
		FeatureFlag flag = new FeatureFlag("dark-mode", "Old", false, "admin-1");
		ReflectionTestUtils.setField(flag, "id", 1L);
		ReflectionTestUtils.setField(flag, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
		when(repository.findByName("dark-mode")).thenReturn(Optional.of(flag));
		when(repository.saveAndFlush(any(FeatureFlag.class))).thenAnswer(invocation -> {
			FeatureFlag saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "updatedAt", Instant.parse("2026-01-01T00:00:01Z"));
			return saved;
		});

		FeatureFlagResponse updated = featureFlagService.updateFlag(
				"dark-mode",
				new UpdateFlagRequest("New", true, FlagState.ACTIVE),
				"admin-2",
				"req-2"
		);

		assertThat(updated.description()).isEqualTo("New");
		assertThat(updated.globalEnabled()).isTrue();
		assertThat(updated.state()).isEqualTo(FlagState.ACTIVE);
		assertThat(updated.updatedBy()).isEqualTo("admin-2");
		verify(auditService).record(any(Long.class), eq("FLAG_UPDATED"), eq("admin-2"), any(Map.class), eq("req-2"));
		verify(featureCacheService).putFlag(any());
	}

	@Test
	void listFlagsMapsPage() {
		FeatureFlag flag = new FeatureFlag("a-flag", null, true, "admin-1");
		when(repository.findAll(any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(flag)));

		Page<FeatureFlagResponse> page = featureFlagService.listFlags(PageRequest.of(0, 10));

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().name()).isEqualTo("a-flag");
	}

	@Test
	void deleteFlagRemovesEntity() {
		FeatureFlag flag = new FeatureFlag("to-delete", null, true, "admin-1");
		ReflectionTestUtils.setField(flag, "id", 1L);
		when(repository.findByName("to-delete")).thenReturn(Optional.of(flag));

		featureFlagService.deleteFlag("to-delete", "admin-1", "req-3");

		ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
		verify(repository).delete(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("to-delete");
		verify(auditService).record(any(Long.class), eq("FLAG_DELETED"), eq("admin-1"), any(Map.class), eq("req-3"));
		verify(featureCacheService).evictFlag("to-delete");
	}
}
