package com.example.featuremanagement.repository;

import com.example.featuremanagement.entity.FeatureFlag;
import com.example.featuremanagement.entity.FlagState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeatureFlagRepositoryTest {

	@Autowired
	private FeatureFlagRepository featureFlagRepository;

	@Test
	@Transactional
	void insertAndLookupByName() {
		FeatureFlag saved = featureFlagRepository.saveAndFlush(
				new FeatureFlag("new-checkout", "Checkout redesign", true, "admin-1"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getVersion()).isNotNull();
		assertThat(saved.getState()).isEqualTo(FlagState.DRAFT);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();

		assertThat(featureFlagRepository.findByName("new-checkout"))
				.isPresent()
				.get()
				.satisfies(flag -> {
					assertThat(flag.getId()).isEqualTo(saved.getId());
					assertThat(flag.getDescription()).isEqualTo("Checkout redesign");
					assertThat(flag.isGlobalEnabled()).isTrue();
					assertThat(flag.getCreatedBy()).isEqualTo("admin-1");
				});

		assertThat(featureFlagRepository.existsByName("new-checkout")).isTrue();
		assertThat(featureFlagRepository.existsByName("missing-flag")).isFalse();
	}

	@Test
	@Transactional
	void updateFlagFields() {
		FeatureFlag flag = featureFlagRepository.saveAndFlush(
				new FeatureFlag("dark-mode", "Dark theme", false, "admin-1"));
		Long originalVersion = flag.getVersion();

		flag.updateDescription("Updated dark theme", "admin-2");
		flag.updateGlobalEnabled(true, "admin-2");
		flag.updateState(FlagState.ACTIVE, "admin-2");
		featureFlagRepository.saveAndFlush(flag);

		assertThat(featureFlagRepository.findByName("dark-mode"))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getDescription()).isEqualTo("Updated dark theme");
					assertThat(found.isGlobalEnabled()).isTrue();
					assertThat(found.getState()).isEqualTo(FlagState.ACTIVE);
					assertThat(found.getUpdatedBy()).isEqualTo("admin-2");
					assertThat(found.getVersion()).isGreaterThan(originalVersion);
				});
	}

	@Test
	@Transactional
	void rejectsDuplicateName() {
		featureFlagRepository.saveAndFlush(
				new FeatureFlag("duplicate-flag", "First", true, "admin-1"));

		assertThatThrownBy(() -> featureFlagRepository.saveAndFlush(
				new FeatureFlag("duplicate-flag", "Second", false, "admin-2")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void optimisticLockingRejectsStaleUpdate() {
		FeatureFlag created = featureFlagRepository.saveAndFlush(
				new FeatureFlag("lock-flag", "Locking", true, "admin-1"));

		FeatureFlag first = featureFlagRepository.findByName("lock-flag").orElseThrow();
		FeatureFlag second = featureFlagRepository.findByName("lock-flag").orElseThrow();

		first.updateDescription("First writer", "admin-1");
		featureFlagRepository.saveAndFlush(first);

		second.updateDescription("Second writer", "admin-2");
		assertThatThrownBy(() -> featureFlagRepository.saveAndFlush(second))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}
}
