package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.FeatureOverrideResponse;
import com.example.featuremanagement.dto.UpsertOverrideRequest;
import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FeatureOverride;
import com.example.featuremanagement.repository.FeatureOverrideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureOverrideServiceTest {

	@Mock
	private FeatureFlagService featureFlagService;

	@Mock
	private FeatureOverrideRepository overrideRepository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private FeatureOverrideService featureOverrideService;

	@Test
	void upsertCreatesOverride() {
		FeatureFlag flag = stubFlag();
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(flag);
		when(overrideRepository.findByIdFlagIdAndIdUserId(flag.getId(), "user-1")).thenReturn(Optional.empty());
		when(overrideRepository.save(any(FeatureOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FeatureOverrideResponse response = featureOverrideService.upsertOverride(
				"new-checkout",
				"user-1",
				new UpsertOverrideRequest(false),
				"admin-1"
		);

		assertThat(response.enabled()).isFalse();
		assertThat(response.userId()).isEqualTo("user-1");
		verify(auditService).record("OVERRIDE_UPSERTED", "new-checkout", "admin-1");
	}

	@Test
	void removeMarksTombstone() {
		FeatureFlag flag = stubFlag();
		FeatureOverride existing = new FeatureOverride(flag.getId(), "user-1", true, "admin-1");
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(flag);
		when(overrideRepository.findByIdFlagIdAndIdUserId(flag.getId(), "user-1")).thenReturn(Optional.of(existing));
		when(overrideRepository.save(any(FeatureOverride.class))).thenAnswer(invocation -> invocation.getArgument(0));

		featureOverrideService.removeOverride("new-checkout", "user-1", "admin-2");

		assertThat(existing.getEnabled()).isNull();
		assertThat(existing.getUpdatedBy()).isEqualTo("admin-2");
		verify(auditService).record("OVERRIDE_REMOVED", "new-checkout", "admin-2");
	}

	@Test
	void listReturnsActiveOverridesOnly() {
		FeatureFlag flag = stubFlag();
		FeatureOverride override = new FeatureOverride(flag.getId(), "user-1", true, "admin-1");
		when(featureFlagService.getFlagEntity("new-checkout")).thenReturn(flag);
		when(overrideRepository.findActiveByFlagId(eq(flag.getId()), any()))
				.thenReturn(new PageImpl<>(List.of(override)));

		Page<FeatureOverrideResponse> page = featureOverrideService.listOverrides(
				"new-checkout",
				PageRequest.of(0, 10)
		);

		assertThat(page.getContent()).hasSize(1);
		assertThat(page.getContent().getFirst().enabled()).isTrue();
	}

	private FeatureFlag stubFlag() {
		FeatureFlag flag = new FeatureFlag("new-checkout", "Checkout", true, "admin-1");
		// id is generated on persist; set via reflection-free approach by saving mock id through subclassing is hard.
		// Use repository-style: FeatureFlag id is null until persist — override service only needs getId().
		// Assign using a test helper via package — instead mock getId by creating with reflection.
		try {
			var field = FeatureFlag.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(flag, UUID.fromString("11111111-1111-1111-1111-111111111111"));
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		return flag;
	}
}
