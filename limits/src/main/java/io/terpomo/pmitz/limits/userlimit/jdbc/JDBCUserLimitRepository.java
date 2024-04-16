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

package io.terpomo.pmitz.limits.userlimit.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.types.CalendarPeriodRateLimit;
import io.terpomo.pmitz.core.limits.types.CountLimit;
import io.terpomo.pmitz.core.limits.types.SlidingWindowRateLimit;
import io.terpomo.pmitz.limits.userlimit.UserLimitRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public class JDBCUserLimitRepository implements UserLimitRepository {

	private final DataSource dataSource;
	private final String schemaName;
	private final String tableName;

	public JDBCUserLimitRepository(DataSource dataSource, String schemaName, String tableName) {

		this.dataSource = dataSource;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}

	@Override
	public Optional<UsageLimit> findUsageLimit(Feature feature, String usageLimitId, UserGrouping userGroup) {

		this.validateFeature(feature);
		this.validateNotNull(usageLimitId, "limitId");
		this.validateUserGrouping(userGroup);

		UsageLimit usageLimit = null;

		String query = String.format(
						"""
						SELECT * FROM %s.%s
							WHERE limit_id = ? AND feature_id = ? AND user_group_id = ?
						""",
						this.schemaName, this.tableName);

		try (Connection connection = this.dataSource.getConnection();
			PreparedStatement statement = connection.prepareStatement(query)) {

			statement.setString(1, usageLimitId);
			statement.setString(2, feature.getFeatureId());
			statement.setString(3, userGroup.getId());

			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					usageLimit = mapResultSetToUsageLimit(resultSet);
				}
			}
		}
		catch (SQLException | IllegalArgumentException ex) {
			throw new RepositoryException("Error finding limit", ex);
		}

		return Optional.ofNullable(usageLimit);
	}

	public void addUsageLimit(Feature feature, UsageLimit usageLimit, UserGrouping userGroup) {

		this.validateFeature(feature);
		this.validateUsageLimit(usageLimit);
		this.validateUserGrouping(userGroup);

		try {
			String query = String.format(
							"""
							INSERT INTO %s.%s (
								limit_id, feature_id, user_group_id, limit_type,
								limit_value, limit_unit, limit_interval, limit_duration
							) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
							""",
							this.schemaName, this.tableName);

			try (Connection conn = this.dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)) {

				stmt.setString(1, usageLimit.getId());
				stmt.setString(2, feature.getFeatureId());
				stmt.setString(3, userGroup.getId());
				stmt.setString(4, usageLimit.getClass().getSimpleName());
				stmt.setLong(5, usageLimit.getValue());
				stmt.setString(6, usageLimit.getUnit());

				if (usageLimit instanceof SlidingWindowRateLimit slidingWindowRateLimit) {
					stmt.setString(7, slidingWindowRateLimit.getInterval().name());
					stmt.setInt(8, slidingWindowRateLimit.getDuration());
				}
				else if (usageLimit instanceof CalendarPeriodRateLimit calendarPeriodRateLimit) {
					stmt.setString(7, calendarPeriodRateLimit.getPeriodicity().name());
					stmt.setInt(8, calendarPeriodRateLimit.getDuration());
				}
				else {
					stmt.setString(7, null);
					stmt.setInt(8, 0);
				}

				stmt.executeUpdate();
			}
		}
		catch (SQLException ex) {
			throw new RepositoryException("Error adding limit", ex);
		}
	}

	public void updateUsageLimit(Feature feature, UsageLimit usageLimit, UserGrouping userGroup) {

		this.validateFeature(feature);
		this.validateUsageLimit(usageLimit);
		this.validateUserGrouping(userGroup);

		try {
			String query = String.format(
							"""
							UPDATE %s.%s
								SET limit_value = ?, limit_unit = ?, limit_interval = ?, limit_duration = ?
								WHERE limit_id = ? AND feature_id = ? AND user_group_id = ?
							""",
							this.schemaName, this.tableName);

			try (Connection conn = this.dataSource.getConnection();

				PreparedStatement stmt = conn.prepareStatement(query)) {
				int idx = 1;
				stmt.setLong(idx++, usageLimit.getValue());
				stmt.setString(idx++, usageLimit.getUnit());

				if (usageLimit instanceof SlidingWindowRateLimit slidingWindowRateLimit) {
					stmt.setString(idx++, slidingWindowRateLimit.getInterval().name());
					stmt.setInt(idx++, slidingWindowRateLimit.getDuration());
				}
				else if (usageLimit instanceof CalendarPeriodRateLimit calendarPeriodRateLimit) {
					stmt.setString(idx++, calendarPeriodRateLimit.getPeriodicity().name());
					stmt.setInt(idx++, calendarPeriodRateLimit.getDuration());
				}
				else {
					stmt.setString(idx++, null);
					stmt.setInt(idx++, 0);
				}

				stmt.setString(idx++, usageLimit.getId());
				stmt.setString(idx++, feature.getFeatureId());
				stmt.setString(idx, userGroup.getId());

				stmt.executeUpdate();
			}
		}
		catch (SQLException ex) {
			throw new RepositoryException("Error updating limit", ex);
		}

	}

	public void deleteUsageLimit(Feature feature, String usageLimitId, UserGrouping userGroup) {

		this.validateFeature(feature);
		this.validateNotNull(usageLimitId, "limitId");
		this.validateUserGrouping(userGroup);

		try {
			String query = String.format(
							"""
							DELETE FROM %s.%s
								WHERE limit_id = ? AND feature_id = ? AND user_group_id = ?
							""",
							this.schemaName, this.tableName);

			try (Connection conn = this.dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)) {

				stmt.setString(1, usageLimitId);
				stmt.setString(2, feature.getFeatureId());
				stmt.setString(3, userGroup.getId());

				stmt.executeUpdate();
			}
		}
		catch (SQLException ex) {
			throw new RepositoryException("Error deleting limit", ex);
		}
	}


	private UsageLimit mapResultSetToUsageLimit(ResultSet resultSet) throws SQLException {

		UsageLimit usageLimit;

		String limitId = resultSet.getString("limit_id");
		String limitUnit = resultSet.getString("limit_unit");
		long limitValue = resultSet.getLong("limit_value");
		String limitType = resultSet.getString("limit_type");

		switch (limitType) {
			case "CalendarPeriodRateLimit":
				String intervalName = resultSet.getString("limit_interval");
				CalendarPeriodRateLimit.Periodicity periodicity = CalendarPeriodRateLimit.Periodicity.valueOf(intervalName);
				CalendarPeriodRateLimit calendarPeriodRateLimit = new CalendarPeriodRateLimit(limitId, limitValue, periodicity);
				calendarPeriodRateLimit.setUnit(limitUnit);
				usageLimit = calendarPeriodRateLimit;
				break;

			case "CountLimit":
				CountLimit countLimit = new CountLimit(limitId, limitValue);
				countLimit.setUnit(limitUnit);
				usageLimit = countLimit;
				break;

			case "SlidingWindowRateLimit":
				ChronoUnit interval = ChronoUnit.valueOf(resultSet.getString("limit_interval"));
				int duration = resultSet.getInt("limit_duration");
				SlidingWindowRateLimit slidingWindowRateLimit = new SlidingWindowRateLimit(limitId, limitValue, interval, duration);
				slidingWindowRateLimit.setUnit(limitUnit);
				usageLimit = slidingWindowRateLimit;
				break;

			default:
				throw new IllegalArgumentException("Unknown limit type: " + limitType);
		}

		return usageLimit;
	}

	private void validateFeature(Feature feature) {

		if (feature == null) {
			throw new IllegalArgumentException(("The feature parameter cannot be null"));
		}
		if (feature.getFeatureId() == null) {
			throw new IllegalArgumentException("A feature must have a identifier");
		}
		if ((feature.getProduct() == null) || (feature.getProduct().getProductId() == null)) {
			throw new IllegalArgumentException("A feature must have a product with identifier");
		}
	}

	private void validateUsageLimit(UsageLimit usageLimit) {

		if (usageLimit == null) {
			throw new IllegalArgumentException(("The limit parameter cannot be null"));
		}
		if (usageLimit.getId() == null) {
			throw new IllegalArgumentException("A limit must have a identifier");
		}
	}

	private void validateUserGrouping(UserGrouping userGrouping) {

		if (userGrouping == null) {
			throw new IllegalArgumentException(("The user/group parameter cannot be null"));
		}
		if (userGrouping.getId() == null) {
			throw new IllegalArgumentException("A user or group of users must have a identifier");
		}
	}

	private void validateNotNull(Object object, String name) {
		if (object == null) {
			throw new IllegalArgumentException("The '" + name + "' parameter cannot be null");
		}
	}
}
