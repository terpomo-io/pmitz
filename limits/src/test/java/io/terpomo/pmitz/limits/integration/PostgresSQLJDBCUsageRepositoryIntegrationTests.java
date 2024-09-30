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

package io.terpomo.pmitz.limits.integration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.TimeZone;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresSQLJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	private static final PostgreSQLContainer<?> postgresqlContainer =
			new PostgreSQLContainer<>("postgres:latest")
					.withEnv("TZ", "UTC")
					.withCommand("postgres", "-c", "timezone=UTC");

	@BeforeAll
	public static void setUpClass() {
		// Start the container before all tests
		postgresqlContainer.start();
		// Set timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@AfterAll
	public static void tearDownClass() {
		// Stop the container after all tests
		if (postgresqlContainer != null) {
			postgresqlContainer.stop();
		}
	}

	@Override
	protected void setupDataSource() {
		// Set up the data source using the container's JDBC URL
		dataSource = new BasicDataSource();
		String jdbcUrlWithTimezone = postgresqlContainer.getJdbcUrl() + "?sessionTimezone=UTC";
		dataSource.setUrl(jdbcUrlWithTimezone);
		dataSource.setUsername(postgresqlContainer.getUsername());
		dataSource.setPassword(postgresqlContainer.getPassword());

		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTimeZoneQuery() {
		return "SHOW timezone";
	}

	@Override
	protected boolean isSingleTimeZoneQuery() {
		return true; // PostgreSQL only returns one value
	}

	@Override
	protected String getTableName() {
		return "\"Usage\"";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			// Set session timezone for the current connection
			stmt.execute("SET LOCAL TIME ZONE 'UTC';");

			// Create schema and tables
			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
			stmt.execute("CREATE TABLE IF NOT EXISTS " + CUSTOM_SCHEMA + ".\"Usage\" (" +
					"usage_id SERIAL PRIMARY KEY, " +
					"feature_id VARCHAR(255), " +
					"product_id VARCHAR(255), " +
					"user_grouping VARCHAR(255), " +
					"limit_id VARCHAR(255), " +
					"window_start TIMESTAMP, " +
					"window_end TIMESTAMP, " +
					"units INT, " +
					"expiration_date TIMESTAMP, " +
					"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
					");");

			ResultSet rs = stmt.executeQuery("SHOW timezone;");
			if (rs.next()) {
				System.out.println("PostgreSQL Timezone: " + rs.getString(1));
			}
		}
	}

	@Override
	protected void tearDownDatabase() {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
			statement.execute("TRUNCATE TABLE " + getFullTableName() + " RESTART IDENTITY CASCADE");
		} catch (SQLException ex) {
			System.out.println("Error during tearDownDatabase: " + ex.getMessage());
		}
	}

	@Override
	protected void printDatabaseContents(String message) {
		System.out.println("---- " + message + " ----");
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + CUSTOM_SCHEMA + "." + getTableName());
			while (rs.next()) {
				int usageId = rs.getInt("usage_id");
				String featureId = rs.getString("feature_id");
				String productId = rs.getString("product_id");
				String userGrouping = rs.getString("user_grouping");
				String limitId = rs.getString("limit_id");
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				int units = rs.getInt("units");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				// Simply print the timestamp values as-is
				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		} catch (SQLException ex) {
			System.out.println("Error while printing database contents: " + ex.getMessage());
		}
	}
}
