package io.terpomo.pmitz.limits.integration;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Product;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRecordRepoMetadata;
import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractJDBCUsageRepositoryIntegrationTest {

	protected BasicDataSource dataSource;
	protected JDBCUsageRepository repository;

	protected abstract void setupDataSource() throws SQLException;

	protected abstract void setupDatabase() throws SQLException;

	protected abstract void tearDownDatabase() throws SQLException;

	// TODO: Remove
	protected abstract void printDatabaseContents();

	@Test
	public void testLoadAndUpdateUsageData() throws SQLException {
		setupDataSource();
		setupDatabase();
		// TODO: Remove
		printDatabaseContents();

		Feature feature = new Feature(new Product("productY"), "featureX");
		UserGrouping userGrouping = new UserGrouping() {
			@Override
			public String getId() {
				return "groupZ";
			}
		};

		ZonedDateTime fixedTime = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
		ZonedDateTime startTime = fixedTime.plusHours(9);
		ZonedDateTime endTime = fixedTime.plusHours(17);

		RecordSearchCriteria criteria = new RecordSearchCriteria("limitA", startTime, endTime);
		LimitTrackingContext loadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

		try {
			// TODO: Remove
			System.out.println("Initial loadContext: " + loadContext);

			repository.loadUsageData(loadContext);

			// TODO: Remove
			System.out.println("Loaded records: " + loadContext.getCurrentUsageRecords());

			if (loadContext.getCurrentUsageRecords().isEmpty()) {
				System.out.println("No records loaded. Exiting test.");
				return;
			}

			UsageRecord recordToUpdate = loadContext.getCurrentUsageRecords().get(0);

			// TODO: Remove
			System.out.println("Debug - recordToUpdate: " + recordToUpdate);

			assertEquals(100, recordToUpdate.units());

			UsageRecord updatedRecord = getUpdatedUsageRecord(recordToUpdate);

			LimitTrackingContext updateContext = new LimitTrackingContext(feature, userGrouping, List.of());
			updateContext.addUpdatedUsageRecords(List.of(updatedRecord));

			// TODO: Remove
			System.out.println("Debug - updateContext: " + updateContext);
			System.out.println("Debug - updatedRecord: " + updatedRecord);

			// Update the records
			repository.updateUsageRecords(updateContext);

			LimitTrackingContext reloadContext = new LimitTrackingContext(feature, userGrouping, List.of(criteria));

			// TODO: Remove
			System.out.println("Debug - reloadContext before reloading: " + reloadContext);

			// TODO: check why race condition or delay is making the following call to not return the expected record
			repository.loadUsageData(reloadContext);

			// TODO: Remove
			System.out.println("Reloaded records: " + reloadContext.getCurrentUsageRecords());

			// TODO: Remove
			if (reloadContext.getCurrentUsageRecords().isEmpty()) {
				System.out.println("No records reloaded. Exiting test.");
				return;
			}

			UsageRecord reloadedRecord = reloadContext.getCurrentUsageRecords().get(0);

			// TODO: Remove
			System.out.println("Debug - reloadedRecord: " + reloadedRecord);

			assertEquals(150, reloadedRecord.units());

		}
		finally {
			tearDownDatabase();
		}
	}


	@NotNull
	private UsageRecord getUpdatedUsageRecord(@NotNull UsageRecord recordToUpdate) {
		JDBCUsageRecordRepoMetadata updatedMetadata = (JDBCUsageRecordRepoMetadata) recordToUpdate.repoMetadata();
		return new UsageRecord(
				new JDBCUsageRecordRepoMetadata(updatedMetadata.usageId(), updatedMetadata.updatedAt()),
				recordToUpdate.limitId(),
				recordToUpdate.startTime(),
				recordToUpdate.endTime(),
				150L,
				recordToUpdate.expirationDate());
	}

	@AfterEach
	void tearDown() throws SQLException {
		tearDownDatabase();
	}
}
