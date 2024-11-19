-- SQL Server Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your SQL Server database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script.
--    Example: CREATE TABLE your_schema.Usage (...);  (replace 'your_schema')
--    Alternatively, you can change the default schema in your SQL client before running the script.

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Usage' AND schema_id = SCHEMA_ID(SCHEMA_NAME()))  -- Checks within current schema
BEGIN
    CREATE TABLE Usage (
        usage_id INT PRIMARY KEY IDENTITY(1,1),
        feature_id NVARCHAR(255),
        product_id NVARCHAR(255),
        user_grouping NVARCHAR(255),
        limit_id NVARCHAR(255),
        window_start DATETIME2,
        window_end DATETIME2,
        units INT,
        expiration_date DATETIME2,
        updated_at DATETIME2 DEFAULT SYSUTCDATETIME()
    );

    -- Create indexes separately
    CREATE INDEX idx_limit_id ON Usage (limit_id);
    CREATE INDEX idx_feature_product_user ON Usage (feature_id, product_id, user_grouping);

END;

-- User Limit Table

--