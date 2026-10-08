package com.example.featuremanagement.controller;

import java.util.UUID;

final class RequestIds {

	private RequestIds() {
	}

	static String resolve(String requestIdHeader) {
		if (requestIdHeader == null || requestIdHeader.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return requestIdHeader.trim();
	}
}
