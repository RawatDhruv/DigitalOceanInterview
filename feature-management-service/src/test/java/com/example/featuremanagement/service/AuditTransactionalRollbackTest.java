package com.example.featuremanagement.service;

import com.example.featuremanagement.dto.CreateFlagRequest;
import com.example.featuremanagement.dto.UpdateFlagRequest;
import com.example.featuremanagement.entity.FlagState;
import com.example.featuremanagement.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class AuditTransactionalRollbackTest {

	@DynamicPropertySource
	static void isolateDatabase(DynamicPropertyRegistry registry) {
		registry.add(
				"spring.datasource.url",
				() -> "jdbc:h2:mem:audit_rollback;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
		);
	}

	@Autowired
	private FeatureFlagService featureFlagService;

	@Autowired
	private FeatureFlagRepository featureFlagRepository;

	@MockitoSpyBean
	private AuditService auditService;

	@Test
	void updateRollsBackWhenAuditPersistenceFails() {
		featureFlagService.createFlag(
				new CreateFlagRequest("audit-rollback", "Original", false, FlagState.ACTIVE),
				"admin-1",
				"req-create"
		);

		doThrow(new RuntimeException("audit write failed"))
				.when(auditService)
				.record(any(), eq("FLAG_UPDATED"), anyString(), anyMap(), anyString());

		assertThatThrownBy(() -> featureFlagService.updateFlag(
				"audit-rollback",
				new UpdateFlagRequest("Should not persist", true, null),
				"admin-2",
				"req-update"
		)).hasMessageContaining("audit write failed");

		var flag = featureFlagRepository.findByName("audit-rollback").orElseThrow();
		assertThat(flag.getDescription()).isEqualTo("Original");
		assertThat(flag.isGlobalEnabled()).isFalse();
		assertThat(flag.getUpdatedBy()).isEqualTo("admin-1");
	}
}
