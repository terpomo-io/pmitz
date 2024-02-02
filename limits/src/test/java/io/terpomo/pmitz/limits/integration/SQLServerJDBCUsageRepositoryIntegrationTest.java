package io.terpomo.pmitz.limits.integration;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class SQLServerJDBCUsageRepositoryIntegrationTest extends AbstractJDBCUsageRepositoryIntegrationTest {

    @Container
    private static final MSSQLServerContainer<?> mssqlServerContainer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:latest").withEnv("TZ", "Europe/Berlin");

    @Override
    protected void setupDataSource() {
        mssqlServerContainer.start();
        dataSource = new BasicDataSource();
        dataSource.setUrl(mssqlServerContainer.getJdbcUrl());
        dataSource.setUsername(mssqlServerContainer.getUsername());
        dataSource.setPassword(mssqlServerContainer.getPassword());
        repository = new JDBCUsageRepository(dataSource, "dbo", "[Usage]");
    }

    @Override
    protected void setupDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            String checkTableExists = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Usage' AND schema_id = SCHEMA_ID('dbo')) ";
            String createTable = "CREATE TABLE dbo.[Usage] ("
                    + "usage_id INT PRIMARY KEY IDENTITY(1,1), "
                    + "feature_id VARCHAR(255), "
                    + "product_id VARCHAR(255), "
                    + "user_grouping VARCHAR(255), "
                    + "limit_id VARCHAR(255), "
                    + "window_start DATETIMEOFFSET, "
                    + "window_end DATETIMEOFFSET, "
                    + "units INT, "
                    + "expiration_date DATETIMEOFFSET, "
                    + "updated_at DATETIMEOFFSET DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            statement.execute(checkTableExists + createTable);

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime windowStart = now.plusHours(9);
            ZonedDateTime windowEnd = now.plusHours(17);

            String insertSQL = String.format(
                    "INSERT INTO dbo.[Usage] " +
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
            statement.execute("DROP TABLE IF EXISTS dbo.[Usage]");
        }
    }

    // TODO: Remove
    protected void printDatabaseContents() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM dbo.\"Usage\"");
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
        } catch (SQLException e) {
            // TODO: handle exceptions
            e.printStackTrace();
        }
    }
}

