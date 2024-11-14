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

	private static final Logger logger = Logger.getLogger(JDBCUsageRepository.class.getName());

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
		StringBuilder query = new StringBuilder("SELECT usage_id, limit_id, window_start, window_end, expiration_date, units ")
				.append("FROM ").append(getFullTableName()).append(" WHERE feature_id = ? AND product_id = ? AND user_grouping = ?");

		List<Object> parameters = new ArrayList<>();
		parameters.add(limitTrackingContext.getFeature().getFeatureId());
		parameters.add(limitTrackingContext.getFeature().getProduct().getProductId());
		parameters.add(limitTrackingContext.getUserGrouping().getId());

		List<String> criteriaConditions = new ArrayList<>();

		for (RecordSearchCriteria criteria : limitTrackingContext.getSearchCriteria()) {
			List<String> conditions = new ArrayList<>();
			List<Object> criteriaParams = new ArrayList<>();

			if (criteria.limitId() != null) {
				conditions.add("limit_id = ?");
				criteriaParams.add(criteria.limitId());
			}

			if (criteria.windowStart() != null) {
				conditions.add("(window_end >= ? OR window_end IS NULL)");
				criteriaParams.add(Timestamp.from(criteria.windowStart().withZoneSameInstant(ZoneOffset.UTC).toInstant()));
			}

			if (criteria.windowEnd() != null) {
				conditions.add("(window_start <= ? OR window_start IS NULL)");
				criteriaParams.add(Timestamp.from(criteria.windowEnd().withZoneSameInstant(ZoneOffset.UTC).toInstant()));
			}

			if (!conditions.isEmpty()) {
				String criteriaCondition = "(" + String.join(" AND ", conditions) + ")";
				criteriaConditions.add(criteriaCondition);
				parameters.addAll(criteriaParams);
			}
		}

		if (!criteriaConditions.isEmpty()) {
			query.append(" AND (").append(String.join(" OR ", criteriaConditions)).append(")");
		} else {
			limitTrackingContext.addCurrentUsageRecords(new ArrayList<>());
			return;
		}

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(query.toString())) {

			for (int i = 0; i < parameters.size(); i++) {
				Object param = parameters.get(i);
				if (param instanceof Timestamp) {
					statement.setTimestamp(i + 1, (Timestamp) param);
				} else if (param instanceof String) {
					statement.setString(i + 1, (String) param);
				} else {
					statement.setObject(i + 1, param);
				}
			}

			try (ResultSet resultSet = statement.executeQuery()) {
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
				limitTrackingContext.addCurrentUsageRecords(loadedRecords);
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to load usage data", e);
		}
	}

	@Override
	public void updateUsageRecords(LimitTrackingContext limitTrackingContext) {
		StringBuilder updateQuery = new StringBuilder("UPDATE ").append(getFullTableName())
				.append(" SET feature_id = ?, product_id = ?, user_grouping = ?, limit_id = ?, ")
				.append("window_start = ?, window_end = ?, units = ?, expiration_date = ? WHERE usage_id = ?");

		StringBuilder insertQuery = new StringBuilder("INSERT INTO ").append(getFullTableName())
				.append(" (feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) ")
				.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

		try (Connection connection = dataSource.getConnection();
				PreparedStatement updateStatement = connection.prepareStatement(updateQuery.toString());
				PreparedStatement insertStatement = connection.prepareStatement(insertQuery.toString(), Statement.RETURN_GENERATED_KEYS)) {

			connection.setAutoCommit(false);

			for (UsageRecord record : limitTrackingContext.getUpdatedUsageRecords()) {
				if (record.limitId() == null) {
					logger.warning("Encountered a UsageRecord with null limitId. Skipping this record.");
					continue;
				}

				ZonedDateTime recordStartTimeUTC = record.startTime() != null ? record.startTime().withZoneSameInstant(ZoneOffset.UTC) : null;
				ZonedDateTime recordEndTimeUTC = record.endTime() != null ? record.endTime().withZoneSameInstant(ZoneOffset.UTC) : null;
				ZonedDateTime recordExpirationDateUTC = record.expirationDate() != null ? record.expirationDate().withZoneSameInstant(ZoneOffset.UTC) : null;

				JDBCUsageRecordRepoMetadata metadata = (JDBCUsageRecordRepoMetadata) record.repoMetadata();

				long usageId = (metadata != null) ? metadata.usageId() : 0;

				if (usageId > 0) {
					// Existing record found; prepare for update
					updateStatement.clearParameters();
					int paramIndex = 1;
					updateStatement.setString(paramIndex++, limitTrackingContext.getFeature().getFeatureId());
					updateStatement.setString(paramIndex++, limitTrackingContext.getFeature().getProduct().getProductId());
					updateStatement.setString(paramIndex++, limitTrackingContext.getUserGrouping().getId());
					updateStatement.setString(paramIndex++, record.limitId());

					if (recordStartTimeUTC != null) {
						updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordStartTimeUTC.toInstant()));
					} else {
						updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
					}

					if (recordEndTimeUTC != null) {
						updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordEndTimeUTC.toInstant()));
					} else {
						updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
					}

					updateStatement.setLong(paramIndex++, record.units());

					if (recordExpirationDateUTC != null) {
						updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordExpirationDateUTC.toInstant()));
					} else {
						updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
					}

					updateStatement.setLong(paramIndex++, usageId);

					updateStatement.addBatch();
				} else {
					// Build select query dynamically
					StringBuilder selectQuery = new StringBuilder("SELECT usage_id FROM ").append(getFullTableName())
							.append(" WHERE limit_id = ?");

					List<Object> selectParams = new ArrayList<>();
					selectParams.add(record.limitId());

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
						// Set parameters for selectStatement
						int paramIdx = 1;
						for (Object param : selectParams) {
							if (param instanceof Timestamp) {
								selectStatement.setTimestamp(paramIdx++, (Timestamp) param);
							} else {
								selectStatement.setString(paramIdx++, param.toString());
							}
						}

						try (ResultSet resultSet = selectStatement.executeQuery()) {
							if (resultSet.next()) {
								// Existing record found; prepare for update
								usageId = resultSet.getLong("usage_id");

								updateStatement.clearParameters();
								int paramIndex = 1;
								updateStatement.setString(paramIndex++, limitTrackingContext.getFeature().getFeatureId());
								updateStatement.setString(paramIndex++, limitTrackingContext.getFeature().getProduct().getProductId());
								updateStatement.setString(paramIndex++, limitTrackingContext.getUserGrouping().getId());
								updateStatement.setString(paramIndex++, record.limitId());

								if (recordStartTimeUTC != null) {
									updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordStartTimeUTC.toInstant()));
								} else {
									updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								if (recordEndTimeUTC != null) {
									updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordEndTimeUTC.toInstant()));
								} else {
									updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								updateStatement.setLong(paramIndex++, record.units());

								if (recordExpirationDateUTC != null) {
									updateStatement.setTimestamp(paramIndex++, Timestamp.from(recordExpirationDateUTC.toInstant()));
								} else {
									updateStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								updateStatement.setLong(paramIndex++, usageId);

								updateStatement.addBatch();
							} else {
								// No existing record; prepare for insert
								insertStatement.clearParameters();
								int paramIndex = 1;
								insertStatement.setString(paramIndex++, limitTrackingContext.getFeature().getFeatureId());
								insertStatement.setString(paramIndex++, limitTrackingContext.getFeature().getProduct().getProductId());
								insertStatement.setString(paramIndex++, limitTrackingContext.getUserGrouping().getId());
								insertStatement.setString(paramIndex++, record.limitId());

								if (recordStartTimeUTC != null) {
									insertStatement.setTimestamp(paramIndex++, Timestamp.from(recordStartTimeUTC.toInstant()));
								} else {
									insertStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								if (recordEndTimeUTC != null) {
									insertStatement.setTimestamp(paramIndex++, Timestamp.from(recordEndTimeUTC.toInstant()));
								} else {
									insertStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								insertStatement.setLong(paramIndex++, record.units());

								if (recordExpirationDateUTC != null) {
									insertStatement.setTimestamp(paramIndex++, Timestamp.from(recordExpirationDateUTC.toInstant()));
								} else {
									insertStatement.setNull(paramIndex++, Types.TIMESTAMP);
								}

								insertStatement.addBatch();
							}
						}
					}
				}
			}

			// Execute batches and commit transaction
			try {
				updateStatement.executeBatch();
				insertStatement.executeBatch();
				connection.commit();
			} catch (SQLException e) {
				connection.rollback();
				throw new SQLException("Failed to update usage records", e);
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error updating usage records", e);
			throw new RuntimeException(e);
		}
	}

	public void deleteOldRecords(ZonedDateTime expirationDate) {
		String deleteQuery = "DELETE FROM " + getFullTableName() + " WHERE expiration_date <= ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
			ZonedDateTime expirationDateUTC = expirationDate.withZoneSameInstant(ZoneOffset.UTC);
			statement.setTimestamp(1, Timestamp.from(expirationDateUTC.toInstant()));
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to delete old records", e);
		}
	}
}
