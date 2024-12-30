-- Usage Table

-- MySQL Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your MySQL database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script:
--    CREATE TABLE IF NOT EXISTS your_schema.Usage (...);  (replace 'your_schema')
--    Alternatively, you can change the default schema in your SQL client before running
--    the script using 'USE your_schema;'. Be careful as this affects all subsequent commands!

CREATE TABLE IF NOT EXISTS Usage (
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
    INDEX idx_limit_id (limit_id),  -- Index on limit_id
    INDEX idx_feature_product_user (feature_id, product_id, user_grouping) -- Composite index
);

-- User Limit Table

--