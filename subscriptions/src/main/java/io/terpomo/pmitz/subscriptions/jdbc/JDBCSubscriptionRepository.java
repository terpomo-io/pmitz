/*
 * Copyright 2023-2025 the original author or authors.
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

package io.terpomo.pmitz.subscriptions.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import io.terpomo.pmitz.core.exception.RepositoryException;
import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;
import io.terpomo.pmitz.subscriptions.SubscriptionRepository;

public class JDBCSubscriptionRepository implements SubscriptionRepository {

	private final DataSource dataSource;
	private final String schemaName;
	private final String subscriptionTableName;
	private final String subscriptionPlanTableName;

	public JDBCSubscriptionRepository(DataSource dataSource, String schemaName, String subscriptionTableName,
			String subscriptionPlanTableName) {
		this.dataSource = dataSource;
		this.schemaName = schemaName;
		this.subscriptionTableName = subscriptionTableName;
		this.subscriptionPlanTableName = subscriptionPlanTableName;
	}

	@Override
	public void create(Subscription subscription) {
		validateSubscription(subscription);
		validateStatus(subscription.getStatus());

		Map<String, String> plansByProduct = Optional.ofNullable(subscription.getPlansByProduct())
				.orElseGet(Collections::emptyMap);
		validatePlans(plansByProduct);

		String subscriptionQuery = String.format(
				"INSERT INTO %s.%s (subscription_id, status, expiration_date) VALUES (?, ?, ?)",
				schemaName, subscriptionTableName);

		String planQuery = String.format(
				"INSERT INTO %s.%s (subscription_id, product_id, plan_id) VALUES (?, ?, ?)",
				schemaName, subscriptionPlanTableName);

		try (Connection conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			try {
				try (PreparedStatement subscriptionStmt = conn.prepareStatement(subscriptionQuery)) {
					subscriptionStmt.setString(1, subscription.getSubscriptionId());
					subscriptionStmt.setString(2, subscription.getStatus().name());
					subscriptionStmt.setTimestamp(3, toTimestamp(subscription.getExpirationDate()));
					subscriptionStmt.executeUpdate();
				}

				if (!plansByProduct.isEmpty()) {
					try (PreparedStatement planStmt = conn.prepareStatement(planQuery)) {
						for (Map.Entry<String, String> entry : plansByProduct.entrySet()) {
							planStmt.setString(1, subscription.getSubscriptionId());
							planStmt.setString(2, entry.getKey());
							planStmt.setString(3, entry.getValue());
							planStmt.addBatch();
						}
						planStmt.executeBatch();
					}
				}

				conn.commit();
			}
			catch (SQLException ex) {
				conn.rollback();
				throw ex;
			}
		}
		catch (SQLException ex) {
			throw new RepositoryException("Error creating subscription", ex);
		}
	}

	@Override
	public Optional<Subscription> find(String subscriptionId) {
		validateNotBlank(subscriptionId, "subscriptionId");

		String query = String.format(
				"SELECT subscription_id, status, expiration_date FROM %s.%s WHERE subscription_id = ?",
				schemaName, subscriptionTableName);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, subscriptionId);
			try (ResultSet resultSet = stmt.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				Subscription subscription = new Subscription(resultSet.getString("subscription_id"));
				String statusValue = resultSet.getString("status");
				if (statusValue != null) {
					subscription.setStatus(SubscriptionStatus.valueOf(statusValue));
				}
				Timestamp expiration = resultSet.getTimestamp("expiration_date");
				if (expiration != null) {
					subscription.setExpirationDate(expiration.toInstant().atZone(ZoneOffset.UTC));
				}
				subscription.setPlans(loadPlans(conn, subscriptionId));
				return Optional.of(subscription);
			}
		}
		catch (SQLException | IllegalArgumentException ex) {
			throw new RepositoryException("Error finding subscription", ex);
		}
	}

	@Override
	public void updateStatus(Subscription subscription, SubscriptionStatus newStatus) {
		validateSubscription(subscription);
		validateStatus(newStatus);

		String query = String.format(
				"UPDATE %s.%s SET status = ? WHERE subscription_id = ?",
				schemaName, subscriptionTableName);

		try (Connection conn = dataSource.getConnection();
				PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, newStatus.name());
			stmt.setString(2, subscription.getSubscriptionId());
			int rows = stmt.executeUpdate();
			if (rows == 0) {
				throw new RepositoryException("Subscription not found: " + subscription.getSubscriptionId());
			}
			subscription.setStatus(newStatus);
		}
		catch (SQLException ex) {
			throw new RepositoryException("Error updating subscription status", ex);
		}
	}

	private Map<String, String> loadPlans(Connection conn, String subscriptionId) throws SQLException {
		String query = String.format(
				"SELECT product_id, plan_id FROM %s.%s WHERE subscription_id = ?",
				schemaName, subscriptionPlanTableName);

		Map<String, String> plansByProduct = new HashMap<>();
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			stmt.setString(1, subscriptionId);
			try (ResultSet resultSet = stmt.executeQuery()) {
				while (resultSet.next()) {
					String productId = resultSet.getString("product_id");
					String planId = resultSet.getString("plan_id");
					plansByProduct.put(productId, planId);
				}
			}
		}
		return plansByProduct;
	}

	private Timestamp toTimestamp(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return Timestamp.from(dateTime.toInstant());
	}

	private void validateSubscription(Subscription subscription) {
		if (subscription == null) {
			throw new IllegalArgumentException("Subscription must not be null");
		}
		validateNotBlank(subscription.getSubscriptionId(), "subscriptionId");
	}

	private void validateStatus(SubscriptionStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Subscription status must not be null");
		}
	}

	private void validatePlans(Map<String, String> plansByProduct) {
		for (Map.Entry<String, String> entry : plansByProduct.entrySet()) {
			validateNotBlank(entry.getKey(), "productId");
			validateNotBlank(entry.getValue(), "planId");
		}
	}

	private void validateNotBlank(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("The '" + name + "' parameter cannot be null or empty");
		}
	}
}
