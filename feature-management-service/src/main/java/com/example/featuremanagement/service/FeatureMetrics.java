package com.example.featuremanagement.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FeatureMetrics {

	private final Counter cacheHits;
	private final Counter cacheMisses;
	private final Counter redisFailures;
	private final Counter dbFallbacks;
	private final Counter evaluationErrors;
	private final Timer evaluationTimer;

	public FeatureMetrics(MeterRegistry registry) {
		this.cacheHits = Counter.builder("fms.cache.hits").description("Redis cache hits").register(registry);
		this.cacheMisses = Counter.builder("fms.cache.misses").description("Redis cache misses").register(registry);
		this.redisFailures = Counter.builder("fms.cache.redis_failures")
				.description("Redis operation failures")
				.register(registry);
		this.dbFallbacks = Counter.builder("fms.evaluation.db_fallbacks")
				.description("Evaluations that read PostgreSQL after cache miss/failure")
				.register(registry);
		this.evaluationErrors = Counter.builder("fms.evaluation.errors")
				.description("Failed evaluations")
				.register(registry);
		this.evaluationTimer = Timer.builder("fms.evaluation.latency")
				.description("Evaluation latency")
				.register(registry);
	}

	public void recordCacheHit() {
		cacheHits.increment();
	}

	public void recordCacheMiss() {
		cacheMisses.increment();
	}

	public void recordRedisFailure() {
		redisFailures.increment();
	}

	public void recordDbFallback() {
		dbFallbacks.increment();
	}

	public void recordEvaluationError() {
		evaluationErrors.increment();
	}

	public void recordEvaluationLatency(long nanos) {
		evaluationTimer.record(nanos, TimeUnit.NANOSECONDS);
	}
}
