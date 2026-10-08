package com.example.featuremanagement.controller;

import java.util.UUID;

final class RequestIds {

	private RequestIds() {
	}

	/** Returns a trimmed client key, or {@code null} when absent. */
	static String optional(String... candidates) {
		for (String candidate : candidates) {
			if (candidate != null && !candidate.isBlank()) {
				return candidate.trim();
			}
		}
		return null;
	}

	/** Client key when present; otherwise a generated id for audit correlation. */
	static String resolve(String... candidates) {
		String optional = optional(candidates);
		return optional != null ? optional : UUID.randomUUID().toString();
	}
}
