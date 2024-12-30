-- Usage Table

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
    CREATE INDEX idx_limit_id ON Usage (limit_id);
    CREATE INDEX idx_feature_product_user ON Usage (feature_id, product_id, user_grouping);

END;
