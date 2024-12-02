-- Usage Table

-- PostgreSQL Table Creation
-- Installation Instructions:
-- 1. Choose a schema in your PostgreSQL database where you want to install the table.
-- 2. Qualify the table name with the schema when running this script:
--    CREATE TABLE IF NOT EXISTS your_schema."Usage" (...); (replace 'your_schema')
--    Alternatively, you can set the search_path in your SQL client before running the script:
--    SET search_path (https://www.postgresql.org/docs/current/runtime-config-client.html#GUC-SEARCH-PATH) TO your_schema, public;

CREATE TABLE IF NOT EXISTS "Usage" (
    usage_id SERIAL PRIMARY KEY,
    feature_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    user_grouping VARCHAR(255) NOT NULL,
    limit_id VARCHAR(255) NOT NULL,
    window_start TIMESTAMP NULL,
    window_end TIMESTAMP NULL,
    units INT NOT NULL,
    expiration_date TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Separate index creation statements
CREATE INDEX idx_limit_id ON "Usage" (limit_id);
CREATE INDEX idx_feature_product_user ON "Usage" (feature_id, product_id, user_grouping);


-- User Limit Table

--