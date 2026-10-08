package com.example.featuremanagement.service;

import com.example.featuremanagement.config.ResilienceProperties;
import com.example.featuremanagement.exception.EvaluationUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbFallbackGuardTest {

	@Test
	void opensCircuitAfterConsecutiveDatabaseFailures() {
		DbFallbackGuard guard = new DbFallbackGuard(new ResilienceProperties(8, 2, Duration.ofSeconds(30)));

		assertThatThrownBy(() -> guard.execute(() -> {
			throw new DataAccessResourceFailureException("down");
		})).isInstanceOf(EvaluationUnavailableException.class);

		assertThatThrownBy(() -> guard.execute(() -> {
			throw new DataAccessResourceFailureException("down");
		})).isInstanceOf(EvaluationUnavailableException.class);

		assertThat(guard.isOpen()).isTrue();
		assertThatThrownBy(() -> guard.execute(() -> "ok"))
				.isInstanceOf(EvaluationUnavailableException.class)
				.hasMessageContaining("circuit is open");
	}

	@Test
	void rejectsWhenConcurrentCapacityExceeded() throws Exception {
		DbFallbackGuard guard = new DbFallbackGuard(new ResilienceProperties(1, 5, Duration.ofSeconds(5)));
		java.util.concurrent.CountDownLatch inCall = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

		Thread holder = new Thread(() -> guard.execute(() -> {
			inCall.countDown();
			try {
				release.await();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			return "held";
		}));
		holder.start();
		assertThat(inCall.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> guard.execute(() -> "other"))
				.isInstanceOf(EvaluationUnavailableException.class)
				.hasMessageContaining("capacity exceeded");

		release.countDown();
		holder.join(2000);
	}
}
