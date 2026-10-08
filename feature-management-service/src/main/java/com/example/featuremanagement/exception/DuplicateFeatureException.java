package com.example.featuremanagement.exception;

public class DuplicateFeatureException extends RuntimeException {

	private final String flagName;

	public DuplicateFeatureException(String flagName) {
		super("Feature flag already exists: " + flagName);
		this.flagName = flagName;
	}

	public String getFlagName() {
		return flagName;
	}
}
