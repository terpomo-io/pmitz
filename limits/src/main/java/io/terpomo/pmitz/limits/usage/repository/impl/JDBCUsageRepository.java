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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

public class JDBCUsageRepository implements UsageRepository {

	private static final Logger LOGGER = Logger.getLogger(JDBCUsageRepository.class.getName());

	private final DataSource dataSource;
	private final String schemaName;
	private final String tableName;

	public JDBCUsageRepository(DataSource dataSource, String schemaName, String tableName) {
		this.dataSource = dataSource;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public String getFullTableName() {
		return schemaName + "." + tableName;
	}

	private List<UsageRecord> loadUsageRecords(Connection connection, LimitTrackingContext context)
			throws SQLException {
		StringBuilder query = new StringBuilder("SELECT usage_id, limit_id, window_start, window_end, expiration_date, units ")
				.append("FROM ").append(getFullTableName())
				.append(" WHERE feature_id = ? AND product_id = ? AND user_grouping = ?");

		List<Object> parameters = buildLoadParameters(context);
		List<String> criteriaConditions = buildCriteriaConditions(context, parameters);

		if (!criteriaConditions.isEmpty()) {
			query.append(" AND (").append(String.join(" OR ", criteriaConditions)).append(")");
		}
		else {
			return new ArrayList<>();
		}

		try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
			setParameters(statement, parameters);
			try (ResultSet resultSet = statement.executeQuery()) {
				return extractUsageRecords(resultSet);
			}
		}
	}

	private List<UsageRecord> extractUsageRecords(ResultSet resultSet) throws SQLException {
		List<UsageRecord> records = new ArrayList<>();
		while (resultSet.next()) {
			Timestamp windowStartTs = resultSet.getTimestamp("window_start");
			Timestamp windowEndTs = resultSet.getTimestamp("window_end");
			Timestamp expirationDateTs = resultSet.getTimestamp("expiration_date");

			ZonedDateTime windowStart = (windowStartTs != null)
					? windowStartTs.toInstant().atZone(ZoneOffset.UTC)
					: null;
			ZonedDateTime windowEnd = (windowEndTs != null)
					? windowEndTs.toInstant().atZone(ZoneOffset.UTC)
					: null;
			ZonedDateTime expirationDate = (expirationDateTs != null)
					? expirationDateTs.toInstant().atZone(ZoneOffset.UTC)
					: null;

			JDBCUsageRecordRepoMetadata metadata = new JDBCUsageRecordRepoMetadata(
					resultSet.getLong("usage_id"), windowStart
			);

			UsageRecord usageRecord = new UsageRecord(
					metadata,
					resultSet.getString("limit_id"),
					windowStart,
					windowEnd,
					resultSet.getLong("units"),
					expirationDate
			);

			records.add(usageRecord);
		}
		return records;
	}

	private List<Object> buildLoadParameters(LimitTrackingContext context) {
		List<Object> parameters = new ArrayList<>();
		parameters.add(context.getFeature().getFeatureId());
		parameters.add(context.getFeature().getProduct().getProductId());
		parameters.add(context.getUserGrouping().getId());
		return parameters;
	}

	private List<String> buildCriteriaConditions(LimitTrackingContext context, List<Object> parameters) {
		List<String> conditionsList = new ArrayList<>();
		for (RecordSearchCriteria criteria : context.getSearchCriteria()) {
			List<String> conditions = new ArrayList<>();
			if (criteria.limitId() != null) {
				conditions.add("limit_id = ?");
				parameters.add(criteria.limitId());
			}
			if (criteria.windowStart() != null) {
				conditions.add("(window_end >= ? OR window_end IS NULL)");
				parameters.add(Timestamp.from(criteria.windowStart().toInstant()));
			}
			if (criteria.windowEnd() != null) {
				conditions.add("(window_start <= ? OR window_start IS NULL)");
				parameters.add(Timestamp.from(criteria.windowEnd().toInstant()));
			}
			if (!conditions.isEmpty()) {
				conditionsList.add("(" + String.join(" AND ", conditions) + ")");
			}
		}
		return conditionsList;
	}

