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

package io.terpomo.pmitz.limits.userlimit.integration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.terpomo.pmitz.core.limits.LimitRule;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;
import io.terpomo.pmitz.utils.JDBCTestUtils;

@Testcontainers
public class MySQLJDBCUserLimitRepositoryIntegrationTests extends AbstractJDBCUserLimitRepositoryIntegrationTests {

	@Container
	private static final MySQLContainer<?> mysqlContainer =
			new MySQLContainer<>("mysql:lts")
					.withDatabaseName(CUSTOM_SCHEMA)
					.withEnv("TZ", "America/New_York");

	@Override
	protected void setupDataSource() {
		dataSource = new BasicDataSource();
		dataSource.setUrl(mysqlContainer.getJdbcUrl());
		dataSource.setUsername(mysqlContainer.getUsername());
		dataSource.setPassword(mysqlContainer.getPassword());
		repository = new JDBCUserLimitRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);
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

	protected void populateTable() throws SQLException {

		CountLimit countLimit = new CountLimit("Maximum picture size", 10);
		countLimit.setUnit("Go");
		insertLimitRuleRecord(this.featureRef, this.user, countLimit);

		countLimit = new CountLimit("Maximum number of picture", 50);
		insertLimitRuleRecord(this.featureRef, this.user, countLimit);

		CalendarPeriodRateLimit calendarPeriodRateLimit = new CalendarPeriodRateLimit(
				"Maximum number of picture uploaded in calendar month", 1000, CalendarPeriodRateLimit.Periodicity.MONTH
		);
		insertLimitRuleRecord(this.featureRef, this.user, calendarPeriodRateLimit);
	}

	private void insertLimitRuleRecord(FeatureRef featureRef, UserGrouping userGroup,
			LimitRule limitRule) throws SQLException {

		String query = String.format(
				"""
						INSERT INTO %s.%s (
							limit_id, feature_id, user_group_id, limit_type, limit_value,
							limit_unit, limit_interval, limit_duration
						) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
						""",
				CUSTOM_SCHEMA, TABLE_NAME);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)) {

			String limitInterval = null;
			int limitDuration = -1;
			if (limitRule instanceof CalendarPeriodRateLimit calendarPeriodRateLimit) {
				limitInterval = calendarPeriodRateLimit.getPeriodicity().name();
				limitDuration = calendarPeriodRateLimit.getDuration();
			}
			stmt.setString(1, limitRule.getId());
			stmt.setString(2, featureRef.featureId());
			stmt.setString(3, userGroup.getId());
			stmt.setString(4, limitRule.getClass().getSimpleName());
			stmt.setLong(5, limitRule.getValue());
			stmt.setString(6, limitRule.getUnit());
			stmt.setString(7, limitInterval);
			stmt.setInt(8, limitDuration);
			stmt.executeUpdate();
		}
	}
}
