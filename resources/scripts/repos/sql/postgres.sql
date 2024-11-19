-- Usage Table

-- PostgreSQL Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your PostgreSQL database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script:
--    CREATE TABLE IF NOT EXISTS your_schema."Usage" (...); (replace 'your_schema')
--    Alternatively, you can set the search_path in your SQL client before running the script:
--    SET search_path TO your_schema, public;

CREATE TABLE IF NOT EXISTS "Usage" (
    usage_id SERIAL PRIMARY KEY,
    feature_id VARCHAR(255),
    product_id VARCHAR(255),
    user_grouping VARCHAR(255),
    limit_id VARCHAR(255),
    window_start TIMESTAMP,
    window_end TIMESTAMP,
    units INT,
    expiration_date TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Separate index creation statements
CREATE INDEX idx_limit_id ON "Usage" (limit_id);
CREATE INDEX idx_feature_product_user ON "Usage" (feature_id, product_id, user_grouping);

-- User Limit Table

--