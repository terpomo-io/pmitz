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

package io.terpomo.pmitz.core.repository.userlimit.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.core.repository.userlimit.UserLimitRepository;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class JDBCUserLimitRepositoryTests {

	private static final String SCHEMA_NAME = "public";
	private static final String TABLE_NAME = "user_usage_limit";

	private final Product product = new Product("Picture hosting service");
	private final Feature feature = new Feature(this.product, "Uploading pictures");
	private final UserGrouping user = new IndividualUser("User1");

	private UserLimitRepository repository;
	private JdbcDataSource dataSource;


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

		assertTrue(usageLimit.isPresent());
		CountLimit countLimit = assertInstanceOf(CountLimit.class, usageLimit.get());
		assertEquals("Maximum picture size", countLimit.getId());
		assertEquals(10L, countLimit.getValue());
		assertEquals("Go", countLimit.getUnit());
	}

	@Test
	void findUsageLimit_CalendarPeriodRateLimitExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded in calendar month", this.user);

		assertTrue(usageLimit.isPresent());
		CalendarPeriodRateLimit calendarPeriodRateLimit = assertInstanceOf(CalendarPeriodRateLimit.class, usageLimit.get());
		assertEquals("Maximum number of picture uploaded in calendar month", calendarPeriodRateLimit.getId());
		assertEquals(1000L, calendarPeriodRateLimit.getValue());
		assertEquals(CalendarPeriodRateLimit.Periodicity.MONTH, calendarPeriodRateLimit.getPeriodicity());
	}

	@Test
	void findUsageLimit_SlidingWindowRateLimitExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded by day", this.user);

		assertTrue(usageLimit.isPresent());
		SlidingWindowRateLimit slidingWindowRateLimit = assertInstanceOf(SlidingWindowRateLimit.class, usageLimit.get());
		assertEquals("Maximum number of picture uploaded by day", slidingWindowRateLimit.getId());
		assertEquals(15L, slidingWindowRateLimit.getValue());
		assertEquals(ChronoUnit.DAYS, slidingWindowRateLimit.getInterval());
		assertEquals(1, slidingWindowRateLimit.getDuration());
	}

	@Test
	void findUsageLimit_usageLimitNotExist() {

		Optional<UsageLimit> usageLimit = this.repository.findUsageLimit(this.feature,
				"Maximum picture resolution", this.user);

		assertFalse(usageLimit.isPresent());
	}

	@Test
	void findUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		RepositoryException exception = assertThrows(
				RepositoryException.class,
				() -> repositoryTest.findUsageLimit(this.feature, "Maximum picture resolution", this.user),
				"The search should return an error"
		);

		assertEquals("Error finding limit", exception.getMessage());
		assertNotNull(exception.getCause());
		assertInstanceOf(SQLException.class, exception.getCause());
	}

	@Test
	void findUsageLimit_UnknownLimit() {

		UnknownLimit unknownLimit = new UnknownLimit("Unknown limit", 10L);

		this.repository.addUsageLimit(this.feature, unknownLimit, this.user);

		RepositoryException exception = assertThrows(
				RepositoryException.class,
				() -> this.repository.findUsageLimit(this.feature, "Unknown limit", this.user)
		);

		assertNotNull(exception.getCause());
		assertEquals("Unknown limit type: UnknownLimit", exception.getCause().getMessage());
	}

	@ParameterizedTest
	@MethodSource
	void findUsageLimit_invalidParameter(Feature feature, String limitId, UserGrouping userGrouping, String message) {

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> this.repository.findUsageLimit(feature, limitId, userGrouping)
		);

		assertEquals(message, exception.getMessage());
	}

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

	@Test
	void addUsageLimit_CountLimitAdded() {

		String limitId = "Maximum picture resolution";
		long limitCount = 400;
		String limitUnit = "dpi";

		CountLimit countLimit = new CountLimit(limitId, limitCount);
		countLimit.setUnit(limitUnit);

		this.repository.addUsageLimit(this.feature, countLimit, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum picture resolution", this.user);

		assertTrue(userLimit.isPresent());
		assertInstanceOf(CountLimit.class, userLimit.get());
		CountLimit countLimitDB = (CountLimit) userLimit.get();
		assertEquals(limitId, countLimitDB.getId());
		assertEquals(limitCount, countLimitDB.getValue());
		assertEquals(limitUnit, countLimitDB.getUnit());
	}

	@Test
	void addUsageLimit_CalendarPeriodRateLimitAdded() {

		String limitId = "Maximum number of picture uploaded in calendar week";
		long limitQuota = 40;
		CalendarPeriodRateLimit.Periodicity limitPeriodicity = CalendarPeriodRateLimit.Periodicity.WEEK;
		String limitUnit = "picture";

		CalendarPeriodRateLimit calendarPeriodRateLimit =
				new CalendarPeriodRateLimit(limitId, limitQuota, limitPeriodicity);
		calendarPeriodRateLimit.setUnit(limitUnit);

		this.repository.addUsageLimit(this.feature, calendarPeriodRateLimit, this.user);

		Optional<UsageLimit> userLimit =
				this.repository.findUsageLimit(this.feature,
						"Maximum number of picture uploaded in calendar week", this.user);

		assertTrue(userLimit.isPresent());
		CalendarPeriodRateLimit calendarPeriodRateLimitDb =
				assertInstanceOf(CalendarPeriodRateLimit.class, userLimit.get());
		assertEquals(limitId, calendarPeriodRateLimitDb.getId());
		assertEquals(limitQuota, calendarPeriodRateLimitDb.getValue());
		assertEquals(limitPeriodicity, calendarPeriodRateLimitDb.getPeriodicity());
		assertEquals(limitUnit, calendarPeriodRateLimitDb.getUnit());
	}

	@Test
	void addUsageLimit_SlidingWindowRateLimitAdded() {

		String limitId = "Maximum number of picture uploaded by year";
		long limitQuota = 1500;
		ChronoUnit limitInterval = ChronoUnit.YEARS;
		String limitUnit = "picture";
		int limitDuration = 1;

		SlidingWindowRateLimit slidingWindowRateLimit =
				new SlidingWindowRateLimit(limitId, limitQuota, limitInterval, limitDuration);
		slidingWindowRateLimit.setUnit(limitUnit);

		this.repository.addUsageLimit(this.feature, slidingWindowRateLimit, this.user);

		Optional<UsageLimit> userLimit =
				this.repository.findUsageLimit(this.feature,
						"Maximum number of picture uploaded by year", this.user);

		assertTrue(userLimit.isPresent());
		SlidingWindowRateLimit slidingWindowRateLimitDb =
				assertInstanceOf(SlidingWindowRateLimit.class, userLimit.get());
		assertEquals(limitId, slidingWindowRateLimitDb.getId());
		assertEquals(limitQuota, slidingWindowRateLimitDb.getValue());
		assertEquals(limitInterval, slidingWindowRateLimitDb.getInterval());
		assertEquals(limitUnit, slidingWindowRateLimitDb.getUnit());
		assertEquals(limitDuration, slidingWindowRateLimitDb.getDuration());
	}

	@Test
	void addUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		CountLimit countLimit = new CountLimit("Maximum picture resolution", 400);
		countLimit.setUnit("dpi");

		RepositoryException exception = assertThrows(
				RepositoryException.class,
				() -> repositoryTest.addUsageLimit(this.feature, countLimit, this.user),
				"The search should return an error"
		);

		assertEquals("Error adding limit", exception.getMessage());
		assertNotNull(exception.getCause());
		assertInstanceOf(SQLException.class, exception.getCause());
	}

	@ParameterizedTest
	@MethodSource
	void addUsageLimit_invalidParameter(Feature feature, UsageLimit usageLimit, UserGrouping userGrouping, String message) {

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> this.repository.addUsageLimit(feature, usageLimit, userGrouping)
		);

		assertEquals(message, exception.getMessage());
	}

	static Stream<Arguments> addUsageLimit_invalidParameter() {
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

	@Test
	void updateUsageLimit_CountLimitModified() {
		CountLimit countLimitToModified = new CountLimit("Maximum number of picture", 15);

		this.repository.updateUsageLimit(this.feature, countLimitToModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture", this.user);

		assertTrue(userLimit.isPresent());
		CountLimit countLimitDb = assertInstanceOf(CountLimit.class, userLimit.get());
		assertEquals("Maximum number of picture", countLimitDb.getId());
		assertEquals(15, countLimitDb.getValue());
	}

	@Test
	void updateUsageLimit_CalendarPeriodLimitModified() {
		CalendarPeriodRateLimit calendarPeriodRateLimitModified =
				new CalendarPeriodRateLimit("Maximum number of picture uploaded in calendar month",
						1500, CalendarPeriodRateLimit.Periodicity.MONTH);

		this.repository.updateUsageLimit(this.feature, calendarPeriodRateLimitModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded in calendar month", this.user);

		assertTrue(userLimit.isPresent());
		CalendarPeriodRateLimit calendarPeriodRateLimitDb =
				assertInstanceOf(CalendarPeriodRateLimit.class, userLimit.get());
		assertEquals("Maximum number of picture uploaded in calendar month", calendarPeriodRateLimitDb.getId());
		assertEquals(1500, calendarPeriodRateLimitDb.getValue());
	}

	@Test
	void updateUsageLimit_SlidingWindowRateLimitModified() {
		SlidingWindowRateLimit slidingWindowRateLimitModified =
				new SlidingWindowRateLimit("Maximum number of picture uploaded by day",
						5, ChronoUnit.DAYS, 1);

		this.repository.updateUsageLimit(this.feature, slidingWindowRateLimitModified, this.user);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(this.feature,
				"Maximum number of picture uploaded by day", this.user);

		assertTrue(userLimit.isPresent());
		SlidingWindowRateLimit slidingWindowRateLimitDb =
				assertInstanceOf(SlidingWindowRateLimit.class, userLimit.get());
		assertEquals("Maximum number of picture uploaded by day", slidingWindowRateLimitDb.getId());
		assertEquals(5, slidingWindowRateLimitDb.getValue());
		assertEquals(1, slidingWindowRateLimitDb.getDuration());
	}

	@Test
	void updateUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		CountLimit countLimit = new CountLimit("Maximum picture resolution", 400);
		countLimit.setUnit("dpi");

		RepositoryException exception = assertThrows(
				RepositoryException.class,
				() -> repositoryTest.updateUsageLimit(this.feature, countLimit, this.user),
				"The search should return an error"
		);

		assertEquals("Error updating limit", exception.getMessage());
		assertNotNull(exception.getCause());
		assertInstanceOf(SQLException.class, exception.getCause());
	}

	@ParameterizedTest
	@MethodSource
	void updateUsageLimit_invalidParameter(Feature feature, UsageLimit usageLimit, UserGrouping userGrouping, String message) {

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> this.repository.updateUsageLimit(feature, usageLimit, userGrouping)
		);

		assertEquals(message, exception.getMessage());
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

	@Test
	void deleteUsageLimit_CountLimitDeleted() {
		Product product = new Product("Picture hosting service");
		Feature feature = new Feature(product, "Uploading pictures");

		IndividualUser user1 = new IndividualUser("User2");

		CountLimit countLimit = new CountLimit("Maximum number of picture", 10);

		this.repository.addUsageLimit(feature, countLimit, user1);

		this.repository.deleteUsageLimit(feature, "Maximum number of picture", user1);

		Optional<UsageLimit> userLimit = this.repository.findUsageLimit(feature, "Maximum number of picture", user1);

		assertTrue(userLimit.isEmpty());
	}

	@Test
	void deleteUsageLimit_SQLException() {

		UserLimitRepository repositoryTest = new JDBCUserLimitRepository(new JdbcDataSource(), SCHEMA_NAME, "abc");

		RepositoryException exception = assertThrows(
				RepositoryException.class,
				() -> repositoryTest.deleteUsageLimit(this.feature, "Maximum picture resolution", this.user),
				"The search should return an error"
		);

		assertEquals("Error deleting limit", exception.getMessage());
		assertNotNull(exception.getCause());
		assertInstanceOf(SQLException.class, exception.getCause());
	}

	@ParameterizedTest
	@MethodSource
	void deleteUsageLimit_invalidParameter(Feature feature, String limitId, UserGrouping userGrouping, String message) {

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> this.repository.deleteUsageLimit(feature, limitId, userGrouping)
		);

		assertEquals(message, exception.getMessage());
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

		SlidingWindowRateLimit slidingWindowRateLimit = new SlidingWindowRateLimit(
				"Maximum number of picture uploaded by day", 15, ChronoUnit.DAYS, 1
		);
		insertUserUsageLimitRecord(dataSource, this.feature, this.user, slidingWindowRateLimit);
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

	// TODO: Remove
	private void printDatabaseContents(DataSource dataSource) {
		try {
			try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
				System.out.println("=========== UsageLimit ==========");
				ResultSet rs = stmt.executeQuery("SELECT * FROM public.user_usage_limit");
				while (rs.next()) {
					long usageId = rs.getLong("usage_id");
					String limitId = rs.getString("limit_id");
					String featureId = rs.getString("feature_id");
					String userGroup = rs.getString("user_group_id");
					String limitType = rs.getString("limit_type");
					long limitValue = rs.getLong("limit_value");
					String limitUnit = rs.getString("limit_unit");
					String limitInterval = rs.getString("limit_interval");
					int limitDuration = rs.getInt("limit_duration");

					System.out.printf(
							"""
							usage_id=%s, limitId=%s, featureId=%s, userGroup=%s, limitType=%s,
							limitValue=%d, limitUnit=%s, limitInterval=%s, limitDuration=%d%n
							""",
							usageId, limitId, featureId, userGroup, limitType,
							limitValue, limitUnit, limitInterval, limitDuration
					);
				}
			}
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}
