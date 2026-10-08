package com.example.featuremanagement.dto;

import jakarta.validation.constraints.NotNull;

public record UpsertOverrideRequest(
		@NotNull
		Boolean enabled
) {
}
