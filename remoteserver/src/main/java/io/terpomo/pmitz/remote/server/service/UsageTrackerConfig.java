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

package io.terpomo.pmitz.remote.server.service;

import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.terpomo.pmitz.all.usage.tracker.FeatureUsageTracker;
import io.terpomo.pmitz.all.usage.tracker.impl.FeatureUsageTrackerImpl;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.limits.UsageLimitVerifierBuilder;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

@Configuration
public class UsageTrackerConfig {

	private static final String DB_SCHEMA_NAME = "dbo";
	private static final String DB_USER_USAGE_TABLE_NAME = "usage";
	private static final String DB_USER_LIMIT_TABLE_NAME = "user_limit";

	@Bean
	ProductRepository productRepository() {
		InMemoryProductRepository productRepo = new InMemoryProductRepository();
		return productRepo;
	}

	@Bean
	UsageLimitVerifier usageLimitVerifier(ProductRepository productRepo, DataSource dataSource) {
		var userLimitRepository = UserLimitRepository.builder().jdbcRepository(dataSource, DB_SCHEMA_NAME, DB_USER_LIMIT_TABLE_NAME);
		return UsageLimitVerifierBuilder.of(productRepo)
				.withUserLimitRepository(userLimitRepository)
				.withJdbcUsageRepository(dataSource, DB_SCHEMA_NAME, DB_USER_USAGE_TABLE_NAME)
				.build();
	}

	@Bean
	FeatureUsageTracker featureUsageTracker(UsageLimitVerifier usageLimitVerifier) {
		var subscriptionVerifier = new SubscriptionVerifier() {

			@Override
			public boolean isFeatureAllowed(Feature feature, UserGrouping userGrouping) {
				return true;
			}

			@Override
			public Optional<Plan> findPlan(Feature feature, UserGrouping userGrouping) {
				return Optional.empty();
			}
		};
		return new FeatureUsageTrackerImpl(usageLimitVerifier, subscriptionVerifier);
	}
}
