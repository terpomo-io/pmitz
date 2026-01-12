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

CREATE TABLE dbo.subscription (
    subscription_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    expiration_date TIMESTAMP
);
CREATE TABLE dbo.subscription_plan (
    subscription_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (subscription_id, product_id),
    CONSTRAINT fk_subscription_plan_subscription FOREIGN KEY (subscription_id)
        REFERENCES dbo.subscription(subscription_id)
);
