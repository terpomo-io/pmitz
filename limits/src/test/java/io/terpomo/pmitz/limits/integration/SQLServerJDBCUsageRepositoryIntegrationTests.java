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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class SQLServerJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	@Container
	private static final MSSQLServerContainer<?> mssqlServerContainer =
			new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:latest")
					.withEnv("TZ", "UTC");  // Set container timezone to UTC

	@Override
	protected void setupDataSource() {
		mssqlServerContainer.start();
		dataSource = new BasicDataSource();
		dataSource.setUrl(mssqlServerContainer.getJdbcUrl());
		dataSource.setUsername(mssqlServerContainer.getUsername());
		dataSource.setPassword(mssqlServerContainer.getPassword());
		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTimeZoneQuery() {
		return "SELECT CURRENT_TIMEZONE()";
	}

	@Override
	protected boolean isSingleTimeZoneQuery() {
		return true; // SQL Server only returns one value
	}

	@Override
	protected String getTableName() {
		return "[Usage]";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {

			statement.execute("IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = '" + CUSTOM_SCHEMA + "') " +
					"EXEC('CREATE SCHEMA " + CUSTOM_SCHEMA + "')");

			// Set the session time zone to UTC (SQL Server doesn't directly support session time zones, but ensure UTC consistency)
			statement.execute("SET LANGUAGE us_english;"); // Ensure language consistency to handle dates properly
			statement.execute("SET DATEFORMAT ymd;"); // Ensure date format consistency for UTC handling

			String createTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Usage' AND schema_id = SCHEMA_ID('" + CUSTOM_SCHEMA + "')) " +
					"CREATE TABLE " + getFullTableName() + " (" +
					"usage_id INT PRIMARY KEY IDENTITY(1,1), " +
					"feature_id VARCHAR(255), " +
					"product_id VARCHAR(255), " +
					"user_grouping VARCHAR(255), " +
					"limit_id VARCHAR(255), " +
					"window_start DATETIME2, " +
					"window_end DATETIME2, " +
					"units INT, " +
					"expiration_date DATETIME2, " +
					"updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
					");";
			statement.execute(createTable);

			try (ResultSet rs = statement.executeQuery("SELECT SYSDATETIMEOFFSET()")) {
				if (rs.next()) {
					System.out.println("SQL Server current datetime with timezone: " + rs.getString(1));
				}
			}
		}
	}

	@Override
	protected void tearDownDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
			// Truncate the table between tests
			statement.execute("TRUNCATE TABLE " + getFullTableName());
		}
	}

	@Override
	protected void printDatabaseContents(String message) throws SQLException {
		System.out.println("---- " + message + " ----");
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + getFullTableName());
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

				// Simply print the timestamp values as-is without converting to UTC
				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
