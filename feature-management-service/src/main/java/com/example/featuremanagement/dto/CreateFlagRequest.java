package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.FlagState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFlagRequest(
		@NotBlank
		@Size(min = 1, max = 150)
		@Pattern(
				regexp = "^[a-z0-9]+(?:[_-][a-z0-9]+)*$",
				message = "flag name must be lowercase alphanumeric with optional hyphens/underscores, no spaces"
		)
		String name,

		@Size(max = 2000)
		String description,

		@NotNull
		Boolean globalEnabled,

		FlagState state
) {
}
