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

package io.terpomo.pmitz.core.integration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.core.repository.userlimit.jdbc.JDBCUserLimitRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class PostgreSQLJDBCUserLimitRepositoryIntegrationTests extends AbstractJDBCUserLimitRepositoryIntegrationTests {

	@Container
	private static final PostgreSQLContainer<?> mysqlContainer =
			new PostgreSQLContainer<>("postgres:latest").withEnv("TZ", "Europe/Berlin");


	@Override
	protected void setupDataSource() {
		mysqlContainer.start();
		dataSource = new BasicDataSource();
		dataSource.setUrl(mysqlContainer.getJdbcUrl());
		dataSource.setUsername(mysqlContainer.getUsername());
		dataSource.setPassword(mysqlContainer.getPassword());
		repository = new JDBCUserLimitRepository(dataSource, SCHEMA_NAME, TABLE_NAME);
	}

	@Override
	protected void setupDatabase() throws SQLException {
		String createQuery = String.format(
				"""
				CREATE TABLE IF NOT EXISTS %s.%s (
					usage_id SERIAL PRIMARY KEY,
					limit_id VARCHAR(255),
					feature_id VARCHAR(255),
					user_group_id VARCHAR(255),
					limit_type VARCHAR(255),
					limit_value INT,
					limit_unit VARCHAR(255),
					limit_interval VARCHAR(255),
					limit_duration INT
				);
				""",
				SCHEMA_NAME, TABLE_NAME);

		String addContraintQuery = String.format(
				"""
				ALTER TABLE %s.%s
					ADD CONSTRAINT c_limit UNIQUE (limit_id,feature_id, user_group_id)
				""",
				SCHEMA_NAME, TABLE_NAME);

		try (Connection conn = dataSource.getConnection();
			Statement stmt = conn.createStatement()) {
			stmt.execute(createQuery);
			stmt.execute(addContraintQuery);
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
				SCHEMA_NAME, TABLE_NAME);

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

	@Override
	protected void tearDownDatabase() throws SQLException {
		String dropContraintQuery = String.format(
				"""
				DROP TABLE IF EXISTS %s.%s
				""",
				SCHEMA_NAME, TABLE_NAME);
		try (Connection conn = dataSource.getConnection();
			Statement statement = conn.createStatement()) {
			statement.execute(dropContraintQuery);
		}
	}

	// TODO: Remove
	@Override
	protected void printDatabaseContents() {

		System.out.printf("========= %s.%s =========%n", SCHEMA_NAME, TABLE_NAME);

		String query = String.format(
				"""
				SELECT * FROM %s.%s
				""",
				SCHEMA_NAME, TABLE_NAME);
		try (Connection conn = dataSource.getConnection();
			Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				int usageId = rs.getInt("usage_id");
				String limitId = rs.getString("limit_id");
				String featureId = rs.getString("feature_id");
				String userGroupId = rs.getString("user_group_id");
				String limitType = rs.getString("limit_type");
				long limitValue = rs.getLong("limit_value");
				String limitUnit = rs.getString("limit_unit");
				String limitInterval = rs.getString("limit_interval");
				int limitDuration = rs.getInt("limit_duration");

				System.out.printf(
						"""
						usageId=%s, limitId=%s, featureId=%s, userGroupId=%s, limitType=%s,
						limitValue=%d, limitUnit=%s, limitInterval=%s, limitDuration=%d%n
						""",
						usageId, limitId, featureId, userGroupId, limitType,
						limitValue, limitUnit, limitInterval, limitDuration);
			}
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
