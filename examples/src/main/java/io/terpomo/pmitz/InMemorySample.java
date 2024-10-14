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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitVerifier;
import io.terpomo.pmitz.limits.UsageLimitVerifierBuilder;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

public class InMemorySample {

	private static final String DB_SCHEMA_NAME = "dbo";
	private static final String DB_USER_USAGE_TABLE_NAME = "usage";
	private static final String DB_USER_LIMIT_TABLE_NAME = "user_limit";
	private static final String DB_CREATION_SCRIPT = "/init-usage-and-user-limit-repos.sql";

	private static final String USER_ID_1 = "user001";
	private static final String USER_ID_2 = "user002";
	private static final String PRODUCT_ID = "Library";
	private static final String FEATURE_ID = "Reserving books";
	private static final String LIMIT_ID = "Maximum books reserved";

	private UsageLimitVerifier usageLimitVerifier;
	private UserLimitRepository userLimitRepository;
	private Product product;

	public static void main(String[] args) {
		InMemorySample sampleApp = new InMemorySample();

		JdbcDataSource dataSource = sampleApp.createDataSource();
		sampleApp.createDatabase(dataSource);

		sampleApp.initLimitVerifier(dataSource);


		// Scenario 1
		// The user 'user1' has his limit increased in order to reserve more books than the general limit allowed.

		UserGrouping user1 = new IndividualUser(USER_ID_1);

		// First call: User user001 attempts to reserve 6 books which exceeds the overall limit of 5 books
		sampleApp.reserveBooks(user1, 6);

		// Added a limit to the user user1 to allow him to reserve 6 books
		sampleApp.addUsageLimitForUser(user1, 6);

		// Second call : Within the limit
		sampleApp.reserveBooks(user1, 6);


		// Scenario 2
		// User 'user2' attempted to reserve more books than allowed.
		// After the refusal, he made a second reservation attempt with a number of books within the permitted limit.

		UserGrouping user2 = new IndividualUser(USER_ID_2);

		// First call: User user2 attempts to reserve 7 books which exceeds the overall limit of 5 books
		sampleApp.reserveBooks(user2, 7);

		// Second call: The user 'user2' reserves a number of books in accordance with the general authorized limit.
		sampleApp.reserveBooks(user2, 5);
	}

	private void reserveBooks(UserGrouping user, int numberOfBooks) {
		Feature feature = getFeature();
		try {
			System.out.printf("The user %s wants to reserve %d books%n", user.getId(), numberOfBooks);

			usageLimitVerifier.recordFeatureUsage(feature, user, Collections.singletonMap(LIMIT_ID, (long) numberOfBooks));

			// Your business logic here
			System.out.println("Books reserved!");
		}
		catch (LimitExceededException ex) {
			System.out.printf("Oops ! It appears that user '%s' has exceeded their reservation limit.%n",
					user.getId());

			Map<String, Long> limitsRemain =  usageLimitVerifier.getLimitsRemainingUnits(feature, user);

			System.out.printf("The reservation limit for user %s is %d books.%n",
					user.getId(), limitsRemain.get(LIMIT_ID));
		}
	}

	private void addUsageLimitForUser(UserGrouping user, long numberOfBooks) {
		Feature feature = getFeature();

		userLimitRepository.updateUsageLimit(feature, new CountLimit(LIMIT_ID, numberOfBooks), user);

		System.out.printf("The reservation limit for user '%s' has been increased to a %d books.%n",
				user.getId(), numberOfBooks);
	}

	private void initLimitVerifier(DataSource usageRepoDataSource) {
		InMemoryProductRepository productRepo = new InMemoryProductRepository();
		try {
			productRepo.load(InMemorySample.class.getResourceAsStream("/products_repository.json"));
		}
		catch (IOException ex) {
			throw new RuntimeException("Product Repository file not found", ex);
		}

		product = productRepo
				.getProductById(PRODUCT_ID)
				.orElseThrow(() -> new RuntimeException("Product not found: %s".formatted(PRODUCT_ID)));

		userLimitRepository = UserLimitRepository.builder().jdbcRepository(usageRepoDataSource, DB_SCHEMA_NAME, DB_USER_LIMIT_TABLE_NAME);

		usageLimitVerifier = UsageLimitVerifierBuilder.of(productRepo)
				.withUserLimitRepository(userLimitRepository)
				.withJdbcUsageRepository(usageRepoDataSource, DB_SCHEMA_NAME, DB_USER_USAGE_TABLE_NAME)
				.build();
	}

	private JdbcDataSource createDataSource() {
		JdbcDataSource dataSource;

		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:dbo;DB_CLOSE_DELAY=-1;");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		return dataSource;
	}

	private void createDatabase(JdbcDataSource dataSource) {
		InputStream inputStream =
				InMemorySample.class.getResourceAsStream(DB_CREATION_SCRIPT);
		if (inputStream == null) {
			throw new RuntimeException("Resource not found: %s".formatted(DB_CREATION_SCRIPT));
		}

		try {
			InputStreamReader reader = new InputStreamReader(inputStream);
			Connection conn = dataSource.getConnection();
			RunScript.execute(conn, reader);
		}
		catch (Exception ex) {
			throw new RuntimeException("Error executing script", ex);
		}
	}

	private Feature getFeature() {
		Optional<Feature> featureOpt = product.getFeatures().stream().filter(ft -> ft.getFeatureId().equals(FEATURE_ID)).findFirst();
		return featureOpt.orElseThrow(() -> new RuntimeException("Feature not found: %s".formatted(FEATURE_ID)));
	}
}
