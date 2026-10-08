package com.example.featuremanagement.service;

import com.example.featuremanagement.config.ResilienceProperties;
import com.example.featuremanagement.exception.EvaluationUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Limits concurrent PostgreSQL fallback traffic during Redis outages and opens a
 * short circuit when the database itself is failing, so evaluation returns 503
 * instead of stampeding the DB.
 */
@Component
@Slf4j
public class DbFallbackGuard {

	private final ResilienceProperties properties;
	private final Semaphore permits;
	private final AtomicInteger consecutiveFailures = new AtomicInteger();
	private volatile long openUntilEpochMs;

	public DbFallbackGuard(ResilienceProperties properties) {
		this.properties = properties;
		this.permits = new Semaphore(Math.max(properties.dbFallbackMaxConcurrent(), 1), true);
	}

	public <T> T execute(Supplier<T> databaseCall) {
		if (isOpen()) {
			log.debug("DB fallback rejected; circuit open untilEpochMs={}", openUntilEpochMs);
			throw new EvaluationUnavailableException(
					"Evaluation unavailable: database fallback circuit is open");
		}
		if (!permits.tryAcquire()) {
			log.debug(
					"DB fallback rejected; concurrent capacity exceeded max={}",
					properties.dbFallbackMaxConcurrent()
			);
			throw new EvaluationUnavailableException(
					"Evaluation unavailable: database fallback capacity exceeded");
		}
		try {
			log.debug("DB fallback acquired; availablePermits={}", permits.availablePermits());
			T result = databaseCall.get();
			consecutiveFailures.set(0);
			return result;
		}
		catch (DataAccessException ex) {
			onFailure();
			log.debug("DB fallback data-access failure consecutiveFailures will reopen circuit if threshold met");
			throw new EvaluationUnavailableException("Evaluation unavailable: database error", ex);
		}
		catch (RuntimeException ex) {
			if (ex instanceof EvaluationUnavailableException) {
				throw ex;
			}
			// FeatureNotFoundException and validation should propagate unchanged
			throw ex;
		}
		finally {
			permits.release();
		}
	}

	boolean isOpen() {
		return System.currentTimeMillis() < openUntilEpochMs;
	}

	private void onFailure() {
		int failures = consecutiveFailures.incrementAndGet();
		if (failures >= properties.dbFallbackFailureThreshold()) {
			openUntilEpochMs = System.currentTimeMillis() + properties.dbFallbackOpenDuration().toMillis();
			consecutiveFailures.set(0);
			log.warn("Opened DB fallback circuit for {}", properties.dbFallbackOpenDuration());
		}
	}
}
