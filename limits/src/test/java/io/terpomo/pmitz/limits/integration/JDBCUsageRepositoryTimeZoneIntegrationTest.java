/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy at https://www.apache.org/licenses/LICENSE-2.0
 */

package io.terpomo.pmitz.limits.integration;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
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

import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JDBCUsageRepositoryTimeZoneIntegrationTest {

	private static final String CUSTOM_SCHEMA = "test_schema";
	private static final String TABLE_NAME = "Usage";
	private static final String LIMIT_ID = "limit-id";
	private static final String FEATURE_ID = "feature1";
	private static final String PRODUCT_ID = "product1";
	private static final String USER_ID = "user1";
	private static final long UNITS = 100L;

	@Container
	private final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.30")
			.withDatabaseName("testdb").withUsername("root").withPassword("root")
			.waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofMinutes(2));

	@Container
	private final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:latest")
			.withDatabaseName("testdb").withUsername("test").withPassword("test")
			.waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofMinutes(2));

	@Container
	private final MSSQLServerContainer<?> sqlServerContainer = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-latest")
			.acceptLicense().withPassword("yourStrong(!)Password")
			.waitingFor(Wait.forListeningPort()).withStartupTimeout(Duration.ofMinutes(2));

	private Stream<Arguments> databaseContainers() {
		return Stream.of(
				Arguments.of("mysql", mysqlContainer),
				Arguments.of("postgres", postgresContainer),
				Arguments.of("sqlserver", sqlServerContainer)
		);
	}

	private void createTable(String databaseType, BasicDataSource dataSource) throws SQLException {
		String createTableQuery;
		String schemaPrefix;
		try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
			switch (databaseType) {
				case "mysql" -> {
					stmt.execute("CREATE SCHEMA IF NOT EXISTS `" + CUSTOM_SCHEMA + "`");
					schemaPrefix = "`" + CUSTOM_SCHEMA + "`.";
					createTableQuery = "CREATE TABLE IF NOT EXISTS " + schemaPrefix + "`Usage` (" +
							"usage_id INT AUTO_INCREMENT PRIMARY KEY, feature_id VARCHAR(255), product_id VARCHAR(255), " +
							"user_grouping VARCHAR(255), limit_id VARCHAR(255), window_start TIMESTAMP, " +
							"window_end TIMESTAMP, units INT, expiration_date TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
				}
				case "postgres" -> {
					stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
					schemaPrefix = CUSTOM_SCHEMA + ".";
					createTableQuery = "CREATE TABLE IF NOT EXISTS " + schemaPrefix + TABLE_NAME + " (" +
							"usage_id SERIAL PRIMARY KEY, feature_id VARCHAR(255), product_id VARCHAR(255), " +
							"user_grouping VARCHAR(255), limit_id VARCHAR(255), window_start TIMESTAMP WITH TIME ZONE, " +
							"window_end TIMESTAMP WITH TIME ZONE, units INT, expiration_date TIMESTAMP WITH TIME ZONE, " +
							"updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)";
				}
				case "sqlserver" -> {
					stmt.execute("IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = '" + CUSTOM_SCHEMA + "') " +
							"EXEC('CREATE SCHEMA " + CUSTOM_SCHEMA + "');");
					schemaPrefix = CUSTOM_SCHEMA + ".";
					createTableQuery = "CREATE TABLE " + schemaPrefix + "[" + TABLE_NAME + "] (" +
							"usage_id INT IDENTITY(1,1) PRIMARY KEY, feature_id NVARCHAR(255), " +
							"product_id NVARCHAR(255), user_grouping NVARCHAR(255), limit_id NVARCHAR(255), " +
							"window_start DATETIME2, window_end DATETIME2, units INT, expiration_date DATETIME2, updated_at DATETIME2 DEFAULT SYSUTCDATETIME())";
				}
				default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
			}
			stmt.execute(createTableQuery);
		}
	}

	private void insertRecords(Connection connection, String databaseType, List<Timestamp> timestamps) throws SQLException {
		String insertQuery = "INSERT INTO %s (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		String qualifiedTableName = switch (databaseType) {
			case "mysql" -> String.format("`%s`.`%s`", CUSTOM_SCHEMA, TABLE_NAME);
			case "postgres" -> String.format("%s.%s", CUSTOM_SCHEMA, TABLE_NAME);
			case "sqlserver" -> String.format("%s.[%s]", CUSTOM_SCHEMA, TABLE_NAME);
			default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
		};
		insertQuery = String.format(insertQuery, qualifiedTableName);

		try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
			for (Timestamp ts : timestamps) {
				statement.setString(1, FEATURE_ID);
				statement.setString(2, PRODUCT_ID);
				statement.setString(3, USER_ID);
				statement.setString(4, LIMIT_ID);
				statement.setTimestamp(5, ts);
				statement.setTimestamp(6, ts);
				statement.setLong(7, UNITS);
				statement.setTimestamp(8, ts);

				if (databaseType.equals("sqlserver")) {
					statement.setNString(1, FEATURE_ID);
					statement.setNString(2, PRODUCT_ID);
					statement.setNString(3, USER_ID);
					statement.setNString(4, LIMIT_ID);
				}

				statement.executeUpdate();
			}
		}
	}

	private List<UsageRecord> loadRecords(JDBCUsageRepository repository, ZonedDateTime from, ZonedDateTime to) {
		Product product = new Product(PRODUCT_ID);
		Feature feature = new Feature(product, FEATURE_ID);
		IndividualUser user = new IndividualUser(USER_ID);

		RecordSearchCriteria criteria = new RecordSearchCriteria(LIMIT_ID, from, to);
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		repository.loadUsageData(context);
		return context.getCurrentUsageRecords();
	}

	private void updateRecords(JDBCUsageRepository repository, List<UsageRecord> recordsToUpdate, ZonedDateTime newExpirationDate) {
		List<UsageRecord> updatedRecords = new ArrayList<>();
		for (UsageRecord usageRecord : recordsToUpdate) {
			JDBCUsageRecordRepoMetadata updatedMetadata = (JDBCUsageRecordRepoMetadata) usageRecord.repoMetadata();
			UsageRecord updatedRecord = new UsageRecord(
					updatedMetadata,
					usageRecord.limitId(),
					usageRecord.startTime(),
					usageRecord.endTime(),
					usageRecord.units() + 10,
					newExpirationDate
			);
			updatedRecords.add(updatedRecord);
		}

		Product product = new Product(PRODUCT_ID);
		Feature feature = new Feature(product, FEATURE_ID);
		IndividualUser user = new IndividualUser(USER_ID);

		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of());
		context.addUpdatedUsageRecords(updatedRecords);
		repository.updateUsageRecords(context);
	}

	@ParameterizedTest
	@MethodSource("databaseContainers")
	void testRepositoryOperations(String databaseType, JdbcDatabaseContainer<?> container) throws SQLException {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setUrl(container.getJdbcUrl());
		dataSource.setUsername(container.getUsername());
		dataSource.setPassword(container.getPassword());

		createTable(databaseType, dataSource);

		JDBCUsageRepository repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, TABLE_NAME);

		List<Timestamp> timestamps = List.of(
				Timestamp.from(ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant()),
				Timestamp.from(ZonedDateTime.of(2022, 1, 1, 7, 0, 0, 0, ZoneId.of("America/New_York")).toInstant()),
				Timestamp.from(ZonedDateTime.of(2022, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/London")).toInstant())
		);

		try (Connection connection = dataSource.getConnection()) {
			insertRecords(connection, databaseType, timestamps);
		}

		List<UsageRecord> loadedRecords = loadRecords(repository,
				ZonedDateTime.of(2021, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC")),
				ZonedDateTime.of(2022, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
		);

		assertEquals(timestamps.size(), loadedRecords.size());

		ZonedDateTime newExpirationDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		updateRecords(repository, loadedRecords, newExpirationDate);

		List<UsageRecord> finalLoadedRecords = loadRecords(repository,
				ZonedDateTime.of(2021, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC")),
				ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
		);

		for (UsageRecord usageRecord : finalLoadedRecords) {
			assertEquals(UNITS + 10, usageRecord.units());
			assertTrue(usageRecord.expirationDate().isEqual(newExpirationDate));
		}

		repository.deleteOldRecords(ZonedDateTime.now());
		loadedRecords = loadRecords(repository,
				ZonedDateTime.of(2021, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC")),
				ZonedDateTime.of(2025, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"))
		);

		assertTrue(loadedRecords.isEmpty());
	}
}
