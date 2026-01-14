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

package io.terpomo.pmitz.subscriptions;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.subscriptions.jdbc.JDBCSubscriptionRepository;

public final class SubscriptionVerifierBuilder {

	private SubscriptionVerifierBuilder() {
		// private constructor
	}

	public static SubscriptionFeatureManagerSpec withSubscriptionRepository(SubscriptionRepository subscriptionRepository) {
		return new Builder(subscriptionRepository);
	}

	public static SubscriptionFeatureManagerSpec withJdbcSubscriptionRepository(DataSource dataSource, String schemaName,
			String subscriptionTableName, String subscriptionPlanTableName) {
		return new Builder(new JDBCSubscriptionRepository(dataSource, schemaName, subscriptionTableName, subscriptionPlanTableName));
	}

	public interface SubscriptionFeatureManagerSpec {
		Creator withSubscriptionFeatureManager(SubscriptionFeatureManager subscriptionFeatureManager);

		Creator withDefaultSubscriptionFeatureManager(ProductRepository productRepository);
	}

	public interface Creator {
		SubscriptionVerifier build();
	}

	public static final class Builder implements SubscriptionFeatureManagerSpec, Creator {

		private final SubscriptionRepository subscriptionRepository;
		private SubscriptionFeatureManager subscriptionFeatureManager;

		private Builder(SubscriptionRepository subscriptionRepository) {
			this.subscriptionRepository = subscriptionRepository;
		}

		@Override
		public Creator withSubscriptionFeatureManager(SubscriptionFeatureManager subscriptionFeatureManager) {
			this.subscriptionFeatureManager = subscriptionFeatureManager;
			return this;
		}

		@Override
		public Creator withDefaultSubscriptionFeatureManager(ProductRepository productRepository) {
			this.subscriptionFeatureManager = new DefaultSubscriptionFeatureManager(productRepository);
			return this;
		}

		@Override
		public SubscriptionVerifier build() {
			return new SubscriptionVerifierImpl(subscriptionRepository, subscriptionFeatureManager);
		}
	}
}
