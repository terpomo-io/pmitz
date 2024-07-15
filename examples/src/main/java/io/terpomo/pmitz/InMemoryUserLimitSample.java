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

package io.terpomo.pmitz;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.limits.UsageLimitVerifierBuilder;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;

public class InMemoryUserLimitSample {

	private static final String USER_USAGE_TABLE_NAME = "usage";
	private static final String USER_LIMIT_TABLE_NAME = "user_limit";
	private static final String DB_SCHEMA_NAME = "dbo";

	private UsageLimitVerifier usageLimitVerifier;
	private UserLimitRepository userLimitRepository;
	private Product product;

	public static void main(String[] args) {
		JdbcDataSource dataSource;
		try {
			Class.forName("org.h2.Driver");

			dataSource = new JdbcDataSource();
			dataSource.setURL("jdbc:h2:mem:dbo;DB_CLOSE_DELAY=-1;");
			dataSource.setUser("sa");
			dataSource.setPassword("");

			Connection conn = dataSource.getConnection();
				RunScript.execute(conn, new InputStreamReader(
						InMemoryUserLimitSample.class.getResourceAsStream("/init-usage-and-user-limit-repos.sql")));
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		InMemoryUserLimitSample sampleApp = new InMemoryUserLimitSample();

		sampleApp.initLimitVerifier(dataSource);

		// First call : Within the limit
		sampleApp.reserveBooks(5);

		sampleApp.showRemainingUnits();

		sampleApp.changeMaximumBooksReservedLimit(6);

		sampleApp.showRemainingUnits();

		// Second call : Fail because the limit reached in the first call
		sampleApp.reserveBooks(2);

	}

	public void reserveBooks(int numberOfBooks) {
		Feature feature = product.getFeatures().stream().filter(ft -> ft.getFeatureId().equals("Reserving books")).findFirst().get();

		try {
			usageLimitVerifier.recordFeatureUsage(feature, new IndividualUser("user001"), Collections.singletonMap("Maximum books reserved", (long) numberOfBooks));

			// Your business logic here
			System.out.println("Books reserved!");

		}
		catch (LimitExceededException ex) {
			System.out.println("Oops! Looks like you exceeded your reservation limit");
		}

	}

	public void changeMaximumBooksReservedLimit(long numberOfBooks) {
		Feature feature = product.getFeatures().stream().filter(ft -> ft.getFeatureId().equals("Reserving books")).findFirst().get();

		userLimitRepository.updateUsageLimit(feature,
				new CountLimit("Maximum books reserved", numberOfBooks), new IndividualUser("user001"));
	}

	public void showRemainingUnits() {
		Feature feature = product.getFeatures().stream().filter(ft -> ft.getFeatureId().equals("Reserving books")).findFirst().get();

		Map<String, Long> limitsRemain =  usageLimitVerifier.getLimitsRemainingUnits(feature, new IndividualUser("user001"));

		System.out.println(limitsRemain);
	}

	private void initLimitVerifier(DataSource usageRepoDataSource) {
		InMemoryProductRepository productRepo = new InMemoryProductRepository();
		try {
			productRepo.load(InMemoryUserLimitSample.class.getResourceAsStream("/products_repository.json"));
		}
		catch (IOException ex) {
			throw new RuntimeException("Product Repository file not found", ex);
		}

		var optProduct = productRepo.getProductById("Library");
		if (optProduct.isEmpty()) {
			throw new IllegalStateException("Product not found in repo");
		}
		product = optProduct.get();

		userLimitRepository = new JDBCUserLimitRepository(usageRepoDataSource, DB_SCHEMA_NAME, USER_LIMIT_TABLE_NAME);

		usageLimitVerifier = UsageLimitVerifierBuilder.of(productRepo)
				.withDefaultUsageLimitResolver(userLimitRepository)
				.withJdbcUsageRepository(usageRepoDataSource, DB_SCHEMA_NAME, USER_USAGE_TABLE_NAME)
				.build();
	}
}
