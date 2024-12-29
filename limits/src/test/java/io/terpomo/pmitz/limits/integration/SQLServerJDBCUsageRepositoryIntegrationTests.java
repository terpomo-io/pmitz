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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.utils.JDBCUtils;

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
	protected void setupDatabase() throws SQLException, IOException {
		try (Connection conn = dataSource.getConnection();
			Statement stmt = conn.createStatement()) {

			JDBCUtils.executeStatementsFile(
					stmt, "../resources/scripts/repos/sql/sqlserver_create.sql", CUSTOM_SCHEMA);
		}
	}

	@Override
	protected void tearDownDatabase() throws SQLException, IOException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			JDBCUtils.executeStatementsFile(
					stmt, "../resources/scripts/repos/sql/sqlserver_drop.sql", CUSTOM_SCHEMA);
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
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
