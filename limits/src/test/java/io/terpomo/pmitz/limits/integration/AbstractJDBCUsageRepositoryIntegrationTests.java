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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;

import static org.assertj.core.api.Assertions.*;

public abstract class AbstractJDBCUsageRepositoryIntegrationTests {

	protected BasicDataSource dataSource;
	protected JDBCUsageRepository repository;

	static final String CUSTOM_SCHEMA = "pmitz";

	String getFullTableName() {
		return CUSTOM_SCHEMA + "." + getTableName();
	}

	@BeforeEach
	void setUp() throws SQLException, IOException {
		setupDataSource();
		setupDatabase();
		printDatabaseContents("After setupDatabase");
	}

	@AfterEach
	void tearDown() throws SQLException, IOException {
		tearDownDatabase();
	}

	protected abstract void setupDataSource();

	protected abstract String getTableName();

	protected abstract void setupDatabase() throws SQLException, IOException;

	protected abstract void tearDownDatabase() throws SQLException, IOException;

	protected abstract void printDatabaseContents(String message) throws SQLException;

	public static void assertZonedDateTimeEqualsIgnoringMillis(ZonedDateTime expected, ZonedDateTime actual) {
		assertThat(actual).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
	}

	@Test
	void testLoadUsageData() {
		Product product = new Product("product1");
		Feature feature = new Feature(product, "feature1");
		IndividualUser userGrouping = new IndividualUser("user1");

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
		assertZonedDateTimeEqualsIgnoringMillis(startTime.plusMinutes(1), loadedRecord1.startTime());
		assertThat(loadedRecord1.endTime()).isNull();
		assertThat(loadedRecord1.units()).isEqualTo(100L);

		UsageRecord loadedRecord2 = loadedRecords.get(1);
		assertThat(loadedRecord2.limitId()).isEqualTo("limit1");
		assertZonedDateTimeEqualsIgnoringMillis(startTime.plusMinutes(2), loadedRecord2.startTime());
		assertZonedDateTimeEqualsIgnoringMillis(endTime.minusMinutes(1), loadedRecord2.endTime());
		assertThat(loadedRecord2.units()).isEqualTo(150L);
	}

	@Test
	void testUpdateUsageRecordsIntegration() {
		Product product = new Product("product2");
		Feature feature = new Feature(product, "feature2");
		IndividualUser userGrouping = new IndividualUser("user2");

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord usageRecord = new UsageRecord(null, "limit2", now.minusHours(1), now.plusHours(1), 200L, now.plusDays(1));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(usageRecord));

