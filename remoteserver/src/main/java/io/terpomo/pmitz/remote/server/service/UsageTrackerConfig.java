/*
 * Copyright 2023-2025 the original author or authors.
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

package io.terpomo.pmitz.remote.server.service;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

@Configuration
public class UsageTrackerConfig {

	private static final String DB_SCHEMA_NAME = "dbo";
	private static final String DB_USER_USAGE_TABLE_NAME = "usage";
	private static final String DB_USER_LIMIT_TABLE_NAME = "user_limit";
	private static final String DB_SUBSCRIPTION_TABLE_NAME = "subscription";
	private static final String DB_SUBSCRIPTION_PLAN_TABLE_NAME = "subscription_plan";

	@Bean
	ProductRepository productRepository() {
		InMemoryProductRepository productRepo = new InMemoryProductRepository();
		return productRepo;
	}

	@Bean
	LimitVerifier limitVerifier(ProductRepository productRepo, DataSource dataSource) {
		var userLimitRepository = UserLimitRepository.builder().jdbcRepository(dataSource, DB_SCHEMA_NAME, DB_USER_LIMIT_TABLE_NAME);
		return LimitVerifierBuilder.of(productRepo)
				.withUserLimitRepository(userLimitRepository)
				.withJdbcUsageRepository(dataSource, DB_SCHEMA_NAME, DB_USER_USAGE_TABLE_NAME)
				.build();
	}

	@Bean
	SubscriptionRepository subscriptionRepository(DataSource dataSource) {
		return new JDBCSubscriptionRepository(dataSource, DB_SCHEMA_NAME, DB_SUBSCRIPTION_TABLE_NAME, DB_SUBSCRIPTION_PLAN_TABLE_NAME);
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
