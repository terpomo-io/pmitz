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

package io.terpomo.pmitz.remote.server.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.all.usage.tracker.impl.FeatureUsageTrackerImpl;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.LimitVerifier;
import io.terpomo.pmitz.limits.LimitVerifierBuilder;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import io.terpomo.pmitz.subscriptions.DefaultSubscriptionFeatureManager;
import io.terpomo.pmitz.subscriptions.SubscriptionFeatureManager;
import io.terpomo.pmitz.subscriptions.SubscriptionVerifierImpl;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

@AutoConfiguration
@EnableConfigurationProperties(RelationalDBConfigProperties.class)
public class UsageTrackerAutoConfiguration {

	@Bean
	ProductRepository productRepository() {
		return new InMemoryProductRepository();
	}

	@Bean
	LimitVerifier limitVerifier(ProductRepository productRepo, DataSource dataSource,
			RelationalDBConfigProperties dbConfig) {
		var userLimitRepository = UserLimitRepository.builder().jdbcRepository(dataSource, dbConfig.schemaName(), dbConfig.userLimitTableName());
		return LimitVerifierBuilder.of(productRepo)
				.withUserLimitRepository(userLimitRepository)
				.withJdbcUsageRepository(dataSource, dbConfig.schemaName(), dbConfig.userUsageTableName())
				.build();
	}

	@Bean
	SubscriptionRepository subscriptionRepository(DataSource dataSource,
			RelationalDBConfigProperties dbConfig) {
		return new JDBCSubscriptionRepository(dataSource, dbConfig.schemaName(), dbConfig.subscriptionTableName(), dbConfig.subscriptionPlanTableName());
	}

	@Bean
	SubscriptionFeatureManager subscriptionFeatureManager(ProductRepository productRepo) {
		return new DefaultSubscriptionFeatureManager(productRepo);
	}

	@Bean
	SubscriptionVerifier subscriptionVerifier(SubscriptionRepository subscriptionRepository,
			SubscriptionFeatureManager subscriptionFeatureManager) {
		return new SubscriptionVerifierImpl(subscriptionRepository, subscriptionFeatureManager);
	}

	@Bean
	FeatureUsageTracker featureUsageTracker(LimitVerifier limitVerifier, SubscriptionVerifier subscriptionVerifier) {
		return new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
	}
}
