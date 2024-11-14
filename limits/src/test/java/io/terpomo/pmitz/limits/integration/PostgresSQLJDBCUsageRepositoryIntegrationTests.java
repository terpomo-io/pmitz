package io.terpomo.pmitz.limits.integration;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.*;

public class PostgresSQLJDBCUsageRepositoryIntegrationTests extends AbstractJDBCUsageRepositoryIntegrationTests {

	private static final PostgreSQLContainer<?> postgresqlContainer =
			new PostgreSQLContainer<>("postgres:latest");

	@BeforeAll
	public static void setUpClass() {
		postgresqlContainer.start();
	}

	@AfterAll
	public static void tearDownClass() {
		if (postgresqlContainer != null) {
			postgresqlContainer.stop();
		}
	}

	@Override
	protected void setupDataSource() {
		dataSource = new BasicDataSource();
		dataSource.setUrl(postgresqlContainer.getJdbcUrl());
		dataSource.setUsername(postgresqlContainer.getUsername());
		dataSource.setPassword(postgresqlContainer.getPassword());

		repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, getTableName());
	}

	@Override
	protected String getTableName() {
		return "\"Usage\"";
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {

			stmt.execute("CREATE SCHEMA IF NOT EXISTS " + CUSTOM_SCHEMA);
			stmt.execute("CREATE TABLE IF NOT EXISTS " + CUSTOM_SCHEMA + ".\"Usage\" (" +
					"usage_id SERIAL PRIMARY KEY, " +
					"feature_id VARCHAR(255), " +
					"product_id VARCHAR(255), " +
					"user_grouping VARCHAR(255), " +
					"limit_id VARCHAR(255), " +
					"window_start TIMESTAMP, " +
					"window_end TIMESTAMP, " +
					"units INT, " +
					"expiration_date TIMESTAMP, " +
					"updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
					");");
		}
	}

	@Override
	protected void tearDownDatabase() {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
			statement.execute("TRUNCATE TABLE " + getFullTableName() + " RESTART IDENTITY CASCADE");
		} catch (SQLException ex) {
			System.out.println("Error during tearDownDatabase: " + ex.getMessage());
		}
	}

	@Override
	protected void printDatabaseContents(String message) {
		System.out.println("---- " + message + " ----");
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM " + CUSTOM_SCHEMA + "." + getTableName());
			while (rs.next()) {
				int usageId = rs.getInt("usage_id");
				String featureId = rs.getString("feature_id");
				String productId = rs.getString("product_id");
				String userGrouping = rs.getString("user_grouping");
				String limitId = rs.getString("limit_id");
				Timestamp windowStart = rs.getTimestamp("window_start");
				Timestamp windowEnd = rs.getTimestamp("window_end");
				int units = rs.getInt("units");
				Timestamp expirationDate = rs.getTimestamp("expiration_date");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		} catch (SQLException ex) {
			System.out.println("Error while printing database contents: " + ex.getMessage());
		}
	}
}
