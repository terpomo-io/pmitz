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

package io.terpomo.pmitz.limits.usage.repository.impl;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

public class JDBCUsageRepository implements UsageRepository {

	private final DataSource dataSource;
	private final String schemaName;
	private final String tableName;

	public JDBCUsageRepository(DataSource dataSource, String schemaName, String tableName) {
		this.dataSource = dataSource;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}

	public String getFullTableName() {
		return schemaName + "." + tableName;
	}

	@Override
	public void loadUsageData(LimitTrackingContext limitTrackingContext) {
		String query = "SELECT usage_id, limit_id, window_start, window_end, expiration_date, updated_at, units " +
				"FROM " + getFullTableName() + " WHERE ";
		List<String> conditions = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		try (Connection connection = dataSource.getConnection()) {

			for (RecordSearchCriteria criteria : limitTrackingContext.getSearchCriteria()) {
				if (criteria.limitId() != null) {
					conditions.add("limit_id = ?");
					parameters.add(criteria.limitId());
				}
				if (criteria.windowStart() != null) {
					conditions.add("window_start >= ?");
					parameters.add(Timestamp.valueOf(criteria.windowStart().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
				}
				if (criteria.windowEnd() != null) {
					conditions.add("(window_end <= ? OR window_end IS NULL)");
					parameters.add(Timestamp.valueOf(criteria.windowEnd().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()));
				}
			}

			if (conditions.isEmpty()) {
				System.out.println("No search criteria provided, returning no records.");
				limitTrackingContext.addCurrentUsageRecords(new ArrayList<>());
				return;
			}

			String finalQuery = query + String.join(" AND ", conditions);
			System.out.println("Executing Query: " + finalQuery);

			try (PreparedStatement statement = connection.prepareStatement(finalQuery)) {
				for (int i = 0; i < parameters.size(); i++) {
					statement.setObject(i + 1, parameters.get(i));
				}

				try (ResultSet resultSet = statement.executeQuery()) {
					List<UsageRecord> loadedRecords = new ArrayList<>();
					while (resultSet.next()) {
						UsageRecord usageRecord = mapResultSetToUsageRecord(resultSet);
						loadedRecords.add(usageRecord);
					}
					limitTrackingContext.addCurrentUsageRecords(loadedRecords);
				}
			}

		} catch (SQLException e) {
			throw new RuntimeException("Failed to load usage data", e);
		}
	}

	private UsageRecord mapResultSetToUsageRecord(ResultSet resultSet) throws SQLException {
		long usageId = resultSet.getLong("usage_id");
		String limitId = resultSet.getString("limit_id");

		// Convert result to UTC ZonedDateTime
		ZonedDateTime windowStart = resultSet.getTimestamp("window_start") != null
				? resultSet.getTimestamp("window_start").toLocalDateTime().atZone(ZoneOffset.UTC)
				: null;

		ZonedDateTime windowEnd = resultSet.getTimestamp("window_end") != null
				? resultSet.getTimestamp("window_end").toLocalDateTime().atZone(ZoneOffset.UTC)
				: null;

		ZonedDateTime expirationDate = resultSet.getTimestamp("expiration_date") != null
				? resultSet.getTimestamp("expiration_date").toLocalDateTime().atZone(ZoneOffset.UTC)
				: null;

		System.out.println("Loaded record: UsageId=" + usageId +
				", LimitId=" + limitId +
				", StartTime=" + windowStart +
				", EndTime=" + windowEnd +
				", ExpirationDate=" + expirationDate);

		JDBCUsageRecordRepoMetadata repoMetadata = new JDBCUsageRecordRepoMetadata(usageId, windowStart);

		return new UsageRecord(repoMetadata, limitId, windowStart, windowEnd, resultSet.getLong("units"), expirationDate);
	}

	@Override
	public void updateUsageRecords(LimitTrackingContext limitTrackingContext) {
		try (Connection connection = dataSource.getConnection()) {
			String updateQuery = "UPDATE " + getFullTableName() + " SET feature_id = ?, product_id = ?, user_grouping = ?, limit_id = ?, " +
					"window_start = ?, window_end = ?, units = ?, expiration_date = ? WHERE usage_id = ?";
			String insertQuery = "INSERT INTO " + getFullTableName() + " (feature_id, product_id, user_grouping, limit_id, " +
					"window_start, window_end, units, expiration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			String selectQuery = "SELECT usage_id FROM " + getFullTableName() + " WHERE limit_id = ? AND (window_start = ? OR window_start IS NULL)";

			try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
					PreparedStatement insertStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
					PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

				connection.setAutoCommit(false);

				for (UsageRecord record : limitTrackingContext.getUpdatedUsageRecords()) {
					System.out.println("Processing record: LimitId=" + record.limitId() +
							", StartTime=" + record.startTime() +
							", EndTime=" + record.endTime() +
							", Units=" + record.units() +
							", ExpirationDate=" + record.expirationDate());

					// Convert ZonedDateTime to LocalDateTime for UTC storage, if not null
					LocalDateTime startTime = record.startTime() != null
							? record.startTime().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toLocalDateTime()
							: null;
					LocalDateTime endTime = record.endTime() != null
							? record.endTime().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toLocalDateTime()
							: null;
					LocalDateTime expirationDate = record.expirationDate() != null
							? record.expirationDate().withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toLocalDateTime()
							: null;

					System.out.println("Converted StartTime (UTC): " + startTime);
					System.out.println("Converted EndTime (UTC): " + endTime);
					System.out.println("Converted ExpirationDate (UTC): " + expirationDate);

					// Set limitId and startTime (handle null startTime with SQL logic)
					selectStatement.setString(1, record.limitId());
					setNullableTimestamp(selectStatement, 2, startTime); // Handle null case with helper method

					// Execute the select query
					try (ResultSet resultSet = selectStatement.executeQuery()) {
						if (resultSet.next()) {
							long usageId = resultSet.getLong("usage_id");
							System.out.println("Updating existing record with usageId: " + usageId);

							updateStatement.setString(1, limitTrackingContext.getFeature().getFeatureId());
							updateStatement.setString(2, limitTrackingContext.getFeature().getProduct().getProductId());
							updateStatement.setString(3, limitTrackingContext.getUserGrouping().getId());
							updateStatement.setString(4, record.limitId());
							setNullableTimestamp(updateStatement, 5, startTime);  // Handle null case with helper method
							setNullableTimestamp(updateStatement, 6, endTime);    // Handle null case with helper method
							updateStatement.setLong(7, record.units());
							setNullableTimestamp(updateStatement, 8, expirationDate); // Handle null expiration date
							updateStatement.setLong(9, usageId);

							updateStatement.addBatch();
						} else {
							System.out.println("No existing record found for limitId: " + record.limitId() +
									" and startTime: " + startTime + ". Inserting new record.");

							insertStatement.setString(1, limitTrackingContext.getFeature().getFeatureId());
							insertStatement.setString(2, limitTrackingContext.getFeature().getProduct().getProductId());
							insertStatement.setString(3, limitTrackingContext.getUserGrouping().getId());
							insertStatement.setString(4, record.limitId());
							setNullableTimestamp(insertStatement, 5, startTime);  // Handle null case with helper method
							setNullableTimestamp(insertStatement, 6, endTime);    // Handle null case with helper method
							insertStatement.setLong(7, record.units());
							setNullableTimestamp(insertStatement, 8, expirationDate); // Handle null expiration date

							insertStatement.addBatch();
						}
					}
				}

				updateStatement.executeBatch();
				insertStatement.executeBatch();
				connection.commit();
			}

		} catch (SQLException e) {
			throw new RuntimeException("Failed to update usage records", e);
		}
	}

	private void setNullableTimestamp(PreparedStatement statement, int parameterIndex, LocalDateTime dateTime) throws SQLException {
		if (dateTime != null) {
			statement.setTimestamp(parameterIndex, Timestamp.valueOf(dateTime));
		} else {
			statement.setNull(parameterIndex, Types.TIMESTAMP);
		}
	}

	public void deleteOldRecords(LocalDateTime expirationDate) {
		try (Connection connection = dataSource.getConnection()) {
			String deleteQuery = "DELETE FROM " + getFullTableName() + " WHERE expiration_date <= ?";

			try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
				statement.setObject(1, Timestamp.valueOf(expirationDate.atZone(ZoneOffset.UTC).toLocalDateTime()));
				statement.executeUpdate();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to delete old records", e);
		}
	}
}
