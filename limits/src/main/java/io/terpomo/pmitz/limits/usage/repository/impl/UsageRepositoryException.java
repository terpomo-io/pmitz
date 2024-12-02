package io.terpomo.pmitz.limits.usage.repository.impl;

public class UsageRepositoryException extends RuntimeException {
	public UsageRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

	public UsageRepositoryException(String message) {
	}
}
