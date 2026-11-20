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

package io.terpomo.pmitz.limits;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.LimitExceededException;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit.Periodicity;
import io.terpomo.pmitz.core.repository.product.inmemory.InMemoryProductRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CalendarLimitJdbcEnforcementTests {

	private static final String SCHEMA = "pmitz_calendar";

	private static final String LIMIT_ID = "exports";

	private final FeatureRef featureRef = new FeatureRef("reports", "export");

	private final UserGrouping user = new IndividualUser("user001");

	private JdbcDataSource dataSource;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:calendarlimits;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
			stmt.execute("CREATE TABLE " + SCHEMA + ".usage ("
					+ "usage_id serial PRIMARY KEY, "
					+ "feature_id varchar, "
					+ "product_id varchar, "
					+ "user_grouping varchar, "
					+ "limit_id varchar, "
					+ "window_start TIMESTAMP, "
					+ "window_end TIMESTAMP, "
					+ "units integer, "
					+ "expiration_date TIMESTAMP, "
					+ "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
					+ ");");
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS " + SCHEMA + ".usage;");
		}
	}

	@ParameterizedTest
	@EnumSource(Periodicity.class)
	void recordFeatureUsageShouldAccumulateUnitsWithinTheCalendarPeriod(Periodicity periodicity) {
		LimitVerifier limitVerifier = limitVerifierWithQuotaOfTwoPer(periodicity);
		Map<String, Long> oneUnit = Map.of(LIMIT_ID, 1L);

		limitVerifier.recordFeatureUsage(featureRef, user, oneUnit);
		assertThat(limitVerifier.getLimitsRemainingUnits(featureRef, user)).containsEntry(LIMIT_ID, 1L);

		limitVerifier.recordFeatureUsage(featureRef, user, oneUnit);
		assertThat(limitVerifier.getLimitsRemainingUnits(featureRef, user)).containsEntry(LIMIT_ID, 0L);
	}

	@ParameterizedTest
	@EnumSource(Periodicity.class)
	void recordFeatureUsageShouldBeRefusedOnceTheCalendarQuotaIsUsed(Periodicity periodicity) {
		LimitVerifier limitVerifier = limitVerifierWithQuotaOfTwoPer(periodicity);
		Map<String, Long> oneUnit = Map.of(LIMIT_ID, 1L);

		limitVerifier.recordFeatureUsage(featureRef, user, oneUnit);
		limitVerifier.recordFeatureUsage(featureRef, user, oneUnit);

		assertThatExceptionOfType(LimitExceededException.class)
				.isThrownBy(() -> limitVerifier.recordFeatureUsage(featureRef, user, oneUnit));
		assertThat(limitVerifier.isWithinLimits(featureRef, user, oneUnit)).isFalse();
	}

	private LimitVerifier limitVerifierWithQuotaOfTwoPer(Periodicity periodicity) {
		Product product = new Product("reports");
		var calendarLimit = new CalendarPeriodRateLimit(LIMIT_ID, 2, periodicity);
		product.setFeatures(List.of(new Feature(product, "export", List.of(calendarLimit))));
		var productRepository = new InMemoryProductRepository();
		productRepository.addProduct(product);

		return LimitVerifierBuilder.of(productRepository)
				.withDefaultLimitRuleResolver()
				.withJdbcUsageRepository(dataSource, SCHEMA, "usage")
				.build();
	}

}
