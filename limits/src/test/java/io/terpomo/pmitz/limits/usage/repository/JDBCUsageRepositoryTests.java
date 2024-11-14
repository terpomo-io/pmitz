package io.terpomo.pmitz.limits.usage.repository;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRecordRepoMetadata;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import io.terpomo.pmitz.limits.usage.repository.impl.LimitTrackingContextBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
					+ "window_start TIMESTAMP, "
					+ "window_end TIMESTAMP, "
					+ "units integer, "
					+ "expiration_date TIMESTAMP, "
					+ "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
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
	void testLoadUsageDataWithDifferentValues() throws Exception {
		ZonedDateTime fixedTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		UsageRecord newRecord = new UsageRecord(
				null,
				"limit2",
				fixedTime.minusDays(15),
				fixedTime.minusDays(5),
				200L,
				fixedTime.plusMonths(2)
		);

		LimitTrackingContext context = createLimitTrackingContext("limit2", "user2", "product2", "feature2",
				fixedTime.minusDays(15), fixedTime.minusDays(5))
				.addUpdatedUsageRecord(newRecord)
				.build();

		repository.updateUsageRecords(context);

		context = createLimitTrackingContext("limit2", "user2", "product2", "feature2",
				fixedTime.minusDays(15), fixedTime.minusDays(5))
				.build();

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.isEmpty()).isFalse();
		UsageRecord record = records.get(0);
		assertThat(record.limitId()).isEqualTo("limit2");
		assertThat(record.startTime().toInstant().toEpochMilli()).isEqualTo(fixedTime.minusDays(15).toInstant().toEpochMilli());
		assertThat(record.endTime().toInstant().toEpochMilli()).isEqualTo(fixedTime.minusDays(5).toInstant().toEpochMilli());
		assertThat(record.units()).isEqualTo(200L);
	}

	@Test
	void testUpdateUsageRecordsWithDifferentValues() throws Exception {
		ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);

		UsageRecord newRecord = new UsageRecord(
				null,
				"limit3",
				now.minusDays(40),
				now.minusDays(20),
				300L,
				now.plusMonths(4)
		);

		LimitTrackingContext context = createLimitTrackingContext("limit3", "user3", "product3", "feature3",
				now.minusDays(40), now.minusDays(20))
				.addUpdatedUsageRecord(newRecord)
				.build();

		repository.updateUsageRecords(context);

		checkInsertedRecord(now.minusDays(40), now.minusDays(20));

		UsageRecord updatedRecord = new UsageRecord(
				new JDBCUsageRecordRepoMetadata(1L, now),
				"limit3",
				now.minusDays(35),
				now.minusDays(15),
				350L,
				now.plusMonths(4)
		);

		context = createLimitTrackingContext("limit3", "user3", "product3", "feature3",
				now.minusDays(40), now.minusDays(20))
				.addUpdatedUsageRecord(updatedRecord)
				.build();

		repository.updateUsageRecords(context);

		checkInsertedRecord(now.minusDays(35), now.minusDays(15));

		// Additional assertion to verify the updated units
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				java.sql.ResultSet resultSet = statement.executeQuery("SELECT units FROM " + repository.getFullTableName() + " ORDER BY usage_id DESC LIMIT 1")) {

			if (resultSet.next()) {
				int actualUnits = resultSet.getInt("units");
				assertThat((long) actualUnits).isEqualTo(350L);
			}
		}
	}

	@Test
	void testLoadUsageDataWithEmptyResultSet() {
		ZonedDateTime time = ZonedDateTime.now().minusYears(1);
		LimitTrackingContext context = createLimitTrackingContext("nonExistingLimit", "user2", "product2", "feature2",
				time, time)
				.build();

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.isEmpty()).isTrue();
	}

	@Test
	void testLoadUsageDataWithMultipleRecords() throws Exception {
		ZonedDateTime time = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		// Insert two records with different start times
		UsageRecord newRecord1 = new UsageRecord(
				null,
				"limit4",
				time.minusDays(60),
				time.minusDays(40),
				200L,
				time.plusMonths(6)
		);

		UsageRecord newRecord2 = new UsageRecord(
				null,
				"limit4",
				time.minusDays(30),
				time.minusDays(10),
				100L,
				time.plusMonths(3)
		);

		LimitTrackingContext context = createLimitTrackingContext("limit4", "user4", "product4", "feature4",
				time.minusDays(60), time.minusDays(10))
				.addUpdatedUsageRecord(newRecord1)
				.addUpdatedUsageRecord(newRecord2)
				.build();

		repository.updateUsageRecords(context);

		// Ensure startDate is before endDate
		testFilteringLogic(time.minusDays(60), time.minusDays(30), 2);
		testFilteringLogic(time.minusDays(60), time.minusDays(10), 2);
		testFilteringLogic(time.minusDays(61), time.minusDays(29), 2);
	}

	void testFilteringLogic(ZonedDateTime startDate, ZonedDateTime endDate, int expectedRecordCount) throws Exception {
		// Normalize the date range
		ZonedDateTime normalizedStartDate = startDate.isBefore(endDate) ? startDate : endDate;
		ZonedDateTime normalizedEndDate = startDate.isBefore(endDate) ? endDate : startDate;

		// Create a context with the normalized date range
		LimitTrackingContext context = createLimitTrackingContext("limit4", "user4", "product4", "feature4",
				normalizedStartDate, normalizedEndDate)
				.build();

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();

		// Verify the expected number of records
		assertThat(records.size()).isEqualTo(expectedRecordCount);

		// Additional assertions to verify the records' properties
		for (UsageRecord record : records) {
			assertThat(record.startTime()).isBetween(normalizedStartDate, normalizedEndDate);
		}
	}

	private void checkInsertedRecord(ZonedDateTime expectedStart, ZonedDateTime expectedEnd) throws Exception {
		String selectQuery = "SELECT window_start, window_end "
				+ "FROM " + repository.getFullTableName() + " ORDER BY usage_id DESC LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				java.sql.ResultSet resultSet = statement.executeQuery(selectQuery)) {

			if (resultSet.next()) {
				ZonedDateTime actualStart = resultSet.getTimestamp("window_start").toInstant().atZone(ZoneOffset.UTC);
				ZonedDateTime actualEnd = resultSet.getTimestamp("window_end") != null ?
						resultSet.getTimestamp("window_end").toInstant().atZone(ZoneOffset.UTC) : null;

				// Verify the actual dates match the expected dates
				assertThat(actualStart).isEqualTo(expectedStart.withZoneSameInstant(ZoneOffset.UTC));
				if (actualEnd != null) {
					assertThat(actualEnd).isEqualTo(expectedEnd.withZoneSameInstant(ZoneOffset.UTC));
				}
			}
		}
	}

	@Test
	void testTimezoneConversions() throws Exception {
		// Create a new UsageRecord with a specific time zone (e.g., America/Vancouver)
		ZoneId vancouverTimeZone = ZoneId.of("America/Vancouver");
		ZonedDateTime windowStartVancouver = ZonedDateTime.of(2024, 10, 8, 17, 15, 34, 0, vancouverTimeZone);
		ZonedDateTime windowEndVancouver = windowStartVancouver.plusHours(1);

		UsageRecord newRecord = new UsageRecord(
				null,
				"limit5",
				windowStartVancouver,
				windowEndVancouver,
				200L,
				windowEndVancouver.plusDays(1)
		);

		LimitTrackingContext context = createLimitTrackingContext("limit5", "user5", "product5", "feature5",
				windowStartVancouver, windowEndVancouver)
				.addUpdatedUsageRecord(newRecord)
				.build();

		repository.updateUsageRecords(context);

		// Retrieve the inserted record and verify time zone conversions
		context = createLimitTrackingContext("limit5", "user5", "product5", "feature5",
				windowStartVancouver, windowEndVancouver)
				.build();

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records.size()).isEqualTo(1);

		UsageRecord insertedRecord = records.get(0);

		// The repository stores and retrieves times in UTC, so we convert expected times to UTC for comparison
		ZonedDateTime expectedStartUTC = windowStartVancouver.withZoneSameInstant(ZoneOffset.UTC);
		ZonedDateTime expectedEndUTC = windowEndVancouver.withZoneSameInstant(ZoneOffset.UTC);

		assertThat(insertedRecord.startTime()).isEqualTo(expectedStartUTC);
		assertThat(insertedRecord.endTime()).isEqualTo(expectedEndUTC);
	}

	private LimitTrackingContextBuilder createLimitTrackingContext(String limitId, String userId, String productId, String featureId,
			ZonedDateTime windowStart, ZonedDateTime windowEnd) {
		return LimitTrackingContextBuilder.newInstance()
				.withLimitId(limitId)
				.withUserId(userId)
				.withProductId(productId)
				.withFeatureId(featureId)
				.addSearchCriteria(new RecordSearchCriteria(limitId, windowStart, windowEnd));
	}
}
