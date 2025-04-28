package io.terpomo.pmitz.remote.client.http;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PmitzApiKeyAuthenticationProvider implements PmitzHttpAuthProvider {
	private static final String API_KEY_ENV_VARIABLE = "PMITZ_API_KEY";
	private static final String API_KEY_SYSTEM_PROP = "pmitz.api.key";
	private static final String API_KEY_HEADER = "X-Api-Key";

	private static final Logger LOGGER = LoggerFactory.getLogger(PmitzApiKeyAuthenticationProvider.class);
	private final String apiKey;

	public PmitzApiKeyAuthenticationProvider() {
		apiKey = findApiKey();
	}

	@Override
	public Map<String, String> getAuthenticationHeaders() {
		return Map.of(API_KEY_HEADER, apiKey);
	}

	private String findApiKey() {
		String apiKey = System.getenv(API_KEY_ENV_VARIABLE);

		if (apiKey == null || apiKey.isBlank()) {
			apiKey = System.getProperty(API_KEY_SYSTEM_PROP);
		}
		if (apiKey == null || apiKey.isBlank()) {
			LOGGER.error("Cannot find Api-Key in environment variable {} or system propertu {}. Authentication with Pmitz server will fail.",
					API_KEY_ENV_VARIABLE, API_KEY_SYSTEM_PROP);
		}
		return apiKey;
	}
}
