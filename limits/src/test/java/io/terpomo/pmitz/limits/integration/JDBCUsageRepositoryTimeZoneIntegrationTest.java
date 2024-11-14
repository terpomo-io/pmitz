package io.terpomo.pmitz.limits.integration;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRecordRepoMetadata;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JDBCUsageRepositoryTimeZoneIntegrationTest {

	private static final String CUSTOM_SCHEMA = "test_schema";
	private static final String TABLE_NAME = "Usage";

	// Define the containers with @Container annotation
	@Container
	private final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.30")
			.withDatabaseName("testdb")
			.withUsername("root")
			.withPassword("root")
			.waitingFor(Wait.forListeningPort()) // Ensures the port is ready
			.withStartupTimeout(Duration.ofMinutes(2)); // Increases startup timeout if needed;

	@Container
	private final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test")
			.waitingFor(Wait.forListeningPort()) // Ensures the port is ready
			.withStartupTimeout(Duration.ofMinutes(2)); // Increases startup timeout if needed;

	@Container
	private final MSSQLServerContainer<?> sqlServerContainer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-latest")
			.acceptLicense()
			.withPassword("yourStrong(!)Password")
			.waitingFor(Wait.forListeningPort()) // Ensures the port is ready
			.withStartupTimeout(Duration.ofMinutes(2)); // Increases startup timeout if needed

	// Provide a stream of database containers to the parameterized tests
	private Stream<Arguments> databaseContainers() {
		return Stream.of(
				Arguments.of("mysql", mysqlContainer),
				Arguments.of("postgres", postgresContainer),
				Arguments.of("sqlserver", sqlServerContainer)
		);
	}

	// Helper method to create tables based on the database type
	private void createTable(String databaseType, BasicDataSource dataSource) {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			switch (databaseType) {
				case "mysql":
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
							"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
							");");
					break;
				case "postgres":
					stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
					stmt.execute("CREATE TABLE IF NOT EXISTS " + CUSTOM_SCHEMA + "." + TABLE_NAME + " (" +
							"usage_id SERIAL PRIMARY KEY, " +
							"feature_id VARCHAR(255), " +
							"product_id VARCHAR(255), " +
							"user_grouping VARCHAR(255), " +
							"limit_id VARCHAR(255), " +
							"window_start TIMESTAMP WITH TIME ZONE, " +
							"window_end TIMESTAMP WITH TIME ZONE, " +
							"units INT, " +
							"expiration_date TIMESTAMP WITH TIME ZONE, " +
							"updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP" +
							");");
					break;
				case "sqlserver":
					stmt.execute("IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = '" + CUSTOM_SCHEMA + "') " +
							"EXEC('CREATE SCHEMA " + CUSTOM_SCHEMA + "');");
					stmt.execute("CREATE TABLE " + CUSTOM_SCHEMA + ".[" + TABLE_NAME + "] (" +
							"usage_id INT IDENTITY(1,1) PRIMARY KEY, " +
							"feature_id NVARCHAR(255), " +
							"product_id NVARCHAR(255), " +
							"user_grouping NVARCHAR(255), " +
							"limit_id NVARCHAR(255), " +
							"window_start DATETIME2, " +
							"window_end DATETIME2, " +
							"units INT, " +
							"expiration_date DATETIME2, " +
							"updated_at DATETIME2 DEFAULT SYSUTCDATETIME()" +
							");");
					break;
				default:
					throw new IllegalArgumentException("Unsupported database type: " + databaseType);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to create table for " + databaseType, e);
		}
	}

	// Helper method to truncate the table after each test
	private void truncateTable(String databaseType, BasicDataSource dataSource) throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			switch (databaseType) {
				case "mysql":
					stmt.execute("SET FOREIGN_KEY_CHECKS=0");
					stmt.execute("TRUNCATE TABLE " + CUSTOM_SCHEMA + "." + TABLE_NAME);
					stmt.execute("SET FOREIGN_KEY_CHECKS=1");
					break;
				case "postgres":
					stmt.execute("TRUNCATE TABLE " + CUSTOM_SCHEMA + "." + TABLE_NAME + " RESTART IDENTITY CASCADE");
					break;
				case "sqlserver":
					stmt.execute("TRUNCATE TABLE " + CUSTOM_SCHEMA + "." + TABLE_NAME);
					break;
				default:
					throw new IllegalArgumentException("Unsupported database type: " + databaseType);
			}
		}
	}


	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testTimezoneNeutrality(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException, ZoneRulesException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);


		// Insert test data with different timezones
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("UTC"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")));
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 1, 1, 7, 0, 0, 0, ZoneId.of("America/New_York")));
		timezones.put(ZoneId.of("Europe/London"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")));
		timezones.put(ZoneId.of("Australia/Sydney"), ZonedDateTime.of(2022, 1, 2, 11, 0, 0, 0, ZoneId.of("Australia/Sydney")));

		// Insert test data into the database
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature-id");
				pstmt.setString(2, "product-id");
				pstmt.setString(3, "user-grouping");
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Test timezone neutrality by retrieving data in UTC
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are in UTC when inserted, so they should be equal when retrieved
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);

				// Additionally, verify that the retrieved timestamps are in UTC
				ZonedDateTime utcWindowStart = windowStart.toInstant().atZone(ZoneId.of("UTC"));
				ZonedDateTime utcWindowEnd = windowEnd.toInstant().atZone(ZoneId.of("UTC"));
				ZonedDateTime utcExpirationDate = expirationDate.toInstant().atZone(ZoneId.of("UTC"));

				assertEquals(utcWindowStart, utcWindowEnd);
				assertEquals(utcWindowStart, utcExpirationDate);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	private void testTimezoneNeutralityInternal(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException, ZoneRulesException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Test data with different timezones
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("UTC"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")));
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 1, 1, 7, 0, 0, 0, ZoneId.of("America/New_York")));
		timezones.put(ZoneId.of("Europe/London"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")));
		timezones.put(ZoneId.of("Australia/Sydney"), ZonedDateTime.of(2022, 1, 2, 11, 0, 0, 0, ZoneId.of("Australia/Sydney")));

		// Insert test data into the database
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature-id");
				pstmt.setString(2, "product-id");
				pstmt.setString(3, "user-grouping");
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Test timezone neutrality by retrieving data in UTC
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are equal (i.e., timezone-neutral)
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);
			}
		}
	}

	// Example of another parameterized test
	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testLoadUsageData(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);


		// Insert test data with different timezones
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("UTC"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")));
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 1, 1, 7, 0, 0, 0, ZoneId.of("America/New_York")));
		timezones.put(ZoneId.of("Europe/London"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")));
		timezones.put(ZoneId.of("Australia/Sydney"), ZonedDateTime.of(2022, 1, 2, 11, 0, 0, 0, ZoneId.of("Australia/Sydney")));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature1"); // Hardcoded feature name
				pstmt.setString(2, "product1"); // Hardcoded product name
				pstmt.setString(3, userGrouping.getId());
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit-id", ZonedDateTime.now().minusHours(1), ZonedDateTime.now().plusHours(1));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		// Load usage data using the repository
		repository.loadUsageData(context);

		// Verify that the loaded data has consistent timezone offsets
		for (UsageRecord record : context.getCurrentUsageRecords()) {
			ZoneOffset startOffset = record.startTime().getOffset();
			assertEquals(startOffset, record.endTime().getOffset());
			assertEquals(startOffset, record.expirationDate().getOffset());
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testUpdateUsageRecords(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource using the already started container
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Insert test data
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("UTC"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")));
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 1, 1, 7, 0, 0, 0, ZoneId.of("America/New_York")));
		timezones.put(ZoneId.of("Europe/London"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")));
		timezones.put(ZoneId.of("Australia/Sydney"), ZonedDateTime.of(2022, 1, 2, 11, 0, 0, 0, ZoneId.of("Australia/Sydney")));

		Product product = new Product("product2");
		Feature feature = new Feature(product, "feature2");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user2";
			}
		};


		// Insert records and collect the start times
		List<ZonedDateTime> startTimes = new ArrayList<>();
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			startTimes.add(zdt.withZoneSameInstant(ZoneOffset.UTC));

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature2"); // Hardcoded feature name
				pstmt.setString(2, "product2"); // Hardcoded product name
				pstmt.setString(3, userGrouping.getId());
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Create a LimitTrackingContext instance
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit-id",
				ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
				ZonedDateTime.of(2022, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC")));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		// Retrieve the inserted records
		repository.loadUsageData(context);

		// Update the records
		for (UsageRecord record : context.getCurrentUsageRecords()) {
			JDBCUsageRecordRepoMetadata updatedMetadata = (JDBCUsageRecordRepoMetadata) record.repoMetadata();
			UsageRecord updatedRecord = new UsageRecord(
					updatedMetadata,
					record.limitId(),         // Use existing limitId
					record.startTime(),       // Use existing startTime
					record.endTime(),         // Keep existing or update as needed
					110L,                     // Update units to 110
					ZonedDateTime.of(2022, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC"))
			);
			context.getUpdatedUsageRecords().add(updatedRecord);
		}

		// Update usage records using the repository
		repository.updateUsageRecords(context);

		// Verify that the updated data is correct
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT units, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME + " WHERE limit_id = 'limit-id'")) {

			while (rs.next()) {
				assertEquals(110L, rs.getLong("units"));
				assertEquals(Timestamp.from(ZonedDateTime.of(2022, 1, 3, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant()), rs.getTimestamp("expiration_date"));
			}
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testDSTTransition(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException, ZoneRulesException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Test Case 3: Edge Case - Daylight Saving Time (DST) Transition
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 3, 13, 1, 59, 0, 0, ZoneId.of("America/New_York"))); // Before DST transition
		timezones.put(ZoneId.of("America/New_York"), ZonedDateTime.of(2022, 3, 13, 3, 0, 0, 0, ZoneId.of("America/New_York"))); // After DST transition

		// Insert test data into the database
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature-id");
				pstmt.setString(2, "product-id");
				pstmt.setString(3, "user-grouping");
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Verify that the inserted data is correct despite DST transition
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are in UTC when inserted, so they should be equal when retrieved
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testNullTimezoneInfo(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Test Case 4: Null or Absent Timezone Information
		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

			pstmt.setString(1, "feature-id");
			pstmt.setString(2, "product-id");
			pstmt.setString(3, "user-grouping");
			pstmt.setString(4, "limit-id");
			pstmt.setNull(5, Types.TIMESTAMP); // Null timestamp
			pstmt.setNull(6, Types.TIMESTAMP); // Null timestamp
			pstmt.setInt(7, 100);
			pstmt.setNull(8, Types.TIMESTAMP); // Null timestamp
			pstmt.executeUpdate();
		}

		// Verify that the insert operation with null timezone info does not throw any exceptions
		// You may want to add additional assertions based on your application's requirements
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testDeleteTimezoneSensitive(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Test Case 8: Deletion Based on Timezone-Sensitive Criteria
		ZonedDateTime zdt = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
		Instant instant = zdt.toInstant();
		Timestamp timestamp = Timestamp.from(instant);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

			pstmt.setString(1, "feature-id");
			pstmt.setString(2, "product-id");
			pstmt.setString(3, "user-grouping");
			pstmt.setString(4, "limit-id");
			pstmt.setTimestamp(5, timestamp);
			pstmt.setTimestamp(6, timestamp);
			pstmt.setInt(7, 100);
			pstmt.setTimestamp(8, timestamp);
			pstmt.executeUpdate();
		}

		// Delete records based on timezone-sensitive criteria
		repository.deleteOldRecords(zdt);

		// Verify that the deletion operation was successful
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			// Assert that no records are found
			assertFalse(rs.next());
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testSystemTimezoneChange(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException, IOException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Save the original system timezone
		String originalTimezone = System.getProperty("user.timezone");

		// Temporarily change the system timezone to "America/New_York"
		System.setProperty("user.timezone", "America/New_York");

		ZonedDateTime zdt = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
		Instant instant = zdt.toInstant();
		Timestamp timestamp = Timestamp.from(instant);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

			pstmt.setString(1, "feature-id");
			pstmt.setString(2, "product-id");
			pstmt.setString(3, "user-grouping");
			pstmt.setString(4, "limit-id");
			pstmt.setTimestamp(5, timestamp);
			pstmt.setTimestamp(6, timestamp);
			pstmt.setInt(7, 100);
			pstmt.setTimestamp(8, timestamp);
			pstmt.executeUpdate();
		}

		// Verify that the insert operation works correctly despite the system timezone change
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are in UTC when inserted, so they should be equal when retrieved
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);
			}
		}

		// Restore the original system timezone
		System.setProperty("user.timezone", originalTimezone);
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testZeroOffsetNonUTCTimezone(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Test Case 6: Timezone with Zero Offset (but not UTC)
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("Africa/Dakar"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Africa/Dakar"))); // +0 offset, not UTC

		// Insert test data into the database
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature-id");
				pstmt.setString(2, "product-id");
				pstmt.setString(3, "user-grouping");
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Verify that the inserted data is correct despite zero offset non-UTC timezone
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are in UTC when inserted, so they should be equal when retrieved
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testExtremeTimezoneOffsets(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		// Setup DataSource
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Test Case 5: Extreme Timezone Offsets
		Map<ZoneId, ZonedDateTime> timezones = new HashMap<>();
		timezones.put(ZoneId.of("Pacific/Kiritimati"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Pacific/Kiritimati"))); // +14 offset
		timezones.put(ZoneId.of("Etc/GMT+12"), ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Etc/GMT+12"))); // -12 offset

		// Insert test data into the database
		for (Map.Entry<ZoneId, ZonedDateTime> entry : timezones.entrySet()) {
			ZonedDateTime zdt = entry.getValue();
			Instant instant = zdt.toInstant();
			Timestamp timestamp = Timestamp.from(instant);

			try (Connection conn = dataSource.getConnection();
					PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
							" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

				pstmt.setString(1, "feature-id");
				pstmt.setString(2, "product-id");
				pstmt.setString(3, "user-grouping");
				pstmt.setString(4, "limit-id");
				pstmt.setTimestamp(5, timestamp);
				pstmt.setTimestamp(6, timestamp);
				pstmt.setInt(7, 100);
				pstmt.setTimestamp(8, timestamp);
				pstmt.executeUpdate();
			}
		}

		// Verify that the inserted data is correct despite extreme timezone offsets
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME)) {

			int count = 0;
			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are in UTC when inserted, so they should be equal when retrieved
				assertEquals(windowStart, windowEnd);
				assertEquals(windowStart, expirationDate);

				// Additionally, verify that the retrieved timestamps are in UTC
				ZonedDateTime utcWindowStart = windowStart.toInstant().atZone(ZoneId.of("UTC"));
				ZonedDateTime utcWindowEnd = windowEnd.toInstant().atZone(ZoneId.of("UTC"));
				ZonedDateTime utcExpirationDate = expirationDate.toInstant().atZone(ZoneId.of("UTC"));

				assertEquals(utcWindowStart, utcWindowEnd);
				assertEquals(utcWindowStart, utcExpirationDate);

				count++;
			}

			// Assert that both extreme timezone offset records were retrieved
			assertEquals(2, count);
		}
	}
	@ParameterizedTest
	@MethodSource("databaseContainers")
	public void testUpdateTimezoneAspects(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		// Create table
		createTable(databaseType, dataSource);

		// Initialize repository
		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		// Test Case 7: Update Operations Across Timezones
		ZonedDateTime zdt = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
		Instant instant = zdt.toInstant();
		Timestamp timestamp = Timestamp.from(instant);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?,?,?,?,?,?,?,?)")) {

			pstmt.setString(1, "feature-id");
			pstmt.setString(2, "product-id");
			pstmt.setString(3, "user-grouping");
			pstmt.setString(4, "limit-id");
			pstmt.setTimestamp(5, timestamp);
			pstmt.setTimestamp(6, timestamp);
			pstmt.setInt(7, 100);
			pstmt.setTimestamp(8, timestamp);
			pstmt.executeUpdate();
		}

		// Update the record with a new timezone
		ZonedDateTime newZdt = ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York"));
		Instant newInstant = newZdt.toInstant();
		Timestamp newTimestamp = Timestamp.from(newInstant);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement pstmt = conn.prepareStatement("UPDATE " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" SET window_start =?, window_end =?, expiration_date =? WHERE limit_id = 'limit-id'")) {

			pstmt.setTimestamp(1, newTimestamp);
			pstmt.setTimestamp(2, newTimestamp);
			pstmt.setTimestamp(3, newTimestamp);
			pstmt.executeUpdate();
		}

		// Verify that the update operation was successful across timezones
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT window_start, window_end, expiration_date FROM " + CUSTOM_SCHEMA + "." + TABLE_NAME +
						" WHERE limit_id = 'limit-id'")) {

			while (rs.next()) {
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				// Verify that all timestamps are updated correctly across timezones
				assertEquals(newTimestamp, windowStart);
				assertEquals(newTimestamp, windowEnd);
				assertEquals(newTimestamp, expirationDate);
			}
		}
	}
}