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

package io.terpomo.pmitz.limits.usage.repository;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRecordRepoMetadata;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;

import static org.assertj.core.api.Assertions.assertThat;

class JDBCUsageRepositoryTests {

	private static final String CUSTOM_SCHEMA = "pmitz";
	private JDBCUsageRepository repository;
	private JdbcDataSource dataSource;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		dataSource.setPassword("");

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
			stmt.execute("CREATE TABLE " + CUSTOM_SCHEMA + ".\"Usage\" ("
					+ "usage_id serial PRIMARY KEY, "
					+ "feature_id varchar, "
					+ "product_id varchar, "
					+ "user_grouping varchar, "
					+ "limit_id varchar, "
					+ "window_start timestamp, "
					+ "window_end timestamp, "
					+ "units integer, "
					+ "expiration_date timestamp, "
					+ "updated_at timestamp DEFAULT CURRENT_TIMESTAMP"
					+ ");");
		}

		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, "\"Usage\"");
	}

	@AfterEach
	void tearDown() throws Exception {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS " + CUSTOM_SCHEMA + ".\"Usage\";");
		}
	}

	@Test
	void testLoadUsageDataWithDifferentValues() throws SQLException {
		ZonedDateTime fixedTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		insertUsageRecord("feature2", "product2", "group2", "limit2",
				fixedTime.minusDays(15).toLocalDateTime(),
				fixedTime.minusDays(5).toLocalDateTime(),
				200, fixedTime.plusMonths(2).toLocalDateTime());

		LimitTrackingContext context = createContext("limit2", fixedTime.minusDays(15), fixedTime.minusDays(5));
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.isEmpty()).isFalse();
		UsageRecord record = records.get(0);
		assertThat(record.limitId()).isEqualTo("limit2");
	}

	@Test
	void testUpdateUsageRecordsWithDifferentValues() throws SQLException {
		ZonedDateTime now = ZonedDateTime.of(2024, 8, 9, 3, 7, 54, 0, ZoneOffset.UTC);

		System.out.println("Inserting record with StartTime: " + now);

		insertUsageRecord("feature3", "product3", "group3", "limit3",
				now.minusDays(40).toLocalDateTime(),
				now.minusDays(20).toLocalDateTime(),
				300, now.plusMonths(4).toLocalDateTime());

		System.out.println("Inserted record: windowStart=" + now.minusDays(40).toLocalDateTime()
				+ ", windowEnd=" + now.minusDays(20).toLocalDateTime());

		checkInsertedRecord(now.minusDays(40).toLocalDateTime(), now.minusDays(20).toLocalDateTime());

		Feature feature = new Feature(new Product("product3"), "feature3");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user3";
			}
		};

		UsageRecord updatedRecord = new UsageRecord(
				new JDBCUsageRecordRepoMetadata(1, now),
				"limit3",
				now.minusDays(35),
				now.minusDays(15),
				350L,
				now.plusMonths(4)
		);

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(updatedRecord));

		System.out.println("Attempting to update record with StartTime: " + updatedRecord.startTime() +
				", EndTime: " + updatedRecord.endTime());

		repository.updateUsageRecords(context);

		checkInsertedRecord(now.minusDays(35).toLocalDateTime(), now.minusDays(15).toLocalDateTime());
	}

	private void checkInsertedRecord(LocalDateTime expectedStart, LocalDateTime expectedEnd) throws SQLException {
		String selectQuery = "SELECT window_start, window_end FROM " + repository.getFullTableName() + " ORDER BY usage_id DESC LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(selectQuery);
				ResultSet resultSet = statement.executeQuery()) {

			if (resultSet.next()) {
				LocalDateTime actualStart = resultSet.getTimestamp("window_start").toLocalDateTime();
				LocalDateTime actualEnd = resultSet.getTimestamp("window_end").toLocalDateTime();

				System.out.println("Expected start: " + expectedStart);
				System.out.println("Actual start from DB: " + actualStart);
				System.out.println("Expected end: " + expectedEnd);
				System.out.println("Actual end from DB: " + actualEnd);

				assertThat(actualStart).isEqualTo(expectedStart);
				assertThat(actualEnd).isEqualTo(expectedEnd);
			}
		}
	}

	private void insertUsageRecord(String featureId, String productId, String userGrouping,
			String limitId, LocalDateTime windowStart, LocalDateTime windowEnd,
			int units, LocalDateTime expirationDate) throws SQLException {
		System.out.println("Inserting with windowStart: " + windowStart + ", windowEnd: " + windowEnd);
		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + CUSTOM_SCHEMA + ".\"Usage\" " +
						"(feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
			stmt.setString(1, featureId);
			stmt.setString(2, productId);
			stmt.setString(3, userGrouping);
			stmt.setString(4, limitId);
			stmt.setTimestamp(5, Timestamp.valueOf(windowStart));
			stmt.setTimestamp(6, Timestamp.valueOf(windowEnd));
			stmt.setInt(7, units);
			stmt.setTimestamp(8, Timestamp.valueOf(expirationDate));
			stmt.executeUpdate();
		}
	}

	private LimitTrackingContext createContext(String limitId, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user2";
			}
		};
		return new LimitTrackingContext(
				new Feature(new Product("product2"), "feature2"),
				userGrouping,
				List.of(new RecordSearchCriteria(limitId, windowStart, windowEnd))
		);
	}

	@Test
	void testLoadUsageDataWithEmptyResultSet() {
		ZonedDateTime time = ZonedDateTime.now().minusYears(1);
		LimitTrackingContext context = createContext("nonExistingLimit", time, time);

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.isEmpty()).isTrue();
	}

	@Test
	void testLoadUsageDataWithMultipleRecords() throws SQLException {
		ZonedDateTime time = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

		// Insert two records with different start and end times
		insertUsageRecord("feature4", "product4", "group4", "limit4",
				time.minusDays(30).toLocalDateTime(),
				time.minusDays(10).toLocalDateTime(),
				100, time.plusMonths(3).toLocalDateTime());

		insertUsageRecord("feature5", "product5", "group5", "limit4",
				time.minusDays(60).toLocalDateTime(),
				time.minusDays(40).toLocalDateTime(),
				200, time.plusMonths(6).toLocalDateTime());

		// Print out the inserted records for debugging
		System.out.println("Inserted record 1: windowStart=" + time.minusDays(30).toLocalDateTime()
				+ ", windowEnd=" + time.minusDays(10).toLocalDateTime());
		System.out.println("Inserted record 2: windowStart=" + time.minusDays(60).toLocalDateTime()
				+ ", windowEnd=" + time.minusDays(40).toLocalDateTime());

		// Use a time range that should include both records
		LimitTrackingContext context = createContext("limit4", time.minusDays(90), time.plusDays(10));

		// Execute the query and check results
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.size()).isEqualTo(2);  // Expect 2 records
	}


}
