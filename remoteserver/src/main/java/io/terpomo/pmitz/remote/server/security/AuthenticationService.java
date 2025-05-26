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

package io.terpomo.pmitz.remote.server.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import io.terpomo.pmitz.core.exception.ConfigurationException;

@Component
@EnableConfigurationProperties(ApiKeyProperties.class)
public class AuthenticationService {
	private static final String AUTH_TOKEN_HEADER_NAME = "X-Api-Key";

	private final ApiKeyProperties apiKeyProperties;

	public AuthenticationService(ApiKeyProperties apiKeyProperties) {
		this.apiKeyProperties = apiKeyProperties;
		if (apiKeyProperties.pmitzApiKey() == null || apiKeyProperties.pmitzApiKey().isBlank()) {
			throw new ConfigurationException("Please provide a value for Api Key using env variable PMITZ_API_KEY");
		}
	}

	public Authentication getAuthentication(HttpServletRequest request) {
		String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);
		if (apiKey == null || !apiKey.equals(apiKeyProperties.pmitzApiKey())) {
			throw new BadCredentialsException("Invalid API Key");
		}

		return new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES);
	}
}
