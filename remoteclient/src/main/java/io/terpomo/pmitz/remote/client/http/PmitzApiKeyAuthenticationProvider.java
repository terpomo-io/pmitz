/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.remote.client.http;

import java.util.Collections;
import java.util.Map;

import io.terpomo.pmitz.core.exception.ConfigurationException;

public class PmitzApiKeyAuthenticationProvider implements PmitzHttpAuthProvider {
	private static final String API_KEY_ENV_VARIABLE = "PMITZ_API_KEY";
	private static final String API_KEY_SYSTEM_PROP = "pmitz.api.key";
	private static final String API_KEY_HEADER = "X-Api-Key";

	private final String apiKey;

	public PmitzApiKeyAuthenticationProvider() {
		apiKey = findApiKey();
	}

	@Override
	public Map<String, String> getAuthenticationHeaders() {
		return (apiKey != null) ? Map.of(API_KEY_HEADER, apiKey) : Collections.emptyMap();
	}

	private String findApiKey() {
		String apiKey = System.getenv(API_KEY_ENV_VARIABLE);

		if (apiKey == null || apiKey.isBlank()) {
			apiKey = System.getProperty(API_KEY_SYSTEM_PROP);
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new ConfigurationException(String.format("Cannot find Api-Key in environment variable %s or system property %s. Authentication with Pmitz server will fail.",
					API_KEY_ENV_VARIABLE, API_KEY_SYSTEM_PROP));
		}
		return apiKey;
	}
}
