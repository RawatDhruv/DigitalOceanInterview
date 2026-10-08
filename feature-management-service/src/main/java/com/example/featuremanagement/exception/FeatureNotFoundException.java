package com.example.featuremanagement.exception;

public class FeatureNotFoundException extends RuntimeException {

	private final String flagName;

	public FeatureNotFoundException(String flagName) {
		super("Feature flag not found: " + flagName);
		this.flagName = flagName;
	}

	public String getFlagName() {
		return flagName;
	}
}
