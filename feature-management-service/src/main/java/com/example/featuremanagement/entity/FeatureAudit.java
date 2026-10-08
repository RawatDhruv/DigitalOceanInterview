package com.example.featuremanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "feature_audits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "flag_id", nullable = false, updatable = false)
	private UUID flagId;

	@Column(nullable = false, updatable = false, length = 50)
	private String action;

	@Column(name = "actor_id", nullable = false, updatable = false, length = 100)
	private String actorId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "change_map", nullable = false, updatable = false)
	private Map<String, Map<String, Object>> changeMap = new LinkedHashMap<>();

	@Column(name = "request_id", updatable = false, length = 100)
	private String requestId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public FeatureAudit(
			UUID flagId,
			String action,
			String actorId,
			Map<String, Map<String, Object>> changeMap,
			String requestId) {
		this.flagId = Objects.requireNonNull(flagId);
		this.action = Objects.requireNonNull(action);
		this.actorId = Objects.requireNonNull(actorId);
		this.changeMap = changeMap == null ? new LinkedHashMap<>() : new LinkedHashMap<>(changeMap);
		this.requestId = requestId;
	}

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}
}
