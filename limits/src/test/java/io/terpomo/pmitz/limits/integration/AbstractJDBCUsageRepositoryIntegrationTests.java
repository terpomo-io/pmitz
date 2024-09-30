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

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractJDBCUsageRepositoryIntegrationTests {

	protected BasicDataSource dataSource;
	protected JDBCUsageRepository repository;

	// Abstract method for child classes to provide the query for checking timezone
	protected abstract String getTimeZoneQuery();

	protected abstract boolean isSingleTimeZoneQuery(); // Some databases return only one value

	protected Clock clock;

	static final String CUSTOM_SCHEMA = "pmitz";

	String getFullTableName() {
		return CUSTOM_SCHEMA + "." + getTableName();
	}

	@BeforeEach
	public void setUp() throws SQLException {
		setupDataSource();
		clock = Clock.fixed(Instant.now(), ZoneOffset.UTC); // Always set the clock to UTC
		ensureDatabaseTimezoneIsUTC(); // Ensure database is in UTC
		setupDatabase();
		printDatabaseContents("After setupDatabase");
	}

	@AfterEach
	public void tearDown() throws SQLException {
		try (Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			conn.commit();
		}
		tearDownDatabase();
		dataSource.close();
	}

	public void ensureDatabaseTimezoneIsUTC() throws SQLException {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(getTimeZoneQuery());
				ResultSet rs = statement.executeQuery()) {

			if (rs.next()) {
				if (isSingleTimeZoneQuery()) {
					String timezone = rs.getString(1);
					// Normalize SQL Server's "(UTC) Coordinated Universal Time" to "UTC"
					if (timezone.contains("Coordinated Universal Time")) {
						timezone = "UTC";
					}
					assertThat(timezone).isEqualTo("UTC");
				}
				else {
					String globalTimezone = rs.getString(1);
					String sessionTimezone = rs.getString(2);

					// Normalize SQL Server's timezones if necessary
					if (globalTimezone.contains("Coordinated Universal Time")) {
						globalTimezone = "UTC";
					}
					if (sessionTimezone.contains("Coordinated Universal Time")) {
						sessionTimezone = "UTC";
					}

					assertThat(globalTimezone).isEqualTo("UTC");
					assertThat(sessionTimezone).isEqualTo("UTC");
				}
			}
		}
	}


	protected abstract void setupDataSource();

	protected abstract String getTableName();

	protected abstract void setupDatabase() throws SQLException;

	protected abstract void tearDownDatabase() throws SQLException;

	protected abstract void printDatabaseContents(String message) throws SQLException;

	public static void assertLocalDateTimeEqualsIgnoringMillis(LocalDateTime expected, LocalDateTime actual) {
		assertThat(actual.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(expected.truncatedTo(ChronoUnit.SECONDS));
	}

	@Test
	public void testLoadUsageData() throws SQLException {
		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		ZonedDateTime nowZoned = ZonedDateTime.now(clock);
		LocalDateTime startTime = nowZoned.minusMinutes(10).toLocalDateTime();
		LocalDateTime endTime = nowZoned.plusMinutes(10).toLocalDateTime();

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", startTime.atZone(ZoneOffset.UTC), endTime.atZone(ZoneOffset.UTC));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		UsageRecord record1 = new UsageRecord(null, "limit1", startTime.plusMinutes(1).atZone(ZoneOffset.UTC), null, 100L, nowZoned.plusDays(1));
		UsageRecord record2 = new UsageRecord(null, "limit1", startTime.plusMinutes(2).atZone(ZoneOffset.UTC), endTime.minusMinutes(1).atZone(ZoneOffset.UTC), 150L, nowZoned.plusDays(2));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(contextToInsert);

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(2);

		UsageRecord loadedRecord1 = loadedRecords.get(0);
		assertThat(loadedRecord1.limitId()).isEqualTo("limit1");
		assertLocalDateTimeEqualsIgnoringMillis(loadedRecord1.startTime().toLocalDateTime(), startTime.plusMinutes(1));
		assertThat(loadedRecord1.endTime()).isNull();  // Check for null window_end
		assertThat(loadedRecord1.units()).isEqualTo(100L);

		UsageRecord loadedRecord2 = loadedRecords.get(1);
		assertThat(loadedRecord2.limitId()).isEqualTo("limit1");
		assertLocalDateTimeEqualsIgnoringMillis(loadedRecord2.startTime().toLocalDateTime(), startTime.plusMinutes(2));
		assertLocalDateTimeEqualsIgnoringMillis(loadedRecord2.endTime().toLocalDateTime(), endTime.minusMinutes(1));
		assertThat(loadedRecord2.units()).isEqualTo(150L);
	}

	@Test
	public void testUpdateUsageRecordsIntegration() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);
		UsageRecord record1 = new UsageRecord(null, "limit1", now.atZone(ZoneOffset.UTC), now.plusHours(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.atZone(ZoneOffset.UTC), now.plusHours(2).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial updateUsageRecords");

		UsageRecord updatedRecord1 = new UsageRecord(null, "limit1", now.atZone(ZoneOffset.UTC), now.plusHours(1).atZone(ZoneOffset.UTC), 150L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord updatedRecord2 = new UsageRecord(null, "limit2", now.atZone(ZoneOffset.UTC), now.plusHours(2).atZone(ZoneOffset.UTC), 250L, now.plusDays(2).atZone(ZoneOffset.UTC));

		LimitTrackingContext contextToUpdate = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToUpdate.addUpdatedUsageRecords(List.of(updatedRecord1, updatedRecord2));

		repository.updateUsageRecords(contextToUpdate);
		printDatabaseContents("After updateUsageRecords with updates");

		try (Connection connection = dataSource.getConnection();
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + repository.getFullTableName())) {
			int count = 0;
			while (rs.next()) {
				count++;
				String limitId = rs.getString("limit_id");
				LocalDateTime startTime = rs.getTimestamp("window_start").toLocalDateTime();
				LocalDateTime endTime = rs.getTimestamp("window_end").toLocalDateTime();
				long units = rs.getLong("units");

				if ("limit1".equals(limitId)) {
					assertLocalDateTimeEqualsIgnoringMillis(startTime, now);
					assertLocalDateTimeEqualsIgnoringMillis(endTime, now.plusHours(1));
					assertThat(units).isEqualTo(150L);
				}
				else if ("limit2".equals(limitId)) {
					assertLocalDateTimeEqualsIgnoringMillis(startTime, now);
					assertLocalDateTimeEqualsIgnoringMillis(endTime, now.plusHours(2));
					assertThat(units).isEqualTo(250L);
				}
			}
			assertThat(count).isEqualTo(2);
		}
	}

	@Test
	public void testUpdateUsageRecordsWithNullDates() throws SQLException {
		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.atZone(ZoneOffset.UTC), null, 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", null, now.plusHours(2).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(context);

		try (Connection connection = dataSource.getConnection();
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + repository.getFullTableName())) {
			while (rs.next()) {
				String limitId = rs.getString("limit_id");
				LocalDateTime startTime = (rs.getTimestamp("window_start") != null) ? rs.getTimestamp("window_start").toLocalDateTime() : null;
				LocalDateTime endTime = (rs.getTimestamp("window_end") != null) ? rs.getTimestamp("window_end").toLocalDateTime() : null;

				if ("limit1".equals(limitId)) {
					assertThat(startTime).isNotNull();
					assertThat(endTime).isNull();  // Check for null window_end
				}
				else if ("limit2".equals(limitId)) {
					assertThat(startTime).isNull();  // Check for null window_start
					assertThat(endTime).isNotNull();
				}
			}
		}
	}

	@Test
	public void testUpdateUsageRecordsWithNullExpirationDates() throws SQLException {
		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.atZone(ZoneOffset.UTC), now.plusHours(1).atZone(ZoneOffset.UTC), 100L, null);
		UsageRecord record2 = new UsageRecord(null, "limit2", now.atZone(ZoneOffset.UTC), now.plusHours(2).atZone(ZoneOffset.UTC), 200L, null);

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(context);

		try (Connection connection = dataSource.getConnection();
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + repository.getFullTableName())) {
			while (rs.next()) {
				String limitId = rs.getString("limit_id");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");

				assertThat(expirationDate).isNull();  // Ensure null expiration_date
			}
		}
	}

	@Test
	public void testUpdateUsageRecordsWithNullStartAndEndDates() throws SQLException {
		UsageRecord record1 = new UsageRecord(null, "limit1", null, null, 100L, null);

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record1));
		repository.updateUsageRecords(context);

		printDatabaseContents("After update with null start and end dates");

		try (Connection connection = dataSource.getConnection();
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM " + repository.getFullTableName())) {
			int count = 0;
			while (rs.next()) {
				count++;
				String limitId = rs.getString("limit_id");
				LocalDateTime startTime = (rs.getTimestamp("window_start") != null) ? rs.getTimestamp("window_start").toLocalDateTime() : null;
				LocalDateTime endTime = (rs.getTimestamp("window_end") != null) ? rs.getTimestamp("window_end").toLocalDateTime() : null;
				long units = rs.getLong("units");

				if ("limit1".equals(limitId)) {
					assertThat(startTime).isNull();
					assertThat(endTime).isNull();
					assertThat(units).isEqualTo(100L);
				}
			}
			assertThat(count).isEqualTo(1);
		}
	}

	@Test
	public void testLoadUsageDataWithNullWindowStart() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", null, now.plusMinutes(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LocalDateTime queryEnd = now.plusMinutes(5);
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", null, queryEnd.atZone(ZoneOffset.UTC));  // Null windowStart

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);
		UsageRecord loadedRecord = loadedRecords.get(0);
		assertThat(loadedRecord.limitId()).isEqualTo("limit1");
		assertThat(loadedRecord.startTime()).isNull();  // Start time is null
		assertLocalDateTimeEqualsIgnoringMillis(record1.endTime().toLocalDateTime(), loadedRecord.endTime().toLocalDateTime());
		assertThat(loadedRecord.units()).isEqualTo(record1.units());
		assertLocalDateTimeEqualsIgnoringMillis(record1.expirationDate().toLocalDateTime(), loadedRecord.expirationDate().toLocalDateTime());
	}

	@Test
	public void testLoadUsageDataWithNullWindowEnd() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.minusMinutes(1).atZone(ZoneOffset.UTC), null, 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LocalDateTime queryStart = now.minusMinutes(5);
		LocalDateTime queryEnd = now.plusMinutes(5);
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", queryStart.atZone(ZoneOffset.UTC), queryEnd.atZone(ZoneOffset.UTC));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);
		UsageRecord loadedRecord = loadedRecords.get(0);
		assertThat(loadedRecord.limitId()).isEqualTo("limit1");
		assertLocalDateTimeEqualsIgnoringMillis(record1.startTime().toLocalDateTime(), loadedRecord.startTime().toLocalDateTime());
		assertThat(loadedRecord.endTime()).isNull();
	}

	@Test
	public void testLoadUsageDataWithInvalidLimitId() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.minusMinutes(1).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LocalDateTime queryStart = now.minusMinutes(5);
		LocalDateTime queryEnd = now.plusMinutes(5);
		RecordSearchCriteria criteria = new RecordSearchCriteria("invalid_limit", queryStart.atZone(ZoneOffset.UTC), queryEnd.atZone(ZoneOffset.UTC));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();  // No records should be returned for an invalid limitId
	}

	@Test
	public void testLoadUsageDataWithNullLimitId() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.minusMinutes(1).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LocalDateTime queryStart = now.minusMinutes(5);
		LocalDateTime queryEnd = now.plusMinutes(5);
		RecordSearchCriteria criteria = new RecordSearchCriteria(null, queryStart.atZone(ZoneOffset.UTC), queryEnd.atZone(ZoneOffset.UTC));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(2);
	}

	@Test
	public void testLoadUsageDataWithOutOfBoundsTimeRange() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.minusMinutes(1).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LocalDateTime queryStart = now.plusHours(2);  // Start searching after the last record's end time
		LocalDateTime queryEnd = now.plusHours(3);    // Search window is fully outside bounds
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", queryStart.atZone(ZoneOffset.UTC), queryEnd.atZone(ZoneOffset.UTC));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();  // No records should be found since the time range is out of bounds
	}

	@Test
	public void testLoadUsageDataWithEmptyCriteria() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		LocalDateTime now = LocalDateTime.now(clock);

		UsageRecord record1 = new UsageRecord(null, "limit1", now.minusMinutes(1).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 100L, now.plusDays(1).atZone(ZoneOffset.UTC));
		UsageRecord record2 = new UsageRecord(null, "limit2", now.minusMinutes(2).atZone(ZoneOffset.UTC), now.plusMinutes(1).atZone(ZoneOffset.UTC), 200L, now.plusDays(2).atZone(ZoneOffset.UTC));

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));

		repository.updateUsageRecords(contextToInsert);
		printDatabaseContents("After initial insert");

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());

		repository.loadUsageData(context);
		printDatabaseContents("After loadUsageData");

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();  // No criteria means no data should be returned
	}

	@Test
	public void testLoadUsageDataWithLargeDatasets() throws SQLException {
		setupDatabase();
		printDatabaseContents("After setupDatabase");

		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user1";
			}
		};

		LocalDateTime now = LocalDateTime.now(clock);
		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());

		for (int i = 0; i < 1000; i++) {
			UsageRecord record = new UsageRecord(null, "limit1", now.minusMinutes(i).atZone(ZoneOffset.UTC), now.minusMinutes(i - 5).atZone(ZoneOffset.UTC), 100L + i, now.plusDays(1).atZone(ZoneOffset.UTC));
			contextToInsert.addUpdatedUsageRecords(List.of(record));
		}

		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", now.minusMinutes(1000).atZone(ZoneOffset.UTC), now.plusMinutes(10).atZone(ZoneOffset.UTC));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);
		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1000);
	}

}
