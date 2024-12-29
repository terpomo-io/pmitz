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

package io.terpomo.pmitz.limits.userlimit.integration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;

import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.userlimit.jdbc.JDBCUserLimitRepository;
import io.terpomo.pmitz.utils.JDBCUtils;

public class SQLServerJDBCUserLimitRepositoryIntegrationTests extends AbstractJDBCUserLimitRepositoryIntegrationTests {

	@Container
	private static final MSSQLServerContainer<?> sqlServerContainer =
			new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:latest")
					.withEnv("TZ", "Europe/Berlin");

	@Override
	protected void setupDataSource() {
		sqlServerContainer.start();
		dataSource = new BasicDataSource();
		dataSource.setUrl(sqlServerContainer.getJdbcUrl());
		dataSource.setUsername(sqlServerContainer.getUsername());
		dataSource.setPassword(sqlServerContainer.getPassword());
		repository = new JDBCUserLimitRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);
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

	protected void populateTable() throws SQLException {

		CountLimit countLimit = new CountLimit("Maximum picture size", 10);
		countLimit.setUnit("Go");
		insertUserUsageLimitRecord(this.feature, this.user, countLimit);

		countLimit = new CountLimit("Maximum number of picture", 50);
		insertUserUsageLimitRecord(this.feature, this.user, countLimit);

		CalendarPeriodRateLimit calendarPeriodRateLimit = new CalendarPeriodRateLimit(
				"Maximum number of picture uploaded in calendar month", 1000, CalendarPeriodRateLimit.Periodicity.MONTH
		);
		insertUserUsageLimitRecord(this.feature, this.user, calendarPeriodRateLimit);

		SlidingWindowRateLimit slidingWindowRateLimit = new SlidingWindowRateLimit(
				"Maximum number of picture uploaded by day", 15, ChronoUnit.DAYS, 1
		);
		insertUserUsageLimitRecord(this.feature, this.user, slidingWindowRateLimit);
	}

	private void insertUserUsageLimitRecord(Feature feature, UserGrouping userGroup,
			UsageLimit usageLimit) throws SQLException {

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
			if (usageLimit instanceof CalendarPeriodRateLimit calendarPeriodRateLimit) {
				limitInterval = calendarPeriodRateLimit.getPeriodicity().name();
				limitDuration = calendarPeriodRateLimit.getDuration();
			}
			else if (usageLimit instanceof SlidingWindowRateLimit slidingWindowRateLimit) {
				limitInterval = slidingWindowRateLimit.getInterval().name();
				limitDuration = slidingWindowRateLimit.getDuration();
			}
			stmt.setString(1, usageLimit.getId());
			stmt.setString(2, feature.getFeatureId());
			stmt.setString(3, userGroup.getId());
			stmt.setString(4, usageLimit.getClass().getSimpleName());
			stmt.setLong(5, usageLimit.getValue());
			stmt.setString(6, usageLimit.getUnit());
			stmt.setString(7, limitInterval);
			stmt.setInt(8, limitDuration);
			stmt.executeUpdate();
		}
	}
}
