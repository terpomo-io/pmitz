/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may obtain a copy at
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

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.IndividualUser;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRecordRepoMetadata;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
			stmt.execute("CREATE TABLE " + CUSTOM_SCHEMA + ".\"Usage\" (" +
					"usage_id serial PRIMARY KEY, " +
					"feature_id varchar, " +
					"product_id varchar, " +
					"user_grouping varchar, " +
					"limit_id varchar, " +
					"window_start TIMESTAMP, " +
					"window_end TIMESTAMP, " +
					"units integer, " +
					"expiration_date TIMESTAMP, " +
					"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
					");");
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
		UsageRecord newRecord = new UsageRecord(null, "limit2", fixedTime.minusDays(15),
				fixedTime.minusDays(5), 200L, fixedTime.plusMonths(2));

		Product product = new Product("product2");
		Feature feature = new Feature(product, "feature2");
		IndividualUser user = new IndividualUser("user2");

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit2",
				fixedTime.minusDays(15), fixedTime.minusDays(5));
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		context.addUpdatedUsageRecords(List.of(newRecord));
		repository.updateUsageRecords(context);

		context = new LimitTrackingContext(feature, user, List.of(criteria));
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records).hasSize(1);
		UsageRecord record = records.get(0);
		assertThat(record.limitId()).isEqualTo("limit2");
		assertThat(record.startTime()).isEqualTo(fixedTime.minusDays(15));
		assertThat(record.endTime()).isEqualTo(fixedTime.minusDays(5));
		assertThat(record.units()).isEqualTo(200L);
	}

	@Test
	void testUpdateUsageRecordsWithDifferentValues() throws Exception {
		ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MICROS);
		UsageRecord newRecord = new UsageRecord(null, "limit3", now.minusDays(40),
				now.minusDays(20), 300L, now.plusMonths(4));

		Product product = new Product("product3");
		Feature feature = new Feature(product, "feature3");
		IndividualUser user = new IndividualUser("user3");

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit3",
				now.minusDays(40), now.minusDays(20));
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		context.addUpdatedUsageRecords(List.of(newRecord));

		repository.updateUsageRecords(context);

		checkInsertedRecord(now.minusDays(40), now.minusDays(20));

		UsageRecord updatedRecord = new UsageRecord(new JDBCUsageRecordRepoMetadata(1L, now),
				"limit3", now.minusDays(35), now.minusDays(15), 350L, now.plusMonths(4));

		context = new LimitTrackingContext(feature, user, List.of(criteria));
		context.addUpdatedUsageRecords(List.of(updatedRecord));
		repository.updateUsageRecords(context);

		checkInsertedRecord(now.minusDays(35), now.minusDays(15));

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				java.sql.ResultSet resultSet = statement.executeQuery(
						"SELECT units FROM " + repository.getFullTableName() + " ORDER BY usage_id DESC LIMIT 1")) {

			assertThat(resultSet.next()).isTrue();
			int actualUnits = resultSet.getInt("units");
			assertThat(actualUnits).isEqualTo(350L);
		}
	}

	@Test
	void testLoadUsageDataWithEmptyResultSet() {
		ZonedDateTime time = ZonedDateTime.now().minusYears(1);

		Product product = new Product("product2");
		Feature feature = new Feature(product, "feature2");
		IndividualUser user = new IndividualUser("user2");

		RecordSearchCriteria criteria = new RecordSearchCriteria("nonExistingLimit", time, time);
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));

		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records).isEmpty();
	}

	@Test
	void testLoadUsageDataWithMultipleRecords() throws Exception {
		ZonedDateTime time = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);

		UsageRecord newRecord1 = new UsageRecord(null, "limit4", time.minusDays(60),
				time.minusDays(40), 200L, time.plusMonths(6));
		UsageRecord newRecord2 = new UsageRecord(null, "limit4", time.minusDays(30),
				time.minusDays(10), 100L, time.plusMonths(3));

		Product product = new Product("product4");
		Feature feature = new Feature(product, "feature4");
		IndividualUser user = new IndividualUser("user4");

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit4",
				time.minusDays(60), time.minusDays(10));
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		context.addUpdatedUsageRecords(List.of(newRecord1, newRecord2));

		repository.updateUsageRecords(context);

		testFilteringLogic(time.minusDays(60), time.minusDays(30), 2);
		testFilteringLogic(time.minusDays(60), time.minusDays(10), 2);
		testFilteringLogic(time.minusDays(61), time.minusDays(29), 2);
	}

	private void testFilteringLogic(ZonedDateTime startDate, ZonedDateTime endDate,
			int expectedRecordCount) throws Exception {
		ZonedDateTime normalizedStartDate = startDate.isBefore(endDate) ? startDate : endDate;
		ZonedDateTime normalizedEndDate = startDate.isBefore(endDate) ? endDate : startDate;

		Product product = new Product("product4");
		Feature feature = new Feature(product, "feature4");
		IndividualUser user = new IndividualUser("user4");

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit4",
				normalizedStartDate, normalizedEndDate);
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records).hasSize(expectedRecordCount);
		for (UsageRecord record : records) {
			assertThat(record.startTime()).isBetween(normalizedStartDate, normalizedEndDate);
		}
	}

	private void checkInsertedRecord(ZonedDateTime expectedStart, ZonedDateTime expectedEnd) throws Exception {
		String selectQuery = "SELECT window_start, window_end FROM " +
				repository.getFullTableName() + " ORDER BY usage_id DESC LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				java.sql.ResultSet resultSet = statement.executeQuery(selectQuery)) {

			assertThat(resultSet.next()).isTrue();
			ZonedDateTime actualStart = resultSet.getTimestamp("window_start")
					.toInstant().atZone(ZoneOffset.UTC);
			ZonedDateTime actualEnd = resultSet.getTimestamp("window_end")
					.toInstant().atZone(ZoneOffset.UTC);

			assertThat(actualStart).isEqualTo(expectedStart.withZoneSameInstant(ZoneOffset.UTC));
			assertThat(actualEnd).isEqualTo(expectedEnd.withZoneSameInstant(ZoneOffset.UTC));
		}
	}

	@Test
	void testTimezoneConversions() throws Exception {
		ZoneId vancouverTimeZone = ZoneId.of("America/Vancouver");
		ZonedDateTime windowStartVancouver = ZonedDateTime.of(2024, 10, 8, 17, 15, 34, 0, vancouverTimeZone);
		ZonedDateTime windowEndVancouver = windowStartVancouver.plusHours(1);
		UsageRecord newRecord = new UsageRecord(null, "limit5", windowStartVancouver,
				windowEndVancouver, 200L, windowEndVancouver.plusDays(1));

		Product product = new Product("product5");
		Feature feature = new Feature(product, "feature5");
		IndividualUser user = new IndividualUser("user5");

		RecordSearchCriteria criteria = new RecordSearchCriteria("limit5",
				windowStartVancouver, windowEndVancouver);
		LimitTrackingContext context = new LimitTrackingContext(feature, user, List.of(criteria));
		context.addUpdatedUsageRecords(List.of(newRecord));
		repository.updateUsageRecords(context);

		context = new LimitTrackingContext(feature, user, List.of(criteria));
		repository.loadUsageData(context);

		List<UsageRecord> records = context.getCurrentUsageRecords();
		assertThat(records).hasSize(1);

		UsageRecord insertedRecord = records.get(0);
		ZonedDateTime expectedStartUTC = windowStartVancouver.withZoneSameInstant(ZoneOffset.UTC);
		ZonedDateTime expectedEndUTC = windowEndVancouver.withZoneSameInstant(ZoneOffset.UTC);
		assertThat(insertedRecord.startTime()).isEqualTo(expectedStartUTC);
		assertThat(insertedRecord.endTime()).isEqualTo(expectedEndUTC);
	}
}
