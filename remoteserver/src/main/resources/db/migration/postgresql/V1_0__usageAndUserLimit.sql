CREATE SCHEMA IF NOT EXISTS dbo;
CREATE TABLE IF NOT EXISTS dbo.usage (
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
CREATE INDEX IF NOT EXISTS idx_usage_limit_id ON dbo.usage (limit_id);
CREATE INDEX IF NOT EXISTS idx_usage_feature_product_user ON dbo.usage (feature_id, product_id, user_grouping);
CREATE TABLE IF NOT EXISTS dbo.user_limit (
    usage_id SERIAL PRIMARY KEY,
    limit_id VARCHAR(255) NOT NULL,
    feature_id VARCHAR(255) NOT NULL,
    user_group_id VARCHAR(255) NOT NULL,
    limit_type VARCHAR(255) NOT NULL,
    limit_value INT NOT NULL,
    limit_unit VARCHAR(255),
    limit_interval VARCHAR(255),
    limit_duration INT
);
ALTER TABLE dbo.user_limit ADD CONSTRAINT c_limit UNIQUE (limit_id,feature_id,user_group_id);