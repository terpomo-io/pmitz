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

package io.terpomo.pmitz.limits;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.repository.product.ProductRepository;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.impl.UsageLimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.UsageLimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

public final class UsageLimitVerifierBuilder {

	private UsageLimitVerifierBuilder() {
		// private constructor
	}

	public static UsageLimitResolverSpec of(ProductRepository productRepository) {
		return new Builder(productRepository);
	}

	static ProductRepository inMemoryProductRepo() {
		return new InMemoryProductRepository();
	}

	public interface UsageLimitResolverSpec {
		UsageRepositorySpec withCustomUsageLimitResolver(UsageLimitResolver usageLimitResolver);

		UsageRepositorySpec withUserLimitRepository(UserLimitRepository userLimitRepository);

		UsageRepositorySpec withDefaultUsageLimitResolver();
	}

	public interface UsageRepositorySpec {
		LimitVerificationStrategySpec withCustomUsageRepository(UsageRepository usageRepository);

		LimitVerificationStrategySpec withJdbcUsageRepository(DataSource dataSource, String schema, String table);
	}

	public interface LimitVerificationStrategySpec {
		Creator withUserLimitVerificationStrategy(UsageLimitVerificationStrategy stratey);

		UsageLimitVerifier build();
	}

	public interface Creator extends LimitVerificationStrategySpec {
		UsageLimitVerifier build();
	}

	public static final class Builder implements UsageLimitResolverSpec, UsageRepositorySpec,
			LimitVerificationStrategySpec, Creator {

		private final ProductRepository productRepository;
		private UsageLimitResolver usageLimitResolver;
		private UsageRepository usageRepository;

		private UsageLimitVerificationStrategyResolver verificationStrategyResolver;

		private Builder(ProductRepository productRepository) {
			this.productRepository = productRepository;
		}

		@Override
		public UsageRepositorySpec withCustomUsageLimitResolver(UsageLimitResolver usageLimitResolver) {
			this.usageLimitResolver = usageLimitResolver;
			return this;
		}

		@Override
		public UsageRepositorySpec withUserLimitRepository(UserLimitRepository userLimitRepository) {
			usageLimitResolver = new UsageLimitResolverImpl(productRepository, userLimitRepository);
			return this;
		}

		@Override
		public UsageRepositorySpec withDefaultUsageLimitResolver() {
			usageLimitResolver = new UsageLimitResolverImpl(productRepository);
			return this;
		}

		@Override
		public LimitVerificationStrategySpec withCustomUsageRepository(UsageRepository usageRepository) {
			this.usageRepository = usageRepository;
			return this;
		}

		@Override
		public LimitVerificationStrategySpec withJdbcUsageRepository(DataSource dataSource, String schema, String table) {
			usageRepository = new JDBCUsageRepository(dataSource, schema, table);
			return this;
		}

		@Override
		public Creator withUserLimitVerificationStrategy(UsageLimitVerificationStrategy strategy) {
			verificationStrategyResolver = new UsageLimitVerificationStrategyDefaultResolver(strategy);
			return this;
		}

		@Override
		public UsageLimitVerifier build() {
			if (verificationStrategyResolver == null) {
				verificationStrategyResolver = new UsageLimitVerificationStrategyDefaultResolver();
			}
			return new UsageLimitVerifierImpl(usageLimitResolver, verificationStrategyResolver, usageRepository);
		}
	}
}


