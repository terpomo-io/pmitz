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

package io.terpomo.pmitz.subscriptions.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;

import static org.assertj.core.api.Assertions.assertThat;

class JDBCSubscriptionRepositoryTests {

	private static final String SCHEMA = "dbo";
	private static final String SUBSCRIPTION_TABLE = "subscription";
	private static final String SUBSCRIPTION_PLAN_TABLE = "subscription_plan";

	private JdbcDataSource dataSource;
	private JDBCSubscriptionRepository repository;

	@BeforeEach
	void setUp() throws SQLException {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:subs-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		createSchema();
		repository = new JDBCSubscriptionRepository(dataSource, SCHEMA, SUBSCRIPTION_TABLE, SUBSCRIPTION_PLAN_TABLE);
	}

	@Test
	void createAndFindSubscriptionShouldReturnPlanMappings() {
		Subscription subscription = new Subscription("sub-001");
		subscription.setStatus(SubscriptionStatus.ACTIVE);
		subscription.setExpirationDate(ZonedDateTime.now(ZoneOffset.UTC).plusDays(5));
		subscription.setPlans(Map.of("product-a", "basic", "product-b", "premium"));

		repository.create(subscription);

		Subscription stored = repository.find("sub-001").orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		assertThat(stored.getExpirationDate().truncatedTo(ChronoUnit.MILLIS))
				.isEqualTo(subscription.getExpirationDate().truncatedTo(ChronoUnit.MILLIS));
		assertThat(stored.getPlansByProduct())
				.containsEntry("product-a", "basic")
				.containsEntry("product-b", "premium");
	}

	@Test
	void updateStatusShouldPersistChanges() {
		Subscription subscription = new Subscription("sub-002");
		subscription.setStatus(SubscriptionStatus.TRIAL);
		subscription.setPlans(Map.of("product-a", "basic"));
		repository.create(subscription);

		repository.updateStatus("sub-002", SubscriptionStatus.CANCELLED);

		Subscription stored = repository.find("sub-002").orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
	}

	@Test
	void findShouldReturnEmptyWhenMissing() {
		assertThat(repository.find("missing")).isEmpty();
	}

	private void createSchema() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
			stmt.execute(
					"""
					CREATE TABLE IF NOT EXISTS %s.%s (
						subscription_id VARCHAR(255) PRIMARY KEY,
						status VARCHAR(50) NOT NULL,
						expiration_date TIMESTAMP NULL
					)
					""".formatted(SCHEMA, SUBSCRIPTION_TABLE));
			stmt.execute(
					"""
					CREATE TABLE IF NOT EXISTS %s.%s (
						subscription_id VARCHAR(255) NOT NULL,
						product_id VARCHAR(255) NOT NULL,
						plan_id VARCHAR(255) NOT NULL,
						PRIMARY KEY (subscription_id, product_id)
					)
					""".formatted(SCHEMA, SUBSCRIPTION_PLAN_TABLE));
		}
	}
}
