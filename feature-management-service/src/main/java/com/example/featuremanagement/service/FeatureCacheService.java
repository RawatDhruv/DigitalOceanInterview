package com.example.featuremanagement.service;

import com.example.featuremanagement.cache.CachedFlag;
import com.example.featuremanagement.cache.CachedOverride;
import com.example.featuremanagement.cache.OverrideCacheValue;
import com.example.featuremanagement.config.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Read-through Redis cache for evaluation. All Redis failures are swallowed so callers
 * can fall back to PostgreSQL without failing the request.
 *
 * <p>Keys: {@code ff:flag:{name}}, {@code ff:override:{flagId}:{userId}} (Redis hashes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureCacheService {

	private static final String FLAG_KEY_PREFIX = "ff:flag:";
	private static final String OVERRIDE_KEY_PREFIX = "ff:override:";

	private static final String FIELD_FLAG_ID = "flagId";
	private static final String FIELD_NAME = "name";
	private static final String FIELD_GLOBAL_ENABLED = "globalEnabled";
	private static final String FIELD_VALUE = "value";
	private static final String FIELD_UPDATED_AT = "updatedAt";

	/**
	 * Apply hash fields only when incoming updatedAt is strictly newer than any cached value.
	 * KEYS[1]=key, ARGV[1]=updatedAt, ARGV[2]=ttlMs, ARGV[3..]=field/value pairs.
	 */
	private static final DefaultRedisScript<Long> PUT_HASH_IF_NEWER = new DefaultRedisScript<>(
			"""
					local current = redis.call('HGET', KEYS[1], 'updatedAt')
					if current and current ~= false and ARGV[1] <= current then
					  return 0
					end
					for i = 3, #ARGV, 2 do
					  redis.call('HSET', KEYS[1], ARGV[i], ARGV[i + 1])
					end
					redis.call('PEXPIRE', KEYS[1], ARGV[2])
					return 1
					""",
			Long.class
	);

	private final StringRedisTemplate redisTemplate;
	private final CacheProperties cacheProperties;

	public Optional<CachedFlag> getFlag(String name) {
		try {
			Map<Object, Object> entries = redisTemplate.opsForHash().entries(flagKey(name));
			if (entries == null || entries.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new CachedFlag(
					Long.parseLong(stringField(entries, FIELD_FLAG_ID)),
					stringField(entries, FIELD_NAME),
					Boolean.parseBoolean(stringField(entries, FIELD_GLOBAL_ENABLED)),
					Instant.parse(stringField(entries, FIELD_UPDATED_AT))
			));
		}
		catch (Exception ex) {
			log.warn("Redis flag read failed for name={}: {}", name, ex.toString());
			return Optional.empty();
		}
	}

	public void putFlag(CachedFlag flag) {
		writeHashIfNewer(
				flagKey(flag.name()),
				flag.updatedAt(),
				cacheProperties.flagTtl(),
				FIELD_FLAG_ID, flag.flagId().toString(),
				FIELD_NAME, flag.name(),
				FIELD_GLOBAL_ENABLED, Boolean.toString(flag.globalEnabled()),
				FIELD_UPDATED_AT, flag.updatedAt().toString()
		);
	}

	public void evictFlag(String name) {
		delete(flagKey(name));
	}

	public Optional<CachedOverride> getOverride(Long flagId, String userId) {
		try {
			Map<Object, Object> entries = redisTemplate.opsForHash().entries(overrideKey(flagId, userId));
			if (entries == null || entries.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new CachedOverride(
					OverrideCacheValue.valueOf(stringField(entries, FIELD_VALUE)),
					Instant.parse(stringField(entries, FIELD_UPDATED_AT))
			));
		}
		catch (Exception ex) {
			log.warn("Redis override read failed for flagId={} userId={}: {}", flagId, userId, ex.toString());
			return Optional.empty();
		}
	}

	public void putOverride(Long flagId, String userId, CachedOverride override) {
		Duration ttl = override.value() == OverrideCacheValue.INHERIT
				? cacheProperties.inheritTtl()
				: cacheProperties.overrideTtl();
		writeHashIfNewer(
				overrideKey(flagId, userId),
				override.updatedAt(),
				ttl,
				FIELD_VALUE, override.value().name(),
				FIELD_UPDATED_AT, override.updatedAt().toString()
		);
	}

	public void putInherit(Long flagId, String userId, Instant updatedAt) {
		putOverride(flagId, userId, CachedOverride.inherit(updatedAt));
	}

	public void evictOverride(Long flagId, String userId) {
		delete(overrideKey(flagId, userId));
	}

	static String flagKey(String name) {
		return FLAG_KEY_PREFIX + name;
	}

	static String overrideKey(Long flagId, String userId) {
		return OVERRIDE_KEY_PREFIX + flagId + ":" + userId;
	}

	private void writeHashIfNewer(String key, Instant updatedAt, Duration ttl, String... fieldValues) {
		try {
			Object[] args = new Object[2 + fieldValues.length];
			args[0] = updatedAt.toString();
			args[1] = String.valueOf(Math.max(ttl.toMillis(), 1L));
			System.arraycopy(fieldValues, 0, args, 2, fieldValues.length);
			redisTemplate.execute(PUT_HASH_IF_NEWER, Collections.singletonList(key), args);
		}
		catch (Exception ex) {
			log.warn("Redis write failed for key={}: {}", key, ex.toString());
		}
	}

	private void delete(String key) {
		try {
			redisTemplate.delete(key);
		}
		catch (Exception ex) {
			log.warn("Redis delete failed for key={}: {}", key, ex.toString());
		}
	}

	private static String stringField(Map<Object, Object> entries, String field) {
		Object value = entries.get(field);
		if (value == null) {
			throw new IllegalStateException("Missing cache field: " + field);
		}
		return value.toString();
	}
}
