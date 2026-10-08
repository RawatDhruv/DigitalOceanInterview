package com.example.featuremanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Placeholder for Phase 6 audit persistence.
 * Mutation services will call this once audit storage is implemented.
 */
@Service
@Slf4j
public class AuditService {

	public void record(String action, String flagName, String actorId) {
		log.debug("Audit event pending persistence: action={}, flag={}, actor={}", action, flagName, actorId);
	}
}
