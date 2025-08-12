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

package io.terpomo.pmitz.limits.userlimit.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JDBCUserLimitRepositoryTests {

	private static final String SCHEMA_NAME = "public";
	private static final String TABLE_NAME = "user_usage_limit";

	private final Product product = new Product("Picture hosting service");
	private final Feature feature = new Feature(this.product, "Uploading pictures");
	private final UserGrouping user = new IndividualUser("User1");

	private UserLimitRepository repository;
	private JdbcDataSource dataSource;

	static Stream<Arguments> findUsageLimit_invalidParameter() {
		return Stream.of(
				Arguments.of(null,
						"Maximum picture resolution", new IndividualUser("user1"),
						"The feature parameter cannot be null"),
				Arguments.of(new Feature(new Product(null), "f1"),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(null, "f1"),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(new Product("p1"), null),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						null, new IndividualUser("user1"),
						"The 'limitId' parameter cannot be null"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						"Maximum picture resolution", new IndividualUser(null),
						"A user or group of users must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						"Maximum picture resolution", null,
						"The user/group parameter cannot be null")
		);
	}

	static Stream<Arguments> updateUsageLimit_invalidParameter() {
		return Stream.of(
				Arguments.of(null,
						new CountLimit("c1", 10), new IndividualUser("user1"),
						"The feature parameter cannot be null"),
				Arguments.of(new Feature(new Product(null), "f1"),
						new CountLimit("c1", 10), new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(null, "f1"),
						new CountLimit("c1", 10), new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(new Product("p1"), null),
						new CountLimit("c1", 10), new IndividualUser("user1"),
						"A feature must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						null, new IndividualUser("user1"),
						"The limit parameter cannot be null"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						new CountLimit(null, 10), new IndividualUser("user1"),
						"A limit must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						new CountLimit("c1", 10), new IndividualUser(null),
						"A user or group of users must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						new CountLimit("c1", 10), null,
						"The user/group parameter cannot be null")
		);
	}

	static Stream<Arguments> deleteUsageLimit_invalidParameter() {
		return Stream.of(
				Arguments.of(null,
						"Maximum picture resolution", new IndividualUser("user1"),
						"The feature parameter cannot be null"),
				Arguments.of(new Feature(new Product(null), "f1"),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(null, "f1"),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a product with identifier"),
				Arguments.of(new Feature(new Product("p1"), null),
						"Maximum picture resolution", new IndividualUser("user1"),
						"A feature must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						null, new IndividualUser("user1"),
						"The 'limitId' parameter cannot be null"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						"Maximum picture resolution", new IndividualUser(null),
						"A user or group of users must have a identifier"),
				Arguments.of(new Feature(new Product("p1"), "f1"),
						"Maximum picture resolution", null,
						"The user/group parameter cannot be null")
		);
	}

	@BeforeEach
	void setUp() throws SQLException {
		this.dataSource = new JdbcDataSource();
		this.dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
		this.dataSource.setUser("sa");
		this.dataSource.setPassword("");

		this.createTable(this.dataSource);

		this.repository = new JDBCUserLimitRepository(this.dataSource, SCHEMA_NAME, TABLE_NAME);

		this.populateTable(this.dataSource);
	}

	@AfterEach
	void tearDown() throws Exception {
		String query = String.format("DROP TABLE IF EXISTS %s.%s;", SCHEMA_NAME, TABLE_NAME);
		try (Connection conn = this.dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute(query);
		}
	}

	@Test
	void findUsageLimit_CountLimitExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum picture size", this.user);

		assertThat(usageLimit).isPresent();
		assertThat(usageLimit.get()).isInstanceOf(CountLimit.class);
		CountLimit countLimit = (CountLimit) usageLimit.get();
		assertThat(countLimit.getId()).isEqualTo("Maximum picture size");
		assertThat(countLimit.getValue()).isEqualTo(10L);
		assertThat(countLimit.getUnit()).isEqualTo("Go");
	}

	@Test
	void findUsageLimit_CalendarPeriodRateLimitExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded in calendar month", this.user);

		assertThat(usageLimit).isPresent();
		assertThat(usageLimit.get()).isInstanceOf(CalendarPeriodRateLimit.class);
		CalendarPeriodRateLimit calendarPeriodRateLimit = (CalendarPeriodRateLimit) usageLimit.get();
		assertThat(calendarPeriodRateLimit.getId()).isEqualTo("Maximum number of picture uploaded in calendar month");
		assertThat(calendarPeriodRateLimit.getValue()).isEqualTo(1000L);
		assertThat(calendarPeriodRateLimit.getPeriodicity()).isEqualTo(CalendarPeriodRateLimit.Periodicity.MONTH);
	}

	@Test
	void findUsageLimit_usageLimitNotExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum picture resolution", this.user);

		assertThat(usageLimit).isNotPresent();
	}

	@Test
	void findUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		assertThatExceptionOfType(RepositoryException.class).as("The search should return an error").isThrownBy(
				() -> repositoryTest.findUsageLimit(this.feature, "Maximum picture resolution", this.user))
				.withMessage("Error finding limit")
				.withCauseInstanceOf(SQLException.class);
	}

	@Test
	void findUsageLimit_UnknownLimit() {

		UnknownLimit unknownLimit = new UnknownLimit("Unknown limit", 10L);

		this.repository.updateUsageLimit(this.feature, unknownLimit, this.user);

		assertThatExceptionOfType(RepositoryException.class).isThrownBy(
				() -> this.repository.findUsageLimit(this.feature, "Unknown limit", this.user))
				.havingRootCause()
				.withMessage("Unknown limit type: UnknownLimit");
	}

	@ParameterizedTest
	@MethodSource
	void findUsageLimit_invalidParameter(Feature feature, String limitId, UserGrouping userGrouping, String message) {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
				() -> this.repository.findUsageLimit(feature, limitId, userGrouping))
				.withMessage(message);
	}

	@Test
	void updateUsageLimit_CountLimitAdded() {

		String limitId = "Maximum picture resolution";
		long limitCount = 400;
		String limitUnit = "dpi";

		CountLimit countLimit = new CountLimit(limitId, limitCount);
		countLimit.setUnit(limitUnit);

		this.repository.updateUsageLimit(this.feature, countLimit, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum picture resolution", this.user);

		assertThat(userLimit).isPresent();
		assertThat(userLimit.get()).isInstanceOf(CountLimit.class);
		CountLimit countLimitDB = (CountLimit) userLimit.get();
		assertThat(countLimitDB.getId()).isEqualTo(limitId);
		assertThat(countLimitDB.getValue()).isEqualTo(limitCount);
		assertThat(countLimitDB.getUnit()).isEqualTo(limitUnit);
	}

	@Test
	void updateUsageLimit_CalendarPeriodRateLimitAdded() {

		String limitId = "Maximum number of picture uploaded in calendar week";
		long limitQuota = 40;
		CalendarPeriodRateLimit.Periodicity limitPeriodicity = CalendarPeriodRateLimit.Periodicity.WEEK;
		String limitUnit = "picture";

		CalendarPeriodRateLimit calendarPeriodRateLimit =
				new CalendarPeriodRateLimit(limitId, limitQuota, limitPeriodicity);
		calendarPeriodRateLimit.setUnit(limitUnit);

		this.repository.updateUsageLimit(this.feature, calendarPeriodRateLimit, this.user);

		Optional<UsageLimit> userLimit =
				this.repository.findUsageLimit(this.feature,
						"Maximum number of picture uploaded in calendar week", this.user);

		assertThat(userLimit).isPresent();
		assertThat(userLimit.get()).isInstanceOf(CalendarPeriodRateLimit.class);
		CalendarPeriodRateLimit calendarPeriodRateLimitDb = (CalendarPeriodRateLimit) userLimit.get();
		assertThat(calendarPeriodRateLimitDb.getId()).isEqualTo(limitId);
		assertThat(calendarPeriodRateLimitDb.getValue()).isEqualTo(limitQuota);
		assertThat(calendarPeriodRateLimitDb.getPeriodicity()).isEqualTo(limitPeriodicity);
		assertThat(calendarPeriodRateLimitDb.getUnit()).isEqualTo(limitUnit);
	}

	@Test
	void updateUsageLimit_CountLimitModified() {
		CountLimit countLimitToModified = new CountLimit("Maximum number of picture", 15);

		this.repository.updateUsageLimit(this.feature, countLimitToModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture", this.user);

		assertThat(userLimit).isPresent();
		assertThat(userLimit.get()).isInstanceOf(CountLimit.class);
		CountLimit countLimitDb = (CountLimit) userLimit.get();
		assertThat(countLimitDb.getId()).isEqualTo("Maximum number of picture");
		assertThat(countLimitDb.getValue()).isEqualTo(15);
	}

	@Test
	void updateUsageLimit_CalendarPeriodLimitModified() {
		CalendarPeriodRateLimit calendarPeriodRateLimitModified =
				new CalendarPeriodRateLimit("Maximum number of picture uploaded in calendar month",
						1500, CalendarPeriodRateLimit.Periodicity.MONTH);

		this.repository.updateUsageLimit(this.feature, calendarPeriodRateLimitModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded in calendar month", this.user);

		assertThat(userLimit).isPresent();
		assertThat(userLimit.get()).isInstanceOf(CalendarPeriodRateLimit.class);
		CalendarPeriodRateLimit calendarPeriodRateLimitDb = (CalendarPeriodRateLimit) userLimit.get();
		assertThat(calendarPeriodRateLimitDb.getId()).isEqualTo("Maximum number of picture uploaded in calendar month");
		assertThat(calendarPeriodRateLimitDb.getValue()).isEqualTo(1500);
	}

	@Test
	void updateUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		CountLimit countLimit = new CountLimit("Maximum picture resolution", 400);
		countLimit.setUnit("dpi");

		assertThatExceptionOfType(RepositoryException.class).as("The search should return an error").isThrownBy(
				() -> repositoryTest.updateUsageLimit(this.feature, countLimit, this.user))
				.withMessage("Error finding limit")
				.withCauseInstanceOf(SQLException.class);
	}

	@ParameterizedTest
	@MethodSource
	void updateUsageLimit_invalidParameter(Feature feature, UsageLimit usageLimit, UserGrouping userGrouping, String message) {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
				() -> this.repository.updateUsageLimit(feature, usageLimit, userGrouping))
				.withMessage(message);
	}

	@Test
	void deleteUsageLimit_CountLimitDeleted() {
		IndividualUser user1 = new IndividualUser("User2");

		CountLimit countLimit = new CountLimit("Maximum number of picture", 10);

		this.repository.updateUsageLimit(feature, countLimit, user1);

		this.repository.deleteUsageLimit(feature, "Maximum number of picture", user1);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(feature, "Maximum number of picture", user1);

		assertThat(userLimit).isEmpty();
	}

	@Test
	void deleteUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		assertThatExceptionOfType(RepositoryException.class).as("The search should return an error").isThrownBy(
				() -> repositoryTest.deleteUsageLimit(this.feature, "Maximum picture resolution", this.user))
				.withMessage("Error deleting limit")
				.withCauseInstanceOf(SQLException.class);
	}

	@ParameterizedTest
	@MethodSource
	void deleteUsageLimit_invalidParameter(Feature feature, String limitId, UserGrouping userGrouping, String message) {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
				() -> this.repository.deleteUsageLimit(feature, limitId, userGrouping))
				.withMessage(message);
	}

	private void createTable(DataSource dataSource) throws SQLException {

		String createQuery = String.format(
				"""
						CREATE TABLE %s.%s (
							usage_id integer AUTO_INCREMENT,
							limit_id varchar NOT NULL,
							feature_id varchar NOT NULL,
							user_group_id varchar NOT NULL,
							limit_type varchar NOT NULL,
							limit_value integer,
							limit_unit varchar,
							limit_interval varchar,
							limit_duration integer,
							PRIMARY KEY (usage_id)
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

	private void populateTable(DataSource dataSource) throws SQLException {

		CountLimit countLimit = new CountLimit("Maximum picture size", 10);
		countLimit.setUnit("Go");
		insertUserUsageLimitRecord(dataSource, this.feature, this.user, countLimit);

		countLimit = new CountLimit("Maximum number of picture", 50);
		insertUserUsageLimitRecord(dataSource, this.feature, this.user, countLimit);

		CalendarPeriodRateLimit calendarPeriodRateLimit = new CalendarPeriodRateLimit(
				"Maximum number of picture uploaded in calendar month", 1000, CalendarPeriodRateLimit.Periodicity.MONTH
		);
		insertUserUsageLimitRecord(dataSource, this.feature, this.user, calendarPeriodRateLimit);
	}

	private void insertUserUsageLimitRecord(DataSource dataSource, Feature feature, UserGrouping userGroup,
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
