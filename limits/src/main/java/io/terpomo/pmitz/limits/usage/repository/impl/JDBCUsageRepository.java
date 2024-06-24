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

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

	@Override
	public void loadUsageData(LimitTrackingContext limitTrackingContext) {
		String baseQuery = "SELECT * FROM " + schemaName + "." + tableName + " WHERE ";
		List<String> conditions = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		for (RecordSearchCriteria criteria : limitTrackingContext.getSearchCriteria()) {
			if (criteria.limitId() != null) {
				conditions.add("limit_id = ?");
				parameters.add(criteria.limitId());
			}
			if (criteria.windowStart() != null) {
				ZonedDateTime startZonedDateTime = criteria.windowStart().withZoneSameInstant(ZoneId.of("UTC"));
				Timestamp startTimestamp = Timestamp.from(startZonedDateTime.toInstant());
				conditions.add("window_start >= ?");
				parameters.add(startTimestamp);
				logTimestampDetails("Before adding to load query - Window Start", startZonedDateTime);
			}

			if (criteria.windowEnd() != null) {
				ZonedDateTime endZonedDateTime = criteria.windowEnd().withZoneSameInstant(ZoneId.of("UTC"));
				Timestamp endTimestamp = Timestamp.from(endZonedDateTime.toInstant());
				conditions.add("window_end <= ?");
				parameters.add(endTimestamp);
				logTimestampDetails("Before adding to load query - Window End", endZonedDateTime);
			}

		}

		String productId = limitTrackingContext.getFeature().getProduct().getProductId();
		if (productId != null) {
			conditions.add("product_id = ?");
			parameters.add(productId);
		}

		String finalQuery = baseQuery + String.join(" AND ", conditions);

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(finalQuery)) {

			for (int i = 0; i < parameters.size(); i++) {
				statement.setObject(i + 1, parameters.get(i));
			}

			// TODO: Remove
			String debugQuery = debugPreparedStatement(finalQuery, parameters);
			System.out.println("Debug SQL for loadUsageData: " + debugQuery);

			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					UsageRecord usageRecord = mapResultSetToUsageRecord(resultSet);
					limitTrackingContext.addCurrentUsageRecords(List.of(usageRecord));
				}
			}
		}
		catch (SQLException ex) {
			// TODO: Handle exceptions
		}
	}

	private UsageRecord mapResultSetToUsageRecord(ResultSet resultSet) throws SQLException {
		long usageId = resultSet.getLong("usage_id");
		String limitId = resultSet.getString("limit_id");

		Timestamp windowStartTimestamp = resultSet.getTimestamp("window_start");
		ZonedDateTime startZonedDateTime = windowStartTimestamp.toInstant().atZone(ZoneId.of("UTC"));

		Timestamp windowEndTimestamp = resultSet.getTimestamp("window_end");
		ZonedDateTime endZonedDateTime = windowEndTimestamp.toInstant().atZone(ZoneId.of("UTC"));

		Long units = resultSet.getLong("units");

		Timestamp expirationTimestamp = Timestamp.from(resultSet.getTimestamp("expiration_date").toInstant().atZone(ZoneId.of("UTC")).toInstant());
		ZonedDateTime expirationZonedDateTime = expirationTimestamp.toInstant().atZone(ZoneId.of("UTC"));

		Timestamp updatedAtTimestamp = resultSet.getTimestamp("updated_at");
		ZonedDateTime updatedAt = updatedAtTimestamp.toInstant().atZone(ZoneId.of("UTC"));


		JDBCUsageRecordRepoMetadata repoMetadata = new JDBCUsageRecordRepoMetadata(
				usageId,
				updatedAt
		);

		return new UsageRecord(
				repoMetadata,
				limitId,
				startZonedDateTime,
				endZonedDateTime,
				units,
				expirationZonedDateTime
		);
	}


	@Override
	public void updateUsageRecords(LimitTrackingContext limitTrackingContext) {
		String updateQuery = "UPDATE " + schemaName + "." + tableName + " SET feature_id = ?, product_id = ?, user_grouping = ?, limit_id = ?, window_start = ?, window_end = ?, units = ?, expiration_date = ? WHERE usage_id = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(updateQuery)) {

			for (UsageRecord record : limitTrackingContext.getUpdatedUsageRecords()) {
				statement.setString(1, limitTrackingContext.getFeature().getFeatureId());
				statement.setString(2, limitTrackingContext.getFeature().getProduct().getProductId());
				statement.setString(3, limitTrackingContext.getUserGrouping().getId());
				statement.setString(4, record.limitId());

				Timestamp startTimestamp = record.startTime() != null ? Timestamp.from(record.startTime().toInstant()) : null;
				statement.setTimestamp(5, startTimestamp);

				Timestamp endTimestamp = record.endTime() != null ? Timestamp.from(record.endTime().toInstant()) : null;
				statement.setTimestamp(6, endTimestamp);

				statement.setLong(7, record.units());

				Timestamp expirationTimestamp = record.expirationDate() != null ? Timestamp.from(record.expirationDate().toInstant()) : null;
				statement.setTimestamp(8, expirationTimestamp);

				if (record.repoMetadata() != null) {
					statement.setLong(9, ((JDBCUsageRecordRepoMetadata) record.repoMetadata()).usageId());
				} else {
					statement.setNull(9, JDBCType.NUMERIC.getVendorTypeNumber());
				}

				// TODO: Remove
				logTimestampDetails("Update - Window Start", record.startTime());
				logTimestampDetails("Update - Window End", record.endTime());
				logTimestampDetails("Update - Expiration Date", record.expirationDate());

				statement.addBatch();
			}

			statement.executeBatch();
		}
		catch (SQLException ex) {
			// TODO: Handle exceptions
			ex.printStackTrace();
		}
	}

	// TODO: Remove
	private void logTimestampDetails(String message, ZonedDateTime zonedDateTime) {
		if (zonedDateTime != null) {
			System.out.println(message + ": ZonedDateTime = " + zonedDateTime);
		}
		else {
			System.out.println(message + ": ZonedDateTime is null");
		}
	}

	// TODO: Remove
	private String debugPreparedStatement(String baseQuery, List<Object> parameters) {
		for (Object param : parameters) {
			String value = (param instanceof String) ? "'" + param + "'" : param.toString();
			baseQuery = baseQuery.replaceFirst("\\?", value);
		}
		return baseQuery;
	}
}
