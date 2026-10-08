package com.example.featuremanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
		Duration flagTtl,
		Duration overrideTtl,
		Duration inheritTtl
) {
}
