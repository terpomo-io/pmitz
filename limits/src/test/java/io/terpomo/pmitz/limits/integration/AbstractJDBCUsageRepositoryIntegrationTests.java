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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.*;

public abstract class AbstractJDBCUsageRepositoryIntegrationTests {

	protected BasicDataSource dataSource;
	protected JDBCUsageRepository repository;

	static final String CUSTOM_SCHEMA = "pmitz";

	String getFullTableName() {
		return CUSTOM_SCHEMA + "." + getTableName();
	}

	@BeforeEach
	public void setUp() throws SQLException {
		setupDataSource();
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

	protected abstract void setupDataSource();

	protected abstract String getTableName();

	protected abstract void setupDatabase() throws SQLException;

	protected abstract void tearDownDatabase() throws SQLException;

	protected abstract void printDatabaseContents(String message) throws SQLException;

	public static void assertZonedDateTimeEqualsIgnoringMillis(ZonedDateTime expected, ZonedDateTime actual) {
		assertThat(actual).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
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

		ZonedDateTime now = ZonedDateTime.now();

		ZonedDateTime startTime = now.minusMinutes(10);
		ZonedDateTime endTime = now.plusMinutes(10);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit1", startTime, endTime);
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		UsageRecord record1 = new UsageRecord(null, "limit1", startTime.plusMinutes(1), null, 100L, now.plusDays(1));
		UsageRecord record2 = new UsageRecord(null, "limit1", startTime.plusMinutes(2), endTime.minusMinutes(1), 150L, now.plusDays(2));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(contextToInsert);

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(2);

		UsageRecord loadedRecord1 = loadedRecords.get(0);
		assertThat(loadedRecord1.limitId()).isEqualTo("limit1");
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord1.startTime(), startTime.plusMinutes(1));
		assertThat(loadedRecord1.endTime()).isNull();
		assertThat(loadedRecord1.units()).isEqualTo(100L);

		UsageRecord loadedRecord2 = loadedRecords.get(1);
		assertThat(loadedRecord2.limitId()).isEqualTo("limit1");
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord2.startTime(), startTime.plusMinutes(2));
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord2.endTime(), endTime.minusMinutes(1));
		assertThat(loadedRecord2.units()).isEqualTo(150L);
	}

	@Test
	public void testUpdateUsageRecordsIntegration() throws SQLException {
		Product product = new Product("product2");
		Feature feature = new Feature(product, "feature2");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user2";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record = new UsageRecord(null, "limit2", now.minusHours(1), now.plusHours(1), 200L, now.plusDays(1));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record));

		repository.updateUsageRecords(context);

		String selectQuery = "SELECT * FROM " + getFullTableName() + " WHERE limit_id = 'limit2'";

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(selectQuery)) {

			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("feature_id")).isEqualTo("feature2");
			assertThat(rs.getString("product_id")).isEqualTo("product2");
			assertThat(rs.getString("user_grouping")).isEqualTo("user2");
			assertThat(rs.getString("limit_id")).isEqualTo("limit2");

			ZonedDateTime windowStart = rs.getTimestamp("window_start").toInstant().atZone(ZoneOffset.UTC);
			ZonedDateTime windowEnd = rs.getTimestamp("window_end").toInstant().atZone(ZoneOffset.UTC);

			assertZonedDateTimeEqualsIgnoringMillis(windowStart, now.minusHours(1));
			assertZonedDateTimeEqualsIgnoringMillis(windowEnd, now.plusHours(1));

			assertThat(rs.getLong("units")).isEqualTo(200L);

			ZonedDateTime expirationDate = rs.getTimestamp("expiration_date").toInstant().atZone(ZoneOffset.UTC);
			assertZonedDateTimeEqualsIgnoringMillis(expirationDate, now.plusDays(1));
		}
	}

	@Test
	public void testUpdateUsageRecordsWithNullDates() throws SQLException {
		Product product = new Product("product3");
		Feature feature = new Feature(product, "feature3");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user3";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record = new UsageRecord(null, "limit3", null, null, 300L, null);

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record));

		repository.updateUsageRecords(context);

		String selectQuery = "SELECT * FROM " + getFullTableName() + " WHERE limit_id = 'limit3'";

		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(selectQuery)) {

			assertThat(rs.next()).isTrue();
			assertThat(rs.getString("feature_id")).isEqualTo("feature3");
			assertThat(rs.getString("product_id")).isEqualTo("product3");
			assertThat(rs.getString("user_grouping")).isEqualTo("user3");
			assertThat(rs.getString("limit_id")).isEqualTo("limit3");

			assertThat(rs.getTimestamp("window_start")).isNull();
			assertThat(rs.getTimestamp("window_end")).isNull();

			assertThat(rs.getLong("units")).isEqualTo(300L);

			assertThat(rs.getTimestamp("expiration_date")).isNull();
		}
	}

	@Test
	public void testLoadUsageDataWithNullWindowStart() throws SQLException {
		Product product = new Product("product4");
		Feature feature = new Feature(product, "feature4");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user4";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record = new UsageRecord(null, "limit4", null, now.plusHours(1), 400L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record));
		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit4", null, now.plusHours(2));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);

		UsageRecord loadedRecord = loadedRecords.get(0);
		assertThat(loadedRecord.limitId()).isEqualTo("limit4");
		assertThat(loadedRecord.startTime()).isNull();
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord.endTime(), now.plusHours(1));
		assertThat(loadedRecord.units()).isEqualTo(400L);
	}

	@Test
	public void testLoadUsageDataWithInvalidLimitId() throws SQLException {
		Product product = new Product("product5");
		Feature feature = new Feature(product, "feature5");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user5";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		// Create a new record without repoMetadata
		UsageRecord recordToInsert = new UsageRecord(null, "limit5", now.minusHours(1), now.plusHours(1), 500L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(recordToInsert));
		repository.updateUsageRecords(contextToInsert); // This will insert the record

		// Load the record to verify the insert
		RecordSearchCriteria validCriteria = new RecordSearchCriteria("limit5", now.minusHours(2), now.plusHours(2));
		LimitTrackingContext validContext = new LimitTrackingContext(feature, userGrouping, List.of(validCriteria));
		repository.loadUsageData(validContext);

		List<UsageRecord> validRecords = validContext.getCurrentUsageRecords();
		assertThat(validRecords).hasSize(1);

		// Test with an invalid limit id
		RecordSearchCriteria invalidCriteria = new RecordSearchCriteria("invalidLimit", now.minusHours(2), now.plusHours(2));
		LimitTrackingContext invalidContext = new LimitTrackingContext(feature, userGrouping, List.of(invalidCriteria));
		repository.loadUsageData(invalidContext);

		List<UsageRecord> invalidRecords = invalidContext.getCurrentUsageRecords();
		assertThat(invalidRecords).isEmpty();
	}

	@Test
	public void testLoadUsageDataWithOutOfBoundsTimeRange() throws SQLException {
		Product product = new Product("product6");
		Feature feature = new Feature(product, "feature6");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user6";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record = new UsageRecord(null, "limit6", now.minusHours(1), now.plusHours(1), 600L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(record));
		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit6", now.plusHours(2), now.plusHours(3));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();
	}

	@Test
	public void testUpdateUsageRecordsWithExistingRecord() throws SQLException {
		Product product = new Product("product7");
		Feature feature = new Feature(product, "feature7");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user7";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime fixedTime = ZonedDateTime.of(2024, 11, 2, 7, 0, 0, 0, ZoneOffset.UTC);

		// Insert a new record
		UsageRecord recordToInsert = new UsageRecord(null, "existing-limit", fixedTime, fixedTime.plusHours(1), 700L, fixedTime.plusDays(1));
		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(recordToInsert));
		repository.updateUsageRecords(contextToInsert);

		// Update the existing record
		UsageRecord recordToUpdate = new UsageRecord(null, "existing-limit", fixedTime, fixedTime.plusHours(2), 750L, fixedTime.plusDays(1));
		System.out.println("Updating record with endTime: " + recordToUpdate.endTime());

		LimitTrackingContext contextToUpdate = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToUpdate.addUpdatedUsageRecords(List.of(recordToUpdate));
		repository.updateUsageRecords(contextToUpdate);

		// Verify the update
		RecordSearchCriteria criteria = new RecordSearchCriteria("existing-limit", fixedTime.minusHours(1), fixedTime.plusHours(3));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));
		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);

		UsageRecord loadedRecord = loadedRecords.get(0);
		System.out.println("Loaded record with endTime: " + loadedRecord.endTime());

		assertZonedDateTimeEqualsIgnoringMillis(fixedTime, loadedRecord.startTime());
		assertZonedDateTimeEqualsIgnoringMillis(fixedTime.plusHours(2), loadedRecord.endTime());
		assertThat(loadedRecord.units()).isEqualTo(750L);
		assertZonedDateTimeEqualsIgnoringMillis(fixedTime.plusDays(1), loadedRecord.expirationDate());
	}

	@Test
	public void testDeleteOldRecords() throws SQLException {
		Product product = new Product("product8");
		Feature feature = new Feature(product, "feature8");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user8";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record1 = new UsageRecord(null, "limit8", now.minusDays(2), now.minusDays(1), 800L, now.minusHours(1));
		UsageRecord record2 = new UsageRecord(null, "limit8", now.minusHours(1), now.plusHours(1), 850L, now.plusDays(1));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(context);

		// Delete records with expiration date <= now
		repository.deleteOldRecords(now);

		// Load and verify
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit8", now.minusDays(3), now.plusDays(3));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));
		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);

		UsageRecord loadedRecord = loadedRecords.get(0);
		assertThat(loadedRecord.limitId()).isEqualTo("limit8");
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord.startTime(), now.minusHours(1));
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord.endTime(), now.plusHours(1));
		assertThat(loadedRecord.units()).isEqualTo(850L);
		assertZonedDateTimeEqualsIgnoringMillis(loadedRecord.expirationDate(), now.plusDays(1));
	}

	@Test
	public void testLoadUsageDataWithEmptyCriteria() throws SQLException {
		Product product = new Product("product9");
		Feature feature = new Feature(product, "feature9");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user9";
			}
		};

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();
	}

	@Test
	public void testLoadUsageDataWithLargeDatasets() throws SQLException {
		Product product = new Product("product10");
		Feature feature = new Feature(product, "feature10");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "user10";
			}
		};

		ZonedDateTime now = ZonedDateTime.now();

		// Insert multiple records
		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		for (int i = 0; i < 100; i++) {
			UsageRecord record = new UsageRecord(null, "limit10", now.minusDays(i), now.plusDays(i), 1000L + i, now.plusDays(i));
			contextToInsert.addUpdatedUsageRecords(List.of(record));
		}
		repository.updateUsageRecords(contextToInsert);

		// Load and verify
		RecordSearchCriteria criteria = new RecordSearchCriteria("limit10", now.minusDays(200), now.plusDays(200));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));
		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(100);
	}
}
