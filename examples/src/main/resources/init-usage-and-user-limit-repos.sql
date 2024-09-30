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
);
CREATE TABLE dbo.user_limit (
    usage_id INT AUTO_INCREMENT,
    limit_id VARCHAR(255),
    feature_id VARCHAR(255),
    user_group_id VARCHAR(255),
    limit_type VARCHAR(255),
    limit_value INT,
    limit_unit  VARCHAR(255),
    limit_interval  VARCHAR(255),
    limit_duration INT,
    PRIMARY KEY (usage_id)
);
