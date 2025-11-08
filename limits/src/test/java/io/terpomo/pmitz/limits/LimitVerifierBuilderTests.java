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

package io.terpomo.pmitz.limits;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.terpomo.pmitz.limits.impl.LimitRuleResolverImpl;
import io.terpomo.pmitz.limits.impl.LimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.LimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitVerifierBuilderTests {
	@Mock
	DataSource dataSource;

	@Mock
	LimitRuleResolver limitRuleResolver;

	@Mock
	UserLimitRepository userLimitRepository;

	@Test
	void builderShouldCreateLimitVerifierWithMinimalConfig() {
		var productRepo = LimitVerifierBuilder.inMemoryProductRepo();
		var builder = LimitVerifierBuilder.of(productRepo)
				.withDefaultLimitRuleResolver()
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var limitVerifierImpl = builder.build();
		assertThat(limitVerifierImpl).isNotNull();

		assertThat(limitVerifierImpl).extracting("limitRuleResolver").isInstanceOf(LimitRuleResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(limitVerifierImpl).extracting("limitRuleResolver")
				.extracting("userLimitRepository").isInstanceOf(LimitRuleResolverImpl.NoOpUserLimitRepository.class);

		assertThat(limitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(LimitVerificationStrategyDefaultResolver.class);
		assertThat(limitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}

	@Test
	void builderShouldCreateLimitVerifierWithUserLimitRespository() {
		var productRepo = LimitVerifierBuilder.inMemoryProductRepo();
		var builder = LimitVerifierBuilder.of(productRepo)
				.withUserLimitRepository(userLimitRepository)
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var limitVerifierImpl = builder.build();
		assertThat(limitVerifierImpl).isNotNull().isInstanceOf(LimitVerifierImpl.class);

		assertThat(limitVerifierImpl).extracting("limitRuleResolver").isInstanceOf(LimitRuleResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(limitVerifierImpl).extracting("limitRuleResolver")
				.extracting("userLimitRepository").isSameAs(userLimitRepository);

		assertThat(limitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(LimitVerificationStrategyDefaultResolver.class);
		assertThat(limitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);

	}

	@Test
	void builderShouldCreateLimitVerifierWithCustomLimitRuleResolver() {
		var productRepo = LimitVerifierBuilder.inMemoryProductRepo();
		var builder = LimitVerifierBuilder.of(productRepo)
				.withCustomLimitRuleResolver(limitRuleResolver)
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var limitVerifierImpl = builder.build();
		assertThat(limitVerifierImpl).isNotNull().isInstanceOf(LimitVerifierImpl.class);

		assertThat(limitVerifierImpl).extracting("limitRuleResolver").isSameAs(limitRuleResolver);

		assertThat(limitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(LimitVerificationStrategyDefaultResolver.class);
		assertThat(limitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}

	@Test
	void builderShouldCreateLimitVerifierWithCustomUsageRepository() {
		UsageRepository usageRepository = mock(UsageRepository.class);
		var productRepo = LimitVerifierBuilder.inMemoryProductRepo();

		var builder = LimitVerifierBuilder.of(productRepo)
				.withDefaultLimitRuleResolver()
				.withCustomUsageRepository(usageRepository);

		var limitVerifierImpl = builder.build();
		assertThat(limitVerifierImpl).isNotNull().isInstanceOf(LimitVerifierImpl.class);

		assertThat(limitVerifierImpl).extracting("limitRuleResolver").isInstanceOf(LimitRuleResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(limitVerifierImpl).extracting("limitRuleResolver")
				.extracting("userLimitRepository").isInstanceOf(LimitRuleResolverImpl.NoOpUserLimitRepository.class);

		assertThat(limitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(LimitVerificationStrategyDefaultResolver.class);
		assertThat(limitVerifierImpl).extracting("usageRepository").isSameAs(usageRepository);
	}

	@Test
	void builderShouldCreateLimitVerifierWithCustomVerificationStrategy() {
		var verificationStrategy = mock(LimitVerificationStrategy.class);
		var productRepo = LimitVerifierBuilder.inMemoryProductRepo();
		var builder = LimitVerifierBuilder.of(productRepo)
				.withDefaultLimitRuleResolver()
				.withJdbcUsageRepository(dataSource, "schema", "table")
				.withUserLimitVerificationStrategy(verificationStrategy);

		var limitVerifierImpl = builder.build();

		assertThat(limitVerifierImpl).isNotNull().isInstanceOf(LimitVerifierImpl.class);

		assertThat(limitVerifierImpl).extracting("limitRuleResolver").isInstanceOf(LimitRuleResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(limitVerifierImpl).extracting("limitRuleResolver")
				.extracting("userLimitRepository").isInstanceOf(LimitRuleResolverImpl.NoOpUserLimitRepository.class);

		assertThat(limitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(LimitVerificationStrategyDefaultResolver.class)
				.extracting("defaultVerificationStrategy").isSameAs(verificationStrategy);
		assertThat(limitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}
}
