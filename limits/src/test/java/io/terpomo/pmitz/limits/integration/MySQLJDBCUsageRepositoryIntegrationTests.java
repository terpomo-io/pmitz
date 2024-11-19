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

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MySQLJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	@Container
	private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:latest")
			.withDatabaseName(CUSTOM_SCHEMA);


	@Override
	protected void setupDataSource() {
		dataSource = new BasicDataSource();
		dataSource.setUrl(mysqlContainer.getJdbcUrl());
		dataSource.setUsername(mysqlContainer.getUsername());
		dataSource.setPassword(mysqlContainer.getPassword());
		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTableName() {
		return "`Usage`";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
			stmt.execute("CREATE TABLE IF NOT EXISTS " + CUSTOM_SCHEMA + ".`Usage` (" +
					"usage_id INT AUTO_INCREMENT PRIMARY KEY, " +
					"feature_id VARCHAR(255), " +
					"product_id VARCHAR(255), " +
					"user_grouping VARCHAR(255), " +
					"limit_id VARCHAR(255), " +
					"window_start TIMESTAMP, " +
					"window_end TIMESTAMP, " +
					"units INT, " +
					"expiration_date TIMESTAMP, " +
					"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
					"INDEX idx_limit_id (`limit_id`)," +       // Index on limit_id
					"INDEX idx_feature_product_user (`feature_id`, `product_id`, `user_grouping`)" + // Composite index
					");");
		}
	}

	@Override
	protected void tearDownDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {

			statement.execute("SET FOREIGN_KEY_CHECKS=0");
			statement.execute("TRUNCATE TABLE " + CUSTOM_SCHEMA + "." + getTableName());
			statement.execute("SET FOREIGN_KEY_CHECKS=1");
		}
	}

	@Override
	protected void printDatabaseContents(String message) throws SQLException {
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

				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		}
	}
}
