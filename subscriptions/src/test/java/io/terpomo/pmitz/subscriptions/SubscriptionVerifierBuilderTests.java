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

package io.terpomo.pmitz.subscriptions;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionRepository;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SubscriptionVerifierBuilderTests {

	@Mock
	DataSource dataSource;

	@Mock
	SubscriptionRepository subscriptionRepository;

	@Mock
	SubscriptionFeatureManager subscriptionFeatureManager;

	@Mock
	ProductRepository productRepository;

	@Test
	void builderShouldCreateSubscriptionVerifierWithCustomRepositoryAndFeatureManager() {
		var subscriptionVerifier = SubscriptionVerifierBuilder
				.withSubscriptionRepository(subscriptionRepository)
				.withSubscriptionFeatureManager(subscriptionFeatureManager)
				.build();

		assertThat(subscriptionVerifier).isNotNull().isInstanceOf(SubscriptionVerifierImpl.class);
		assertThat(subscriptionVerifier).extracting("subscriptionRepository").isSameAs(subscriptionRepository);
		assertThat(subscriptionVerifier).extracting("subscriptionFeatureManager").isSameAs(subscriptionFeatureManager);
	}

	@Test
	void builderShouldCreateSubscriptionVerifierWithCustomRepositoryAndDefaultFeatureManager() {
		var subscriptionVerifier = SubscriptionVerifierBuilder
				.withSubscriptionRepository(subscriptionRepository)
				.withDefaultSubscriptionFeatureManager(productRepository)
				.build();

		assertThat(subscriptionVerifier).isNotNull().isInstanceOf(SubscriptionVerifierImpl.class);
		assertThat(subscriptionVerifier).extracting("subscriptionRepository").isSameAs(subscriptionRepository);
		assertThat(subscriptionVerifier).extracting("subscriptionFeatureManager")
				.isInstanceOf(DefaultSubscriptionFeatureManager.class)
				.extracting("productRepository").isSameAs(productRepository);
	}

	@Test
	void builderShouldCreateSubscriptionVerifierWithJdbcRepositoryAndCustomFeatureManager() {
		var subscriptionVerifier = SubscriptionVerifierBuilder
				.withJdbcSubscriptionRepository(dataSource, "schema", "subscriptions", "subscription_plans")
				.withSubscriptionFeatureManager(subscriptionFeatureManager)
				.build();

		assertThat(subscriptionVerifier).isNotNull().isInstanceOf(SubscriptionVerifierImpl.class);
		assertThat(subscriptionVerifier).extracting("subscriptionRepository")
				.isInstanceOf(JDBCSubscriptionRepository.class)
				.extracting("dataSource").isSameAs(dataSource);
		assertThat(subscriptionVerifier).extracting("subscriptionRepository")
				.extracting("schemaName").isEqualTo("schema");
		assertThat(subscriptionVerifier).extracting("subscriptionRepository")
				.extracting("subscriptionTableName").isEqualTo("subscriptions");
		assertThat(subscriptionVerifier).extracting("subscriptionRepository")
				.extracting("subscriptionPlanTableName").isEqualTo("subscription_plans");
		assertThat(subscriptionVerifier).extracting("subscriptionFeatureManager").isSameAs(subscriptionFeatureManager);
	}

	@Test
	void builderShouldCreateSubscriptionVerifierWithJdbcRepositoryAndDefaultFeatureManager() {
		var subscriptionVerifier = SubscriptionVerifierBuilder
				.withJdbcSubscriptionRepository(dataSource, "schema", "subscriptions", "subscription_plans")
				.withDefaultSubscriptionFeatureManager(productRepository)
				.build();

		assertThat(subscriptionVerifier).isNotNull().isInstanceOf(SubscriptionVerifierImpl.class);
		assertThat(subscriptionVerifier).extracting("subscriptionRepository")
				.isInstanceOf(JDBCSubscriptionRepository.class)
				.extracting("dataSource").isSameAs(dataSource);
		assertThat(subscriptionVerifier).extracting("subscriptionFeatureManager")
				.isInstanceOf(DefaultSubscriptionFeatureManager.class)
				.extracting("productRepository").isSameAs(productRepository);
	}
}