	private void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
		for (int i = 0; i < parameters.size(); i++) {
			statement.setObject(i + 1, parameters.get(i));
		}
	}

	@Override
	public void loadUsageData(LimitTrackingContext context) {
		try (Connection connection = dataSource.getConnection()) {
			List<UsageRecord> records = loadUsageRecords(connection, context);
			context.addCurrentUsageRecords(records);
		}
		catch (SQLException ex) {
			throw new UsageRepositoryException("Failed to load usage data", ex);
		}
	}

	private void updateUsageRecord(PreparedStatement updateStatement, UsageRecord usageRecord,
			LimitTrackingContext context, long usageId) throws SQLException {
		updateStatement.clearParameters();
		int index = 1;
		updateStatement.setString(index++, context.getFeature().getFeatureId());
		updateStatement.setString(index++, context.getFeature().getProduct().getProductId());
		updateStatement.setString(index++, context.getUserGrouping().getId());
		updateStatement.setString(index++, usageRecord.limitId());

		ZonedDateTime startTime = usageRecord.startTime();
		updateStatement.setTimestamp(index++, (startTime != null)
				? Timestamp.from(startTime.toInstant())
				: null);

		ZonedDateTime endTime = usageRecord.endTime();
		updateStatement.setTimestamp(index++, (endTime != null)
				? Timestamp.from(endTime.toInstant())
				: null);

		updateStatement.setLong(index++, usageRecord.units());

		ZonedDateTime expirationDate = usageRecord.expirationDate();
		updateStatement.setTimestamp(index++, (expirationDate != null)
				? Timestamp.from(expirationDate.toInstant())
				: null);

		updateStatement.setLong(index, usageId);

		updateStatement.addBatch();
	}

	private void insertUsageRecord(PreparedStatement insertStatement, UsageRecord usageRecord,
			LimitTrackingContext context) throws SQLException {
		insertStatement.clearParameters();
		int index = 1;
		insertStatement.setString(index++, context.getFeature().getFeatureId());
		insertStatement.setString(index++, context.getFeature().getProduct().getProductId());
		insertStatement.setString(index++, context.getUserGrouping().getId());
		insertStatement.setString(index++, usageRecord.limitId());

		ZonedDateTime startTime = usageRecord.startTime();
		insertStatement.setTimestamp(index++, (startTime != null)
				? Timestamp.from(startTime.toInstant())
				: null);

		ZonedDateTime endTime = usageRecord.endTime();
		insertStatement.setTimestamp(index++, (endTime != null)
				? Timestamp.from(endTime.toInstant())
				: null);

		insertStatement.setLong(index++, usageRecord.units());

		ZonedDateTime expirationDate = usageRecord.expirationDate();
		insertStatement.setTimestamp(index, (expirationDate != null)
				? Timestamp.from(expirationDate.toInstant())
				: null);

		insertStatement.addBatch();
	}

	private long findRecordId(Connection connection, UsageRecord usageRecord,
			LimitTrackingContext context) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT usage_id FROM ").append(getFullTableName())
				.append(" WHERE limit_id = ?");

		List<Object> params = new ArrayList<>();
		params.add(usageRecord.limitId());

		ZonedDateTime startTime = usageRecord.startTime();
		if (startTime != null) {
			query.append(" AND window_start = ?");
			params.add(Timestamp.from(startTime.toInstant()));
		}
		else {
			query.append(" AND window_start IS NULL");
		}
		query.append(" AND feature_id = ? AND product_id = ? AND user_grouping = ?");
		params.add(context.getFeature().getFeatureId());
		params.add(context.getFeature().getProduct().getProductId());
		params.add(context.getUserGrouping().getId());

		try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
			setParameters(statement, params);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("usage_id");
				}
			}
		}
		return -1;
	}

	private void processUsageRecordUpdates(Connection connection, List<UsageRecord> usageRecords,
			LimitTrackingContext context) throws SQLException {
		String updateQuery = "UPDATE " + getFullTableName()
				+ " SET feature_id = ?, product_id = ?, user_grouping = ?, limit_id = ?, "
				+ "window_start = ?, window_end = ?, units = ?, expiration_date = ? WHERE usage_id = ?";

		String insertQuery = "INSERT INTO " + getFullTableName()
				+ " (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery,
						Statement.RETURN_GENERATED_KEYS)) {

			for (UsageRecord usageRecord : usageRecords) {
				if (usageRecord.limitId() == null) {
					LOGGER.warning("UsageRecord with null limitId encountered. Skipping.");
					continue;
				}

				if (usageRecord.startTime() != null && usageRecord.endTime() != null
						&& usageRecord.endTime().isBefore(usageRecord.startTime())) {
					throw new IllegalArgumentException("endTime cannot be before startTime in UsageRecord");
				}
				if (usageRecord.expirationDate() != null) {
					if (usageRecord.startTime() != null
							&& usageRecord.expirationDate().isBefore(usageRecord.startTime())) {
						throw new IllegalArgumentException("expirationDate cannot be before startTime in UsageRecord");
					}
					if (usageRecord.endTime() != null
							&& usageRecord.expirationDate().isBefore(usageRecord.endTime())) {
						throw new IllegalArgumentException("expirationDate cannot be before endTime in UsageRecord");
					}
				}

				JDBCUsageRecordRepoMetadata metadata = (JDBCUsageRecordRepoMetadata) usageRecord.repoMetadata();
				long usageId = (metadata != null) ? metadata.usageId() : -1;

				if (usageId != -1) {
					updateUsageRecord(updateStatement, usageRecord, context, usageId);
				}
				else {
					usageId = findRecordId(connection, usageRecord, context);
					if (usageId != -1) {
						updateUsageRecord(updateStatement, usageRecord, context, usageId);
					}
					else {
						insertUsageRecord(insertStatement, usageRecord, context);
					}
				}
			}

			updateStatement.executeBatch();
			insertStatement.executeBatch();
		}
	}

	@Override
	public void updateUsageRecords(LimitTrackingContext context) {
		try (Connection connection = dataSource.getConnection()) {
			performUpdatesInTransaction(connection, context);
		}
		catch (SQLException ex) {
			throw new UsageRepositoryException("Error updating usage records", ex);
		}
	}

	private void performUpdatesInTransaction(Connection connection, LimitTrackingContext context)
			throws SQLException {
		connection.setAutoCommit(false);
		try {
			processUsageRecordUpdates(connection, context.getUpdatedUsageRecords(), context);
			connection.commit();
		}
		catch (SQLException ex) {
			connection.rollback();
			throw new UsageRepositoryException("Failed to update/insert usage records", ex);
		}
		finally {
			try {
				connection.setAutoCommit(true);
			}
			catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, "Failed to restore auto-commit after transaction: ", ex);
			}
		}
	}

	private void deleteRecords(Connection connection, ZonedDateTime expirationDate) throws SQLException {
		String deleteQuery = "DELETE FROM " + getFullTableName() + " WHERE expiration_date <= ?";
		try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
			statement.setTimestamp(1, Timestamp.from(expirationDate.toInstant()));
			statement.executeUpdate();
		}
	}

	public void deleteOldRecords(ZonedDateTime expirationDate) {
		try (Connection connection = dataSource.getConnection()) {
			deleteRecordsInTransaction(connection, expirationDate);
		}
		catch (SQLException ex) {
			throw new UsageRepositoryException("Failed to connect to the database for deleting records", ex);
		}
	}

	private void deleteRecordsInTransaction(Connection connection, ZonedDateTime expirationDate)
			throws SQLException {
		connection.setAutoCommit(false);
		try {
			deleteRecords(connection, expirationDate);
			connection.commit();
		}
		catch (SQLException ex) {
			connection.rollback();
			throw new UsageRepositoryException("Failed to delete old records", ex);
		}
		finally {
			try {
				connection.setAutoCommit(true);
			}
			catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, "Failed to reset auto-commit after deleting records: ", ex);
			}
		}
	}
}
