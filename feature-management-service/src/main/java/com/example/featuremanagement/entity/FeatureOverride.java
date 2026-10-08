package com.example.featuremanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "feature_overrides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureOverride {

	@EmbeddedId
	private FeatureOverrideId id;

	/**
	 * Explicit override value. {@code null} is an inheritance tombstone (removed override).
	 */
	@Column
	private Boolean enabled;

	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public FeatureOverride(UUID flagId, String userId, Boolean enabled, String actor) {
		this.id = new FeatureOverrideId(flagId, Objects.requireNonNull(userId));
		this.enabled = enabled;
		this.updatedBy = Objects.requireNonNull(actor);
	}

	@PrePersist
	@PreUpdate
	protected void touch() {
		updatedAt = Instant.now();
	}

	public UUID getFlagId() {
		return id.getFlagId();
	}

	public String getUserId() {
		return id.getUserId();
	}

	public void upsertEnabled(boolean enabled, String actor) {
		this.enabled = enabled;
		this.updatedBy = Objects.requireNonNull(actor);
	}

	public void markRemoved(String actor) {
		this.enabled = null;
		this.updatedBy = Objects.requireNonNull(actor);
	}

	public boolean isActiveOverride() {
		return enabled != null;
	}
}
