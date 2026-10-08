package com.example.featuremanagement.dto;

import com.example.featuremanagement.entity.FlagState;
import jakarta.validation.constraints.Size;

public record UpdateFlagRequest(
		@Size(max = 2000)
		String description,

		Boolean globalEnabled,

		FlagState state
) {
}
