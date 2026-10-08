package com.example.featuremanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureOverrideId implements Serializable {

	@Column(name = "flag_id", nullable = false)
	private Long flagId;

	@Column(name = "user_id", nullable = false, length = 150)
	private String userId;

	public FeatureOverrideId(Long flagId, String userId) {
		this.flagId = flagId;
		this.userId = userId;
	}
}
