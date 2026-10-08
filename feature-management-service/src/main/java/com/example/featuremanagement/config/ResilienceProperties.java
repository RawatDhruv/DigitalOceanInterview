package com.example.featuremanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.resilience")
public record ResilienceProperties(
		int dbFallbackMaxConcurrent,
		int dbFallbackFailureThreshold,
		Duration dbFallbackOpenDuration
) {
}
