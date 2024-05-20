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

import io.terpomo.pmitz.limits.impl.UsageLimitResolverImpl;
import io.terpomo.pmitz.limits.impl.UsageLimitVerifierImpl;
import io.terpomo.pmitz.limits.impl.strategy.UsageLimitVerificationStrategyDefaultResolver;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageLimitVerifierBuilderTest {
	@Mock
	DataSource dataSource;

	@Mock
	UsageLimitResolver usageLimitResolver;

	@Mock
	UserLimitRepository userLimitRepository;

	@Test
	void builderShouldCreateUsageLimitVerifierWithMinimalConfig() {
		var productRepo = UsageLimitVerifierBuilder.inMemoryProductRepo();
		var builder = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver()
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var usageLimitVerifierImpl = builder.build();
		assertThat(usageLimitVerifierImpl).isNotNull();

		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver").isInstanceOf(UsageLimitResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver")
				.extracting("userLimitRepository").isInstanceOf(UsageLimitResolverImpl.NoOpUserLimitRepository.class);

		assertThat(usageLimitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(UsageLimitVerificationStrategyDefaultResolver.class);
		assertThat(usageLimitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}

	@Test
	void builderShouldCreateUsageLimitVerifierWithUserLimitRespository() {
		var productRepo = UsageLimitVerifierBuilder.inMemoryProductRepo();
		var builder = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver(userLimitRepository)
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var usageLimitVerifierImpl = builder.build();
		assertThat(usageLimitVerifierImpl).isNotNull().isInstanceOf(UsageLimitVerifierImpl.class);

		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver").isInstanceOf(UsageLimitResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver")
				.extracting("userLimitRepository").isSameAs(userLimitRepository);

		assertThat(usageLimitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(UsageLimitVerificationStrategyDefaultResolver.class);
		assertThat(usageLimitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);

	}

	@Test
	void builderShouldCreateUsageLimitVerifierWithCustomUsageLimitResolver() {
		var productRepo = UsageLimitVerifierBuilder.inMemoryProductRepo();
		var builder = UsageLimitVerifierBuilder.of(productRepo)
				.withCustomUsageLimitResolver(usageLimitResolver)
				.withJdbcUsageRepository(dataSource, "schema", "table");

		var usageLimitVerifierImpl = builder.build();
		assertThat(usageLimitVerifierImpl).isNotNull().isInstanceOf(UsageLimitVerifierImpl.class);

		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver").isSameAs(usageLimitResolver);

		assertThat(usageLimitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(UsageLimitVerificationStrategyDefaultResolver.class);
		assertThat(usageLimitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}

	@Test
	void builderShouldCreateUsageLimitVerifierWithCustomUsageRepository() {
		UsageRepository usageRepository = mock(UsageRepository.class);
		var productRepo = UsageLimitVerifierBuilder.inMemoryProductRepo();

		var builder = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver()
				.withCustomUsageRepository(usageRepository);

		var usageLimitVerifierImpl = builder.build();
		assertThat(usageLimitVerifierImpl).isNotNull().isInstanceOf(UsageLimitVerifierImpl.class);

		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver").isInstanceOf(UsageLimitResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver")
				.extracting("userLimitRepository").isInstanceOf(UsageLimitResolverImpl.NoOpUserLimitRepository.class);

		assertThat(usageLimitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(UsageLimitVerificationStrategyDefaultResolver.class);
		assertThat(usageLimitVerifierImpl).extracting("usageRepository").isSameAs(usageRepository);
	}

	@Test
	void builderShouldCreateUsageLimitVerifierWithCustomVerificationStrategy() {
		var verificationStrategy = mock(UsageLimitVerificationStrategy.class);
		var productRepo = UsageLimitVerifierBuilder.inMemoryProductRepo();
		var builder = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver()
				.withJdbcUsageRepository(dataSource, "schema", "table")
				.withUserLimitVerificationStrategy(verificationStrategy);

		var usageLimitVerifierImpl = builder.build();

		assertThat(usageLimitVerifierImpl).isNotNull().isInstanceOf(UsageLimitVerifierImpl.class);

		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver").isInstanceOf(UsageLimitResolverImpl.class)
				.extracting("productRepository").isSameAs(productRepo);
		assertThat(usageLimitVerifierImpl).extracting("usageLimitResolver")
				.extracting("userLimitRepository").isInstanceOf(UsageLimitResolverImpl.NoOpUserLimitRepository.class);

		assertThat(usageLimitVerifierImpl).extracting("limitVerifierStrategyResolver")
				.isInstanceOf(UsageLimitVerificationStrategyDefaultResolver.class)
				.extracting("defaultVerificationStrategy").isSameAs(verificationStrategy);
		assertThat(usageLimitVerifierImpl).extracting("usageRepository").isInstanceOf(JDBCUsageRepository.class);
	}
}