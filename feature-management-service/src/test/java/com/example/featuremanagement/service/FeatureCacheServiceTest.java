package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.cache.CachedOverride;
import com.example.featuremanagement.cache.OverrideCacheValue;
import com.example.featuremanagement.config.CacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureCacheServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	private FeatureCacheService featureCacheService;

	@BeforeEach
	void setUp() {
		CacheProperties properties = new CacheProperties(
				Duration.ofSeconds(30),
				Duration.ofSeconds(30),
				Duration.ofSeconds(15)
		);
		featureCacheService = new FeatureCacheService(
				redisTemplate,
				properties,
				new FeatureMetrics(new SimpleMeterRegistry())
		);
	}

	@Test
	void getFlagReturnsCachedMetadata() {
		Long flagId = 42L;
		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.entries("ff:flag:new-checkout")).thenReturn(Map.of(
				"flagId", flagId.toString(),
				"name", "new-checkout",
				"globalEnabled", "true",
				"updatedAt", "2026-01-01T00:00:00Z"
		));

		Optional<CachedFlag> result = featureCacheService.getFlag("new-checkout");

		assertThat(result).isPresent();
		assertThat(result.get().flagId()).isEqualTo(flagId);
		assertThat(result.get().globalEnabled()).isTrue();
	}

	@Test
	void getFlagReturnsEmptyWhenRedisUnavailable() {
		when(redisTemplate.opsForHash()).thenThrow(new RuntimeException("connection refused"));

		assertThat(featureCacheService.getFlag("new-checkout")).isEmpty();
	}

	@Test
	void putFlagUsesTimestampAwareScript() {
		Long flagId = 42L;
		CachedFlag flag = new CachedFlag(
				flagId,
				"new-checkout",
				true,
				Instant.parse("2026-01-01T00:00:00Z")
		);

		featureCacheService.putFlag(flag);

		verify(redisTemplate).execute(
				any(RedisScript.class),
				eq(List.of("ff:flag:new-checkout")),
				eq("2026-01-01T00:00:00Z"),
				eq("30000"),
				eq("flagId"),
				eq(flagId.toString()),
				eq("name"),
				eq("new-checkout"),
				eq("globalEnabled"),
				eq("true"),
				eq("updatedAt"),
				eq("2026-01-01T00:00:00Z")
		);
	}

	@Test
	void putOverrideUsesInheritTtl() {
		Long flagId = 42L;
		featureCacheService.putOverride(
				flagId,
				"user-1",
				new CachedOverride(OverrideCacheValue.INHERIT, Instant.parse("2026-01-01T00:00:00Z"))
		);

		verify(redisTemplate).execute(
				any(RedisScript.class),
				eq(List.of("ff:override:" + flagId + ":user-1")),
				eq("2026-01-01T00:00:00Z"),
				eq("15000"),
				eq("value"),
				eq("INHERIT"),
				eq("updatedAt"),
				eq("2026-01-01T00:00:00Z")
		);
	}

	@Test
	void putFlagSwallowsRedisFailures() {
		doThrow(new RuntimeException("redis down"))
				.when(redisTemplate)
				.execute(
						any(RedisScript.class),
						anyList(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString(),
						anyString()
				);

		featureCacheService.putFlag(new CachedFlag(
				1L,
				"new-checkout",
				false,
				Instant.parse("2026-01-01T00:00:00Z")
		));
	}

	@Test
	void evictFlagDeletesKey() {
		featureCacheService.evictFlag("new-checkout");
		verify(redisTemplate).delete("ff:flag:new-checkout");
	}

	@Test
	void getOverrideReturnsEmptyOnCorruptPayload() {
		when(redisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.entries(anyString())).thenReturn(Map.of("value", "TRUE"));

		assertThat(featureCacheService.getOverride(1L, "user-1")).isEmpty();
	}
}
