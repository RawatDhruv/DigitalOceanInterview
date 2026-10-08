package com.example.featuremanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
		name = "feature_flags",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_feature_flags_name",
				columnNames = "name"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureFlag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 150, updatable = false)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "global_enabled", nullable = false)
	private boolean globalEnabled;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FlagState state = FlagState.DRAFT;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_by", nullable = false, updatable = false, length = 100)
	private String createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_by", nullable = false, length = 100)
	private String updatedBy;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public FeatureFlag(
			String name,
			String description,
			boolean globalEnabled,
			String actor) {
		this.name = Objects.requireNonNull(name);
		this.description = description;
		this.globalEnabled = globalEnabled;
		this.createdBy = Objects.requireNonNull(actor);
		this.updatedBy = actor;
	}

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

	public void updateDescription(String description, String actor) {
		if (!Objects.equals(this.description, description)) {
			this.description = description;
			this.updatedBy = Objects.requireNonNull(actor);
		}
	}

	public void updateGlobalEnabled(boolean enabled, String actor) {
		if (this.globalEnabled != enabled) {
			this.globalEnabled = enabled;
			this.updatedBy = Objects.requireNonNull(actor);
		}
	}

	public void updateState(FlagState state, String actor) {
		Objects.requireNonNull(state);

		if (this.state != state) {
			this.state = state;
			this.updatedBy = Objects.requireNonNull(actor);
		}
	}
}
