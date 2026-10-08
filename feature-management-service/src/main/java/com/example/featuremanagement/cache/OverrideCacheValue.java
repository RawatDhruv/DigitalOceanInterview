package com.example.featuremanagement.cache;

public enum OverrideCacheValue {
	TRUE,
	FALSE,
	INHERIT;

	public static OverrideCacheValue fromEnabled(Boolean enabled) {
		if (enabled == null) {
			return INHERIT;
		}
		return enabled ? TRUE : FALSE;
	}

	public boolean isExplicit() {
		return this == TRUE || this == FALSE;
	}

	public boolean asBoolean() {
		return this == TRUE;
	}
}
