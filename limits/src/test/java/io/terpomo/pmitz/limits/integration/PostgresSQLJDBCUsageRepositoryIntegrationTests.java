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

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;

@Testcontainers
public class PostgresSQLJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	@Container
	private static final PostgreSQLContainer<?> postgresqlContainer =
			new PostgreSQLContainer<>("postgres:latest");

	@Override
	protected void setupDataSource() {
		dataSource = new BasicDataSource();
		dataSource.setUrl(postgresqlContainer.getJdbcUrl());
		dataSource.setUsername(postgresqlContainer.getUsername());
		dataSource.setPassword(postgresqlContainer.getPassword());

		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTableName() {
		return "\"Usage\"";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
			stmt.execute("CREATE TABLE IF NOT EXISTS " + CUSTOM_SCHEMA + ".\"Usage\" (" +
					"usage_id SERIAL PRIMARY KEY, " +
					"feature_id VARCHAR(255) NOT NULL, " +
					"product_id VARCHAR(255) NOT NULL, " +
					"user_grouping VARCHAR(255) NOT NULL, " +
					"limit_id VARCHAR(255) NOT NULL, " +
					"window_start TIMESTAMP NULL, " +
					"window_end TIMESTAMP NULL, " +
					"units INT NOT NULL, " +
					"expiration_date TIMESTAMP NULL, " +
					"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL" +
					");");

			stmt.execute("CREATE INDEX IF NOT EXISTS idx_limit_id ON " + CUSTOM_SCHEMA + ".\"Usage\" (limit_id);");
			stmt.execute("CREATE INDEX IF NOT EXISTS idx_feature_product_user ON " + CUSTOM_SCHEMA + ".\"Usage\" (feature_id, product_id, user_grouping);");
		}
	}

	@Override
	protected void tearDownDatabase() {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("TRUNCATE TABLE " + getFullTableName() + " RESTART IDENTITY CASCADE");
		} catch (SQLException ex) {
			System.out.println("Error during tearDownDatabase: " + ex.getMessage());
		}
	}

	@Override
	protected void printDatabaseContents(String message) {
		System.out.println("---- " + message + " ----");
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + CUSTOM_SCHEMA + "." + getTableName())) {

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

				System.out.println("UsageId: " + usageId +
						", FeatureId: " + featureId +
						", ProductId: " + productId +
						", UserGrouping: " + userGrouping +
						", LimitId: " + limitId +
						", WindowStart: " + windowStart +
						", WindowEnd: " + windowEnd +
						", Units: " + units +
						", ExpirationDate: " + expirationDate +
						", UpdatedAt: " + updatedAt);
			}
		} catch (SQLException ex) {
			System.out.println("Error while printing database contents: " + ex.getMessage());
		}
	}
}
