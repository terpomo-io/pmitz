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

import io.terpomo.pmitz.limits.UsageRecord;
import io.terpomo.pmitz.limits.usage.repository.LimitTrackingContext;
import io.terpomo.pmitz.limits.usage.repository.RecordSearchCriteria;
import io.terpomo.pmitz.limits.usage.repository.UsageRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private List<UsageRecord> loadUsageRecords(Connection connection, LimitTrackingContext limitTrackingContext) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT usage_id, limit_id, window_start, window_end, expiration_date, units ")
				.append("FROM ").append(getFullTableName()).append(" WHERE feature_id = ? AND product_id = ? AND user_grouping = ?");

		List<Object> parameters = buildLoadParameters(limitTrackingContext);

		List<String> criteriaConditions = buildCriteriaConditions(limitTrackingContext, parameters);

		if (!criteriaConditions.isEmpty()) {
			query.append(" AND (").append(String.join(" OR ", criteriaConditions)).append(")");
		} else {
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
		List<UsageRecord> loadedRecords = new ArrayList<>();
		while (resultSet.next()) {
			Timestamp windowStartTs = resultSet.getTimestamp("window_start");
			Timestamp windowEndTs = resultSet.getTimestamp("window_end");
			Timestamp expirationDateTs = resultSet.getTimestamp("expiration_date");

			ZonedDateTime windowStart = windowStartTs != null ? windowStartTs.toInstant().atZone(ZoneOffset.UTC) : null;
			ZonedDateTime windowEnd = windowEndTs != null ? windowEndTs.toInstant().atZone(ZoneOffset.UTC) : null;
			ZonedDateTime expirationDate = expirationDateTs != null ? expirationDateTs.toInstant().atZone(ZoneOffset.UTC) : null;

			JDBCUsageRecordRepoMetadata repoMetadata = new JDBCUsageRecordRepoMetadata(resultSet.getLong("usage_id"), windowStart);
			UsageRecord usageRecord = new UsageRecord(
					repoMetadata,
					resultSet.getString("limit_id"),
					windowStart,
					windowEnd,
					resultSet.getLong("units"),
					expirationDate
			);

			loadedRecords.add(usageRecord);
		}
		return loadedRecords;
	}

	private List<Object> buildLoadParameters(LimitTrackingContext limitTrackingContext) {
		List<Object> parameters = new ArrayList<>();
		parameters.add(limitTrackingContext.getFeature().getFeatureId());
		parameters.add(limitTrackingContext.getFeature().getProduct().getProductId());
		parameters.add(limitTrackingContext.getUserGrouping().getId());
		return parameters;
	}

	private List<String> buildCriteriaConditions(LimitTrackingContext limitTrackingContext, List<Object> parameters) {
		List<String> criteriaConditions = new ArrayList<>();
		for (RecordSearchCriteria criteria : limitTrackingContext.getSearchCriteria()) {
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
				criteriaConditions.add("(" + String.join(" AND ", conditions) + ")");
			}
		}
		return criteriaConditions;
	}

	private void setParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
		for (int i = 0; i < parameters.size(); i++) {
			statement.setObject(i + 1, parameters.get(i));
		}
	}



	@Override
	public void loadUsageData(LimitTrackingContext limitTrackingContext) {
		try (Connection connection = dataSource.getConnection()) {
			List<UsageRecord> loadedRecords = loadUsageRecords(connection, limitTrackingContext);
			limitTrackingContext.addCurrentUsageRecords(loadedRecords);
		} catch (SQLException ex) {
			LOGGER.log(Level.SEVERE, "Failed to load usage data", ex);
			throw new UsageRepositoryException("Failed to load usage data", ex);
		}
	}

	private void updateUsageRecord(PreparedStatement updateStatement, UsageRecord usageRecord, LimitTrackingContext limitTrackingContext, long usageId) throws SQLException {
		updateStatement.clearParameters();
		int parameterIndex = 1;
		updateStatement.setString(parameterIndex++, limitTrackingContext.getFeature().getFeatureId());
		updateStatement.setString(parameterIndex++, limitTrackingContext.getFeature().getProduct().getProductId());
		updateStatement.setString(parameterIndex++, limitTrackingContext.getUserGrouping().getId());
		updateStatement.setString(parameterIndex++, usageRecord.limitId());

		ZonedDateTime recordStartTime = usageRecord.startTime();
		updateStatement.setTimestamp(parameterIndex++, recordStartTime != null ? Timestamp.from(recordStartTime.toInstant()) : null);

		ZonedDateTime recordEndTime = usageRecord.endTime();
		updateStatement.setTimestamp(parameterIndex++, recordEndTime != null ? Timestamp.from(recordEndTime.toInstant()) : null);

		updateStatement.setLong(parameterIndex++, usageRecord.units());

		ZonedDateTime recordExpirationDate = usageRecord.expirationDate();
		updateStatement.setTimestamp(parameterIndex++, recordExpirationDate != null ? Timestamp.from(recordExpirationDate.toInstant()) : null);

		updateStatement.setLong(parameterIndex, usageId);

		updateStatement.addBatch();
	}

	private void insertUsageRecord(PreparedStatement insertStatement, UsageRecord usageRecord, LimitTrackingContext limitTrackingContext) throws SQLException {
		insertStatement.clearParameters();
		int parameterIndex = 1;
		insertStatement.setString(parameterIndex++, limitTrackingContext.getFeature().getFeatureId());
		insertStatement.setString(parameterIndex++, limitTrackingContext.getFeature().getProduct().getProductId());
		insertStatement.setString(parameterIndex++, limitTrackingContext.getUserGrouping().getId());  // Corrected from insertStatement to updateStatement
		insertStatement.setString(parameterIndex++, usageRecord.limitId());

		ZonedDateTime recordStartTime = usageRecord.startTime();
		insertStatement.setTimestamp(parameterIndex++, recordStartTime != null ? Timestamp.from(recordStartTime.toInstant()) : null);

		ZonedDateTime recordEndTime = usageRecord.endTime();
		insertStatement.setTimestamp(parameterIndex++, recordEndTime != null ? Timestamp.from(recordEndTime.toInstant()) : null);

		insertStatement.setLong(parameterIndex++, usageRecord.units());

		ZonedDateTime recordExpirationDate = usageRecord.expirationDate();
		insertStatement.setTimestamp(parameterIndex, recordExpirationDate != null ? Timestamp.from(recordExpirationDate.toInstant()) : null);

		insertStatement.addBatch();
	}



	private long findRecordId(Connection connection, UsageRecord usageRecord, LimitTrackingContext limitTrackingContext) throws SQLException {
		StringBuilder selectQuery = new StringBuilder("SELECT usage_id FROM ").append(getFullTableName())
				.append(" WHERE limit_id = ?");

		List<Object> selectParams = new ArrayList<>();
		selectParams.add(usageRecord.limitId());
		ZonedDateTime recordStartTimeUTC = usageRecord.startTime();
		if (recordStartTimeUTC != null) {
			selectQuery.append(" AND window_start = ?");
			selectParams.add(Timestamp.from(recordStartTimeUTC.toInstant()));
		} else {
			selectQuery.append(" AND window_start IS NULL");
		}
		selectQuery.append(" AND feature_id = ? AND product_id = ? AND user_grouping = ?");
		selectParams.add(limitTrackingContext.getFeature().getFeatureId());
		selectParams.add(limitTrackingContext.getFeature().getProduct().getProductId());
		selectParams.add(limitTrackingContext.getUserGrouping().getId());

		try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery.toString())) {
			for (int i = 0; i < selectParams.size(); i++) {
				selectStatement.setObject(i + 1, selectParams.get(i));
			}

			try (ResultSet resultSet = selectStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("usage_id");
				}
			}
		}
		return -1;
	}

	private void processUsageRecordUpdates(Connection connection, List<UsageRecord> usageRecords, LimitTrackingContext context) throws SQLException {

		String updateQuery = "UPDATE " + getFullTableName() +
				" SET feature_id = ?, product_id = ?, user_grouping = ?, limit_id = ?, " +
				"window_start = ?, window_end = ?, units = ?, expiration_date = ? WHERE usage_id = ?";

		String insertQuery = "INSERT INTO " + getFullTableName() +
				" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {

			for (UsageRecord usageRecord : usageRecords) {
				if (usageRecord.limitId() == null) {
					LOGGER.warning("UsageRecord with null limitId encountered. Skipping.");
					continue;
				}

				JDBCUsageRecordRepoMetadata metadata = (JDBCUsageRecordRepoMetadata) usageRecord.repoMetadata();
				long usageId = (metadata != null) ? metadata.usageId() : -1;

				if (usageId != -1) {
					updateUsageRecord(updateStatement, usageRecord, context, usageId);

				} else {
					usageId = findRecordId(connection, usageRecord, context);
					if (usageId != -1) {
						updateUsageRecord(updateStatement, usageRecord, context, usageId);

					} else {
						insertUsageRecord(insertStatement, usageRecord, context);

					}
				}
			}

			updateStatement.executeBatch();
			insertStatement.executeBatch();

		}
	}

	@Override
	public void updateUsageRecords(LimitTrackingContext limitTrackingContext) {
		try (Connection connection = dataSource.getConnection()) {
			performUpdatesInTransaction(connection, limitTrackingContext);
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Error updating usage records: ", e);
			throw new UsageRepositoryException("Error updating usage records", e);
		}
	}



	private void performUpdatesInTransaction(Connection connection, LimitTrackingContext limitTrackingContext) throws SQLException {
		connection.setAutoCommit(false);
		try {
			processUsageRecordUpdates(connection, limitTrackingContext.getUpdatedUsageRecords(), limitTrackingContext);
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			LOGGER.log(Level.SEVERE, "Rolling back transaction due to error: ", e);
			throw new UsageRepositoryException("Failed to update/insert usage records", e);
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
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
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Failed to connect to the database for deleting records: ", e);
			throw new UsageRepositoryException("Failed to connect to the database for deleting records", e);
		}
	}



	private void deleteRecordsInTransaction(Connection connection, ZonedDateTime expirationDate) throws SQLException {
		connection.setAutoCommit(false);
		try {
			deleteRecords(connection, expirationDate);
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			LOGGER.log(Level.SEVERE, "Failed to delete old records, rolling back transaction:", e);
			throw new UsageRepositoryException("Failed to delete old records", e);
		} finally {
			try {
				connection.setAutoCommit(true);
			} catch (SQLException ex) {
				LOGGER.log(Level.SEVERE, "Failed to reset auto-commit after deleting records: ", ex);
			}
		}
	}

}



class UsageRepositoryException extends RuntimeException {
	public UsageRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}
}