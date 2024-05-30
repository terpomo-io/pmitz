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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

import static org.junit.jupiter.api.Assertions.*;

class JDBCUsageRepositoryTests {

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
			stmt.execute("CREATE TABLE public.\"Usage\" ("
					+ "usage_id integer NOT NULL, "
					+ "feature_id varchar, "
					+ "product_id varchar, "
					+ "user_grouping varchar, "
					+ "limit_id varchar, "
					+ "window_start timestamp, "
					+ "window_end timestamp, "
					+ "units integer, "
					+ "expiration_date timestamp, "
					+ "updated_at timestamp DEFAULT CURRENT_TIMESTAMP, "
					+ "PRIMARY KEY (usage_id)"
					+ ");");
		}

		repository = new JDBCUsageRepository(dataSource, "public", "\"Usage\"");
	}

	@AfterEach
	void tearDown() throws Exception {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS public.\"Usage\";");
		}
	}

	@Test
	void testLoadUsageData() throws SQLException {
		ZonedDateTime fixedTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		insertUsageRecord(1, "feature1", "product1", "group1", "limit1", fixedTime.minusDays(30),
				fixedTime.minusDays(10), 100, fixedTime.plusMonths(3));

		// TODO: Remove
		printDatabaseContents();

		LimitTrackingContext context = createContext("limit1", fixedTime.minusDays(30), fixedTime.minusDays(10));
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertFalse(records.isEmpty());
		UsageRecord record = records.get(0);
		assertEquals("limit1", record.limitId());
	}

	@Test
	void testUpdateUsageRecords() throws SQLException {
		ZonedDateTime now = ZonedDateTime.now();
		insertUsageRecord(1, "feature1", "product1", "group1", "limit1", now.minusDays(30),
				now.minusDays(10), 100, now.plusMonths(3));

		Feature feature = new Feature(new Product("product1"), "feature1");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "group1";
			}
		};
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", now.minusDays(30), now.minusDays(10));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		JDBCUsageRecordRepoMetadata repoMetadata = new JDBCUsageRecordRepoMetadata(1, now);
		UsageRecord updatedRecord = new UsageRecord(repoMetadata, "limit1", now.minusDays(25),
				now.minusDays(5), 150L, now.plusMonths(3));

		context.addUpdatedUsageRecords(List.of(updatedRecord));
		repository.updateUsageRecords(context);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement("SELECT * FROM public.\"Usage\" WHERE usage_id = ?")) {
			stmt.setInt(1, 1);
			ResultSet rs = stmt.executeQuery();
			assertTrue(rs.next());

			Timestamp dbWindowStart = rs.getTimestamp("window_start");
			Timestamp dbWindowEnd = rs.getTimestamp("window_end");

			LocalDateTime expectedStart = updatedRecord.startTime().toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
			LocalDateTime expectedEnd = updatedRecord.endTime().toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);

			LocalDateTime actualStart = dbWindowStart.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
			LocalDateTime actualEnd = dbWindowEnd.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);

			assertEquals(expectedStart, actualStart);
			assertEquals(expectedEnd, actualEnd);
		}
	}

	@Test
	void testLoadUsageDataWithEmptyResultSet() {
		ZonedDateTime time = ZonedDateTime.now().minusYears(1);
		LimitTrackingContext context = createContext("nonExistingLimit", time, time);

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertTrue(records.isEmpty());
	}

	@Test
	void testLoadUsageDataWithMultipleRecords() throws SQLException {
		ZonedDateTime time = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		insertUsageRecord(1, "feature1", "product1", "group1", "limit1", time.minusDays(30), time.minusDays(10), 100, time.plusMonths(3));
		insertUsageRecord(2, "feature1", "product1", "group1", "limit1", time.minusDays(60), time.minusDays(40), 200, time.plusMonths(6));

		LimitTrackingContext context = createContext("limit1", time.minusDays(60), time);

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertEquals(2, records.size());
	}

	@Test
	void testLoadUsageDataBoundaryConditions() throws SQLException {
		ZonedDateTime start = ZonedDateTime.now().minusDays(30).truncatedTo(ChronoUnit.SECONDS);
		ZonedDateTime end = start.plusDays(20);

		insertUsageRecord(1, "feature1", "product1", "group1", "limit1", start.minusSeconds(1), end.plusSeconds(1), 100, end.plusMonths(3));

		LimitTrackingContext context = createContext("limit1", start, end);

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertTrue(records.isEmpty(), "No records should be fetched as they fall just outside the boundary");
	}


	@Test
	void testLoadUsageDataForDataIntegrity() throws SQLException {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS);
		ZonedDateTime windowStart = now.minusDays(30);
		ZonedDateTime expirationDate = now.plusMonths(3);
		int units = Integer.MAX_VALUE;

		insertUsageRecord(1, "feature1", "product1", "group1", "limit1", windowStart, now, units, expirationDate);

		LimitTrackingContext context = createContext("limit1", windowStart, now);

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertFalse(records.isEmpty(), "Records should be fetched");

		UsageRecord record = records.get(0);

		assertEquals("limit1", record.limitId());
		assertEquals(units, record.units());

		ZonedDateTime expectedStartTime = windowStart.withZoneSameInstant(ZoneId.of("UTC"));
		ZonedDateTime expectedEndTime = now.withZoneSameInstant(ZoneId.of("UTC"));
		ZonedDateTime expectedExpirationDate = expirationDate.withZoneSameInstant(ZoneId.of("UTC"));

		assertEquals(expectedStartTime.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS),
				record.startTime().toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
		assertEquals(expectedEndTime.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS),
				record.endTime().toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));
		assertEquals(expectedExpirationDate.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS),
				record.expirationDate().toLocalDateTime().truncatedTo(ChronoUnit.SECONDS));

		JDBCUsageRecordRepoMetadata metadata = (JDBCUsageRecordRepoMetadata) record.repoMetadata();
		assertNotNull(metadata);
		assertEquals(1, metadata.usageId());
		assertNotNull(metadata.updatedAt());
	}

	// TODO: Remove
	private void printDatabaseContents() {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM public.\"Usage\"");
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

				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		}
		catch (SQLException ex) {
			// TODO: handle exceptions
			ex.printStackTrace();
		}
	}

	// TODO: change so we don't provide usage id since it's auto generated on the db side
	private void insertUsageRecord(int id, String featureId, String productId, String userGrouping,
			String limitId, ZonedDateTime windowStart, ZonedDateTime windowEnd,
			int units, ZonedDateTime expirationDate) throws SQLException {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement("INSERT INTO public.\"Usage\" " +
						"(usage_id, feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			stmt.setInt(1, id);
			stmt.setString(2, featureId);
			stmt.setString(3, productId);
			stmt.setString(4, userGrouping);
			stmt.setString(5, limitId);
			stmt.setTimestamp(6, Timestamp.from(windowStart.toInstant()));
			stmt.setTimestamp(7, Timestamp.from(windowEnd.toInstant()));
			stmt.setInt(8, units);
			stmt.setTimestamp(9, Timestamp.from(expirationDate.toInstant()));
			stmt.executeUpdate();
		}
	}

	private LimitTrackingContext createContext(String limitId, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
		return new LimitTrackingContext(
				new Feature(new Product("product1"), "feature1"),
				new UserGrouping() {
					@Override
					public String getId() {
						return "group1";
					}
				},
				List.of(new RecordSearchCriteria(limitId, windowStart, windowEnd))
		);
	}
}

