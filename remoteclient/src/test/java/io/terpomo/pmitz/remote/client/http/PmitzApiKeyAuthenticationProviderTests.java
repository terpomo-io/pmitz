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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import io.terpomo.pmitz.core.exception.ConfigurationException;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class PmitzApiKeyAuthenticationProviderTests {

	@SystemStub
	private EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@Test
	void getAuthenticationHeadersShouldReturnHeaderWithEnvVariableValueWhenEnvVariableIsDefined() {

		environmentVariables.set("PMITZ_API_KEY", "my-api-key");

		var authProvider = new PmitzApiKeyAuthenticationProvider();

		Map<String, String> headers = authProvider.getAuthenticationHeaders();

		assertThat(headers).hasSize(1)
				.containsEntry("X-Api-Key", "my-api-key");
	}

	@Test
	void getAuthenticationHeadersShouldReturnHeaderWithSysPropertyValueWhenSysPropertyIsDefined() {

		System.setProperty("pmitz.api.key", "my-api-key");

		try {
			var authProvider = new PmitzApiKeyAuthenticationProvider();

			Map<String, String> headers = authProvider.getAuthenticationHeaders();

			assertThat(headers).hasSize(1)
					.containsEntry("X-Api-Key", "my-api-key");
		}
		finally {
			System.clearProperty("pmitz.api.key");
		}
	}

	@Test
	void getAuthenticationHeadersShouldReturnHeaderWithEnvVarValueWhenEnvVarAndSysPropertyAreDefined() {

		environmentVariables.set("PMITZ_API_KEY", "api-key-from-env-var");
		System.setProperty("pmitz.api.key", "api-key-from-sys-prop");

		try {
			var authProvider = new PmitzApiKeyAuthenticationProvider();

			Map<String, String> headers = authProvider.getAuthenticationHeaders();

			assertThat(headers).hasSize(1)
					.containsEntry("X-Api-Key", "api-key-from-env-var");
		}
		finally {
			System.clearProperty("pmitz.api.key");
		}
	}

	@Test
	void initShouldThrowExceptionWhenEnvVarIsnotDefinedAndSystemPropertyIsNotDefined() {
		assertThatThrownBy(PmitzApiKeyAuthenticationProvider::new)
				.isInstanceOf(ConfigurationException.class)
				.hasMessage("Cannot find Api-Key in environment variable PMITZ_API_KEY or system property pmitz.api.key. " +
						"Authentication with Pmitz server will fail.");
	}
}
