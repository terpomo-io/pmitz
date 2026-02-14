-- Usage Table

-- SQL Server Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your SQL Server database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script.
--    Example: CREATE TABLE your_schema.Usage (...);  (replace 'your_schema')
--    Alternatively, you can change the default schema in your SQL client before running the script.

IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'your_schema')
    EXEC('CREATE SCHEMA your_schema');

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Usage' AND schema_id = SCHEMA_ID('your_schema'))
BEGIN
    CREATE TABLE your_schema.Usage (
        usage_id INT PRIMARY KEY IDENTITY(1,1),
        feature_id NVARCHAR(255) NOT NULL,
        product_id NVARCHAR(255) NOT NULL,
        user_grouping NVARCHAR(255) NOT NULL,
        limit_id NVARCHAR(255) NOT NULL,
        window_start DATETIME2 NULL,
        window_end DATETIME2 NULL,
        units INT NOT NULL,
        expiration_date DATETIME2 NULL,
        updated_at DATETIME2 DEFAULT SYSUTCDATETIME() NOT NULL
    );

    -- Create indexes separately
    CREATE INDEX idx_limit_id ON your_schema.Usage (limit_id);
    CREATE INDEX idx_feature_product_user ON your_schema.Usage (feature_id, product_id, user_grouping);
END;

-- User Limit Table
IF OBJECT_ID(N'your_schema.user_usage_limit', N'U') IS NULL
BEGIN
    CREATE TABLE your_schema.[user_usage_limit] (
        usage_id INT PRIMARY KEY IDENTITY(1,1),
        limit_id VARCHAR(255) NOT NULL,
        feature_id VARCHAR(255) NOT NULL,
        user_group_id VARCHAR(255) NOT NULL,
        limit_type VARCHAR(255) NOT NULL,
        limit_value INT NOT NULL,
        limit_unit VARCHAR(255),
        limit_interval VARCHAR(255),
        limit_duration INT
    );
    ALTER TABLE your_schema.user_usage_limit ADD CONSTRAINT c_limit UNIQUE (limit_id,feature_id, user_group_id);
END

-- Subscription Tables
IF OBJECT_ID(N'your_schema.subscription', N'U') IS NULL
BEGIN
    CREATE TABLE your_schema.subscription (
        subscription_id VARCHAR(255) PRIMARY KEY,
        status VARCHAR(50) NOT NULL,
        expiration_date DATETIME2 NULL
    );
END

IF OBJECT_ID(N'your_schema.subscription_plan', N'U') IS NULL
BEGIN
    CREATE TABLE your_schema.subscription_plan (
        subscription_id VARCHAR(255) NOT NULL,
        product_id VARCHAR(255) NOT NULL,
        plan_id VARCHAR(255) NOT NULL,
        CONSTRAINT pk_subscription_plan PRIMARY KEY (subscription_id, product_id),
        CONSTRAINT fk_subscription_plan_subscription FOREIGN KEY (subscription_id)
            REFERENCES your_schema.subscription(subscription_id) ON DELETE CASCADE
    );
END
