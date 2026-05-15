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

package io.terpomo.pmitz.mcpserver.backend;

import java.io.FileInputStream;
import java.io.IOException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.limits.LimitVerifier;
import io.terpomo.pmitz.limits.LimitVerifierBuilder;
import io.terpomo.pmitz.mcpserver.config.PmitzMcpConfig;
import io.terpomo.pmitz.remote.client.http.PmitzApiKeyAuthenticationProvider;
import io.terpomo.pmitz.remote.client.http.PmitzHttpClient;
import io.terpomo.pmitz.subscriptions.SubscriptionVerifierBuilder;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

public final class PmitzBackendFactory {

	private PmitzBackendFactory() {
	}

	public static PmitzBackend create(PmitzMcpConfig config) {
		return switch (config.mode()) {
			case REMOTE -> new RemotePmitzBackend(new PmitzHttpClient(config.remoteUrl(),
					new PmitzApiKeyAuthenticationProvider()));
			case LOCAL -> createLocal(config);
		};
	}

	private static PmitzBackend createLocal(PmitzMcpConfig config) {
		if (config.productFile() == null) {
			throw new IllegalArgumentException("PMITZ_PRODUCT_FILE must be set in local mode");
		}
		if (config.jdbcUrl() == null) {
			throw new IllegalArgumentException("PMITZ_JDBC_URL must be set in local mode");
		}

		InMemoryProductRepository productRepository = new InMemoryProductRepository();
		try (var inputStream = new FileInputStream(config.productFile())) {
			productRepository.load(ProductJsonInput.normalize(inputStream));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load PMITZ_PRODUCT_FILE", ex);
		}

		DataSource dataSource = dataSource(config);
		LimitVerifier limitVerifier = LimitVerifierBuilder.of(productRepository)
				.withDefaultLimitRuleResolver()
				.withJdbcUsageRepository(dataSource, config.schemaName(), config.usageTableName())
				.build();

		var subscriptionRepository = new JDBCSubscriptionRepository(dataSource, config.schemaName(),
				config.subscriptionTableName(), config.subscriptionPlanTableName());
		var subscriptionVerifier = SubscriptionVerifierBuilder.withSubscriptionRepository(subscriptionRepository)
				.withDefaultSubscriptionFeatureManager(productRepository)
				.build();

		return new LocalPmitzBackend(productRepository, limitVerifier, subscriptionVerifier,
				subscriptionRepository);
	}

	private static DataSource dataSource(PmitzMcpConfig config) {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(config.jdbcUrl());
		dataSource.setUsername(config.jdbcUsername());
		dataSource.setPassword(config.jdbcPassword());
		return dataSource;
	}

}
