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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.utils.JDBCTestUtils;

import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
public class MySQLJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	private static final Logger logger = LoggerFactory.getLogger(MySQLJDBCUsageRepositoryIntegrationTests.class);

	@Container
	private static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:lts")
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
	protected void setupDatabase() throws SQLException, IOException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			JDBCTestUtils.executeStatementsFile(
					stmt, "../resources/scripts/repos/sql/mysql_create.sql", CUSTOM_SCHEMA);
		}
	}


	@Override
	protected void tearDownDatabase() throws SQLException, IOException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			JDBCTestUtils.executeStatementsFile(
					stmt, "../resources/scripts/repos/sql/mysql_drop.sql", CUSTOM_SCHEMA);
		}
	}

	@Override
	protected void printDatabaseContents(String message) {
		logger.info("---- {} ----", message);
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

				logger.info("""
						UsageId: {}, FeatureId: {}, ProductId: {}, UserGrouping: {}, LimitId: {}, \
						WindowStart: {}, WindowEnd: {}, Units: {}, ExpirationDate: {}, UpdatedAt: {}\
						""",
						usageId, featureId, productId, userGrouping, limitId, windowStart, windowEnd, units,
						expirationDate, updatedAt);
			}
		}
		catch (SQLException ex) {
			fail("Error while printing database contents", ex);
		}
	}
}
