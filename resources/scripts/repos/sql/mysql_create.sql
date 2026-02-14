-- Usage Table

-- MySQL Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your MySQL database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script:
--    CREATE TABLE IF NOT EXISTS your_schema.Usage (...);  (replace 'your_schema')
--    Alternatively, you can change the default schema in your SQL client before running
--    the script using 'USE your_schema;'. Be careful as this affects all subsequent commands!

CREATE SCHEMA IF NOT EXISTS your_schema;
CREATE TABLE IF NOT EXISTS your_schema.Usage (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    feature_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    user_grouping VARCHAR(255) NOT NULL,
    limit_id VARCHAR(255) NOT NULL,
    window_start TIMESTAMP NULL,
    window_end TIMESTAMP NULL,
    units INT NOT NULL,
    expiration_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    INDEX idx_limit_id (limit_id),
    INDEX idx_feature_product_user (feature_id, product_id, user_grouping)
);

-- User Limit Table

CREATE TABLE IF NOT EXISTS your_schema.user_usage_limit (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    limit_id VARCHAR(255) NOT NULL,
    feature_id VARCHAR(255) NOT NULL,
    user_group_id VARCHAR(255) NOT NULL,
    limit_type VARCHAR(255) NOT NULL,
    limit_value INT NOT NULL,
    limit_unit VARCHAR(255),
    limit_interval VARCHAR(255),
    limit_duration INT
) ENGINE=InnoDB;
ALTER TABLE your_schema.user_usage_limit ADD CONSTRAINT c_limit UNIQUE (limit_id,feature_id,user_group_id);

-- Subscription Tables

CREATE TABLE IF NOT EXISTS your_schema.subscription (
    subscription_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    expiration_date TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS your_schema.subscription_plan (
    subscription_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (subscription_id, product_id),
    CONSTRAINT fk_subscription_plan_subscription FOREIGN KEY (subscription_id)
        REFERENCES your_schema.subscription(subscription_id) ON DELETE CASCADE
) ENGINE=InnoDB;
