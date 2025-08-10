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
import io.terpomo.pmitz.limits.impl.LimitRuleResolverImpl;
import io.terpomo.pmitz.limits.impl.LimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.LimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

public final class LimitVerifierBuilder {

	private LimitVerifierBuilder() {
		// private constructor
	}

	public static LimitRuleResolverSpec of(ProductRepository productRepository) {
		return new Builder(productRepository);
	}

	static ProductRepository inMemoryProductRepo() {
		return new InMemoryProductRepository();
	}

	public interface LimitRuleResolverSpec {
		UsageRepositorySpec withCustomLimitRuleResolver(LimitRuleResolver limitRuleResolver);

		UsageRepositorySpec withUserLimitRepository(UserLimitRepository userLimitRepository);

		UsageRepositorySpec withDefaultLimitRuleResolver();
	}

	public interface UsageRepositorySpec {
		LimitVerificationStrategySpec withCustomUsageRepository(UsageRepository usageRepository);

		LimitVerificationStrategySpec withJdbcUsageRepository(DataSource dataSource, String schema, String table);
	}

	public interface LimitVerificationStrategySpec {
		Creator withUserLimitVerificationStrategy(LimitVerificationStrategy stratey);

		LimitVerifier build();
	}

	public interface Creator extends LimitVerificationStrategySpec {
		LimitVerifier build();
	}

	public static final class Builder implements LimitRuleResolverSpec, UsageRepositorySpec,
			LimitVerificationStrategySpec, Creator {

		private final ProductRepository productRepository;
		private LimitRuleResolver limitRuleResolver;
		private UsageRepository usageRepository;

		private LimitVerificationStrategyResolver verificationStrategyResolver;

		private Builder(ProductRepository productRepository) {
			this.productRepository = productRepository;
		}

		public UsageRepositorySpec withCustomLimitRuleResolver(LimitRuleResolver limitRuleResolver) {
			this.limitRuleResolver = limitRuleResolver;
			return this;
		}

		@Override
		public UsageRepositorySpec withUserLimitRepository(UserLimitRepository userLimitRepository) {
			limitRuleResolver = new LimitRuleResolverImpl(productRepository, userLimitRepository);
			return this;
		}

		@Override
		public UsageRepositorySpec withDefaultLimitRuleResolver() {
			limitRuleResolver = new LimitRuleResolverImpl(productRepository);
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
		public Creator withUserLimitVerificationStrategy(LimitVerificationStrategy strategy) {
			verificationStrategyResolver = new LimitVerificationStrategyDefaultResolver(strategy);
			return this;
		}

		@Override
		public LimitVerifier build() {
			if (verificationStrategyResolver == null) {
				verificationStrategyResolver = new LimitVerificationStrategyDefaultResolver();
			}
			return new LimitVerifierImpl(limitRuleResolver, verificationStrategyResolver, usageRepository);
		}
	}
}


