package com.example.featuremanagement.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds field-level previous/new maps for audit records.
 */
public final class ChangeMap {

	private final Map<String, Map<String, Object>> entries = new LinkedHashMap<>();

	private ChangeMap() {
	}

	public static ChangeMap create() {
		return new ChangeMap();
	}

	public ChangeMap put(String field, Object previous, Object next) {
		if (Objects.equals(previous, next)) {
			return this;
		}
		Map<String, Object> entry = new LinkedHashMap<>();
		entry.put("previous", previous);
		entry.put("new", next);
		entries.put(field, entry);
		return this;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public Map<String, Map<String, Object>> toMap() {
		return new LinkedHashMap<>(entries);
	}
}