		try {
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

				assertZonedDateTimeEqualsIgnoringMillis(now.minusHours(1), windowStart);
				assertZonedDateTimeEqualsIgnoringMillis(now.plusHours(1), windowEnd);

				assertThat(rs.getLong("units")).isEqualTo(200L);

				ZonedDateTime expirationDate = rs.getTimestamp("expiration_date").toInstant().atZone(ZoneOffset.UTC);
				assertZonedDateTimeEqualsIgnoringMillis(now.plusDays(1), expirationDate);
			}
		}
		catch (SQLException ex) {
			Fail.fail("Error updating records", ex);
		}
	}

	@Test
	void testUpdateUsageRecordsWithNullDates() {
		Product product = new Product("product3");
		Feature feature = new Feature(product, "feature3");
		IndividualUser userGrouping = new IndividualUser("user3");

		UsageRecord usageRecord = new UsageRecord(null, "limit3", null, null, 300L, null);

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(usageRecord));

		try {
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
		catch (SQLException ex) {
			fail("Error updating records with null dates", ex);
		}
	}

	@Test
	void testLoadUsageDataWithNullWindowStart() {
		Product product = new Product("product4");
		Feature feature = new Feature(product, "feature4");
		IndividualUser userGrouping = new IndividualUser("user4");

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord usageRecord = new UsageRecord(null, "limit4", null, now.plusHours(1), 400L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(usageRecord));
		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit4", null, now.plusHours(2));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		try {
			repository.loadUsageData(context);

			List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
			assertThat(loadedRecords).hasSize(1);

			UsageRecord loadedRecord = loadedRecords.get(0);
			assertThat(loadedRecord.limitId()).isEqualTo("limit4");
			assertThat(loadedRecord.startTime()).isNull();
			assertZonedDateTimeEqualsIgnoringMillis(now.plusHours(1), loadedRecord.endTime());
			assertThat(loadedRecord.units()).isEqualTo(400L);
		}
		catch (Exception ex) {
			fail("Error loading data with null window start", ex);
		}
	}

	@Test
	void testLoadUsageDataWithInvalidLimitId() {
		Product product = new Product("product5");
		Feature feature = new Feature(product, "feature5");
		IndividualUser userGrouping = new IndividualUser("user5");

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord recordToInsert = new UsageRecord(null, "limit5", now.minusHours(1), now.plusHours(1), 500L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(recordToInsert));
		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria validCriteria = new RecordSearchCriteria("limit5", now.minusHours(2), now.plusHours(2));
		LimitTrackingContext validContext = new LimitTrackingContext(feature, userGrouping, List.of(validCriteria));
		repository.loadUsageData(validContext);

		List<UsageRecord> validRecords = validContext.getCurrentUsageRecords();
		assertThat(validRecords).hasSize(1);

		RecordSearchCriteria invalidCriteria = new RecordSearchCriteria("invalidLimit", now.minusHours(2), now.plusHours(2));
		LimitTrackingContext invalidContext = new LimitTrackingContext(feature, userGrouping, List.of(invalidCriteria));
		repository.loadUsageData(invalidContext);

		List<UsageRecord> invalidRecords = invalidContext.getCurrentUsageRecords();
		assertThat(invalidRecords).isEmpty();
	}

	@Test
	void testLoadUsageDataWithOutOfBoundsTimeRange() {
		Product product = new Product("product6");
		Feature feature = new Feature(product, "feature6");
		IndividualUser userGrouping = new IndividualUser("user6");

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord usageRecord = new UsageRecord(null, "limit6", now.minusHours(1), now.plusHours(1), 600L, now.plusDays(1));

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(usageRecord));
		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit6", now.plusHours(2), now.plusHours(3));
		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();
	}

	@Test
	void testUpdateUsageRecordsWithExistingRecord() {
		Product product = new Product("product7");
		Feature feature = new Feature(product, "feature7");
		IndividualUser userGrouping = new IndividualUser("user7");

		ZonedDateTime fixedTime = ZonedDateTime.of(2024, 11, 2, 7, 0, 0, 0, ZoneOffset.UTC);

		UsageRecord recordToInsert = new UsageRecord(null, "existingLimit", fixedTime, fixedTime.plusHours(1), 700L, fixedTime.plusDays(1));
		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToInsert.addUpdatedUsageRecords(List.of(recordToInsert));
		repository.updateUsageRecords(contextToInsert);

		UsageRecord recordToUpdate = new UsageRecord(null, "existingLimit", fixedTime, fixedTime.plusHours(2), 750L, fixedTime.plusDays(1));

		LimitTrackingContext contextToUpdate = new LimitTrackingContext(feature, userGrouping, List.of());
		contextToUpdate.addUpdatedUsageRecords(List.of(recordToUpdate));
		repository.updateUsageRecords(contextToUpdate);

		RecordSearchCriteria criteria = new RecordSearchCriteria("existingLimit", fixedTime.minusHours(1), fixedTime.plusHours(3));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));
		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);

		UsageRecord loadedRecord = loadedRecords.get(0);
		assertZonedDateTimeEqualsIgnoringMillis(fixedTime, loadedRecord.startTime());
		assertZonedDateTimeEqualsIgnoringMillis(fixedTime.plusHours(2), loadedRecord.endTime());
		assertThat(loadedRecord.units()).isEqualTo(750L);
		assertZonedDateTimeEqualsIgnoringMillis(fixedTime.plusDays(1), loadedRecord.expirationDate());
	}

	@Test
	void testDeleteOldRecords() {
		Product product = new Product("product8");
		Feature feature = new Feature(product, "feature8");
		IndividualUser userGrouping = new IndividualUser("user8");

		ZonedDateTime now = ZonedDateTime.now();

		UsageRecord record1 = new UsageRecord(null, "limit8", now.minusDays(2), now.minusDays(1), 800L, now.minusHours(1));
		UsageRecord record2 = new UsageRecord(null, "limit8", now.minusHours(1), now.plusHours(1), 850L, now.plusDays(1));

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());
		context.addUpdatedUsageRecords(List.of(record1, record2));
		repository.updateUsageRecords(context);

		repository.deleteOldRecords(now);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit8", now.minusDays(3), now.plusDays(3));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));
		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(1);

		UsageRecord loadedRecord = loadedRecords.get(0);
		assertThat(loadedRecord.limitId()).isEqualTo("limit8");
		assertZonedDateTimeEqualsIgnoringMillis(now.minusHours(1), loadedRecord.startTime());
		assertZonedDateTimeEqualsIgnoringMillis(now.plusHours(1), loadedRecord.endTime());
		assertThat(loadedRecord.units()).isEqualTo(850L);
		assertZonedDateTimeEqualsIgnoringMillis(now.plusDays(1), loadedRecord.expirationDate());
	}

	@Test
	void testLoadUsageDataWithEmptyCriteria() {
		Product product = new Product("product9");
		Feature feature = new Feature(product, "feature9");
		IndividualUser userGrouping = new IndividualUser("user9");

		LimitTrackingContext context = new LimitTrackingContext(feature, userGrouping, List.of());

		repository.loadUsageData(context);

		List<UsageRecord> loadedRecords = context.getCurrentUsageRecords();
		assertThat(loadedRecords).isEmpty();
	}

	@Test
	void testLoadUsageDataWithLargeDatasets() {
		Product product = new Product("product10");
		Feature feature = new Feature(product, "feature10");
		IndividualUser userGrouping = new IndividualUser("user10");

		ZonedDateTime now = ZonedDateTime.now();

		LimitTrackingContext contextToInsert = new LimitTrackingContext(feature, userGrouping, List.of());
		for (int i = 0; i < 100; i++) {
			UsageRecord usageRecord = new UsageRecord(null, "limit10", now.minusDays(i), now.plusDays(i), 1000L + i, now.plusDays(i));
			contextToInsert.addUpdatedUsageRecords(List.of(usageRecord));
		}

		repository.updateUsageRecords(contextToInsert);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit10", now.minusDays(200), now.plusDays(200));
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		repository.loadUsageData(loadContext);

		List<UsageRecord> loadedRecords = loadContext.getCurrentUsageRecords();
		assertThat(loadedRecords).hasSize(100);
	}
}
