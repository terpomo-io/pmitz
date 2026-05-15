/*
 * Copyright 2023-2026 the original author or authors.
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

package io.terpomo.pmitz.mcpserver.config;

import java.util.Locale;
import java.util.Optional;

public record PmitzMcpConfig(Mode mode, String version, String remoteUrl, String productFile,
		String jdbcUrl, String jdbcUsername, String jdbcPassword, String schemaName,
		String usageTableName, String userLimitTableName, String subscriptionTableName,
		String subscriptionPlanTableName) {

	public static PmitzMcpConfig fromEnvironment() {
		return new PmitzMcpConfig(
				Mode.from(get("PMITZ_MCP_MODE").orElse("remote")),
				get("PMITZ_MCP_VERSION").orElse("0.9.0"),
				get("PMITZ_REMOTE_URL").orElse("http://localhost:8080"),
				get("PMITZ_PRODUCT_FILE").orElse(null),
				get("PMITZ_JDBC_URL").orElse(null),
				get("PMITZ_JDBC_USERNAME").orElse(""),
				get("PMITZ_JDBC_PASSWORD").orElse(""),
				get("PMITZ_DB_SCHEMA").orElse("PUBLIC"),
				get("PMITZ_USAGE_TABLE").orElse("usage"),
				get("PMITZ_USER_LIMIT_TABLE").orElse("user_limit"),
				get("PMITZ_SUBSCRIPTION_TABLE").orElse("subscription"),
				get("PMITZ_SUBSCRIPTION_PLAN_TABLE").orElse("subscription_plan"));
	}

	private static Optional<String> get(String name) {
		return Optional.ofNullable(System.getenv(name)).filter(value -> !value.isBlank());
	}

	public enum Mode {

		REMOTE,

		LOCAL;

		static Mode from(String value) {
			return Mode.valueOf(value.trim().toUpperCase(Locale.ROOT));
		}

	}

}
