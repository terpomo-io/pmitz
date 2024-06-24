create schema if not exists dbo;
CREATE TABLE dbo.usage (
    usage_id IDENTITY,
    feature_id VARCHAR(255),
    product_id VARCHAR(255),
    user_grouping VARCHAR(255),
    limit_id VARCHAR(255),
    window_start TIMESTAMP,
    window_end TIMESTAMP,
    units INT,
    expiration_date TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usage_id)
)