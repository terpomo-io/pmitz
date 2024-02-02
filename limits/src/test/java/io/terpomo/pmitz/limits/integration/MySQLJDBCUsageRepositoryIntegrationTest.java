package io.terpomo.pmitz.limits.integration;

import io.terpomo.pmitz.limits.usage.repository.impl.JDBCUsageRepository;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class MySQLJDBCUsageRepositoryIntegrationTest extends AbstractJDBCUsageRepositoryIntegrationTest {

    private static final String CUSTOM_SCHEMA = "pmitz";


    @Container
    private static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>("mysql:latest")
                    .withDatabaseName(CUSTOM_SCHEMA)
                    .withEnv("TZ", "America/New_York");


    @Override
    protected void setupDataSource() {
        mysqlContainer.start();
        dataSource = new BasicDataSource();
        dataSource.setUrl(mysqlContainer.getJdbcUrl());
        dataSource.setUsername(mysqlContainer.getUsername());
        dataSource.setPassword(mysqlContainer.getPassword());
        repository = new JDBCUsageRepository(dataSource, CUSTOM_SCHEMA, "`Usage`");
    }

    @Override
    protected void setupDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS `Usage` ("
                    + "usage_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "feature_id VARCHAR(255), "
                    + "product_id VARCHAR(255), "
                    + "user_grouping VARCHAR(255), "
                    + "limit_id VARCHAR(255), "
                    + "window_start TIMESTAMP, "
                    + "window_end TIMESTAMP, "
                    + "units INT, "
                    + "expiration_date TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB;");

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime windowStart = now.plusHours(9);
            ZonedDateTime windowEnd = now.plusHours(17);

            String insertSQL = String.format(
                    "INSERT INTO `Usage` " +
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
            statement.execute("DROP TABLE IF EXISTS `Usage`");
        }
    }

    // TODO: Remove
    @Override
    protected void printDatabaseContents() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM `Usage`");
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
