package com.example.featuremanagement.service;

import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.exception.FeatureNotFoundException;
import com.example.featuremanagement.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

	@Mock
	private FeatureFlagRepository repository;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private FeatureFlagService featureFlagService;

	@Test
	void getFlagReturnsEntity() {
		FeatureFlag flag = new FeatureFlag("new-checkout", "Checkout", true, "admin-1");
		when(repository.findByName("new-checkout")).thenReturn(Optional.of(flag));

		FeatureFlag result = featureFlagService.getFlag("new-checkout");

		assertThat(result.getName()).isEqualTo("new-checkout");
		verifyNoInteractions(auditService);
	}

	@Test
	void getFlagThrowsWhenMissing() {
		when(repository.findByName("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> featureFlagService.getFlag("missing"))
				.isInstanceOf(FeatureNotFoundException.class)
				.hasMessageContaining("missing");
	}
}
