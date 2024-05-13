package io.terpomo.pmitz.limits.integration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public class PostgreSQLJDBCUsageRepositoryIntegrationTest extends AbstractJDBCUsageRepositoryIntegrationTest {

	@Container
	private static final PostgreSQLContainer<?> postgresqlContainer =
			new PostgreSQLContainer<>("postgres:latest").withEnv("TZ", "Europe/Berlin");

	@Override
	protected void setupDataSource() {
		postgresqlContainer.start();
		dataSource = new BasicDataSource();
		dataSource.setUrl(postgresqlContainer.getJdbcUrl());
		dataSource.setUsername(postgresqlContainer.getUsername());
		dataSource.setPassword(postgresqlContainer.getPassword());
		repository = new JDBCUsageRepository(dataSource, "public", "\"Usage\"");
	}

	@Override
	protected void setupDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
			statement.execute("CREATE TABLE IF NOT EXISTS public.\"Usage\" ("
					+ "usage_id SERIAL PRIMARY KEY, "
					+ "feature_id VARCHAR(255), "
					+ "product_id VARCHAR(255), "
					+ "user_grouping VARCHAR(255), "
					+ "limit_id VARCHAR(255), "
					+ "window_start TIMESTAMP WITH TIME ZONE, "
					+ "window_end TIMESTAMP WITH TIME ZONE, "
					+ "units INTEGER, "
					+ "expiration_date TIMESTAMP WITH TIME ZONE, "
					+ "updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
					+ ");");

			ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
			ZonedDateTime windowStart = now.plusHours(9);
			ZonedDateTime windowEnd = now.plusHours(17);

			String insertSQL = String.format(
					"INSERT INTO public.\"Usage\" " +
							"(feature_id, product_id, user_grouping, limit_id, window_start, window_end, units, expiration_date) " +
							"VALUES ('featureX', 'productY', 'groupZ', 'limitA', '%s', '%s', 100, '%s')",
					Timestamp.from(windowStart.toInstant()),
					Timestamp.from(windowEnd.toInstant()),
					Timestamp.from(windowStart.plusDays(1).toInstant())
			);
			statement.execute(insertSQL);
		}
	}

	@Override
	protected void tearDownDatabase() throws SQLException {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS public.\"Usage\"");
		}
	}

	// TODO: Remove
	protected void printDatabaseContents() {
		try (Connection conn = dataSource.getConnection();
				Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM public.\"Usage\"");
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

				// TODO: Remove
				System.out.println("UsageId: " + usageId + ", FeatureId: " + featureId + ", ProductId: " + productId
						+ ", UserGrouping: " + userGrouping + ", LimitId: " + limitId + ", WindowStart: " + windowStart
						+ ", WindowEnd: " + windowEnd + ", Units: " + units + ", ExpirationDate: " + expirationDate
						+ ", UpdatedAt: " + updatedAt);
			}
		}
		catch (SQLException e) {
			// TODO: handle exceptions
			e.printStackTrace();
		}
	}
}
