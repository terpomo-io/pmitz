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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;

@Testcontainers
public class SQLServerJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	@Container
	private static final MSSQLServerContainer<?> mssqlServerContainer =
			new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:latest")
					.acceptLicense();

	@Override
	protected void setupDataSource() {
		dataSource = new BasicDataSource();
		dataSource.setUrl(mssqlServerContainer.getJdbcUrl());
		dataSource.setUsername(mssqlServerContainer.getUsername());
		dataSource.setPassword(mssqlServerContainer.getPassword());
		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTableName() {
		return "[Usage]";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			stmt.execute("IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = '" + CUSTOM_SCHEMA + "') " +
					"EXEC('CREATE SCHEMA " + CUSTOM_SCHEMA + "')");

			String createTable = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Usage' AND schema_id = SCHEMA_ID('" + CUSTOM_SCHEMA + "')) " +
					"CREATE TABLE " + getFullTableName() + " (" +
					"usage_id INT PRIMARY KEY IDENTITY(1,1), " +
					"feature_id NVARCHAR(255) NOT NULL, " +
					"product_id NVARCHAR(255) NOT NULL, " +
					"user_grouping NVARCHAR(255) NOT NULL, " +
					"limit_id NVARCHAR(255) NOT NULL, " +
					"window_start DATETIME2 NULL, " +
					"window_end DATETIME2 NULL, " +
					"units INT NOT NULL, " +
					"expiration_date DATETIME2 NULL, " +
					"updated_at DATETIME2 DEFAULT SYSUTCDATETIME() NOT NULL" +
					");";

			stmt.execute(createTable);

			stmt.execute("IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_limit_id' AND object_id = OBJECT_ID('" + getFullTableName() + "')) " +
					"CREATE INDEX idx_limit_id ON " + getFullTableName() + " (limit_id);");

			stmt.execute("IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_feature_product_user' AND object_id = OBJECT_ID('" + getFullTableName() + "')) " +
					"CREATE INDEX idx_feature_product_user ON " + getFullTableName() + " (feature_id, product_id, user_grouping);");
		}
	}

	@Override
	protected void tearDownDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("TRUNCATE TABLE " + getFullTableName());
		}
	}

	@Override
	protected void printDatabaseContents(String message) {
		System.out.println("---- " + message + " ----");
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + getFullTableName())) {

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
			ex.printStackTrace();
		}
	}
}
